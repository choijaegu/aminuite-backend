package com.example.political_chat_backend;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Controller
public class ChatController {

    private final PersistedChatMessageRepository persistedChatMessageRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final ChatRoomService chatRoomService;
    private final ChatRoomUserService chatRoomUserService; // ChatRoomUserService 주입

    // 사용자별 마지막 메시지 전송 시간 (쿨다운용)
    private final Map<String, LocalDateTime> lastMessageTimes = new ConcurrentHashMap<>();
    private static final long CHAT_COOLDOWN_SECONDS = 5; // 테스트를 위해 5초로 짧게 (원래 60초였음)

    @Autowired
    public ChatController(PersistedChatMessageRepository persistedChatMessageRepository,
                          SimpMessagingTemplate messagingTemplate,
                          ChatRoomService chatRoomService,
                          ChatRoomUserService chatRoomUserService) {
        this.persistedChatMessageRepository = persistedChatMessageRepository;
        this.messagingTemplate = messagingTemplate;
        this.chatRoomService = chatRoomService;
        this.chatRoomUserService = chatRoomUserService;
    }

    // 사용자 입장 시
    @MessageMapping("/chat.addUser/{roomId}")
    public void addUser(@Payload ChatMessage chatMessage,
                        @DestinationVariable String roomId,
                        SimpMessageHeaderAccessor headerAccessor) {
        String username = chatMessage.getSender();
        // WebSocket 세션에 사용자 이름과 방 ID 저장 (연결 해제 시 사용)
        Map<String, Object> sessionAttributes = headerAccessor.getSessionAttributes();
        if (sessionAttributes != null) {
            sessionAttributes.put("username", username);
            sessionAttributes.put("roomId", roomId); // 현재 사용자가 어떤 방에 있는지 세션에 기록
        }

        chatRoomUserService.addUserToRoom(roomId, username); // ChatRoomUserService에 사용자 추가
        chatMessage.setType(ChatMessage.MessageType.JOIN);
        chatMessage.setContent(username + " 님이 입장했습니다.");
        chatMessage.setRoomId(roomId);

        // (선택 사항) 입장 메시지도 DB에 저장할 수 있습니다.
        // PersistedChatMessage joinMsgToSave = new PersistedChatMessage(username, chatMessage.getContent(), chatMessage.getType());
        // persistedChatMessageRepository.save(joinMsgToSave);

        messagingTemplate.convertAndSend("/topic/room/" + roomId, chatMessage); // JOIN 메시지 브로드캐스트
        broadcastUserList(roomId); // 사용자 목록 및 인원 수 브로드캐스트
    }

    // 일반 메시지 전송 시
    @MessageMapping("/chat.sendMessage/{roomId}")
    public void sendMessage(@Payload ChatMessage chatMessage,
                            @DestinationVariable String roomId,
                            SimpMessageHeaderAccessor headerAccessor) { // headerAccessor는 현재 직접 사용 안함
        String sender = chatMessage.getSender();
        LocalDateTime currentTime = LocalDateTime.now();

        // 방장 여부 확인
        boolean isModerator = isModerator(sender, roomId);

        if (!isModerator) { // 방장이 아닐 경우에만 쿨다운 체크
            LocalDateTime lastMessageTime = lastMessageTimes.get(sender + ":" + roomId); // 방별 쿨다운을 위해 roomId도 키에 포함
            if (lastMessageTime != null) {
                long secondsSinceLastMessage = ChronoUnit.SECONDS.between(lastMessageTime, currentTime);
                if (secondsSinceLastMessage < CHAT_COOLDOWN_SECONDS) {
                    System.out.println("쿨다운 적용 (방: " + roomId + "): " + sender + " 님은 " + (CHAT_COOLDOWN_SECONDS - secondsSinceLastMessage) + "초 후에 메시지를 보낼 수 있습니다.");
                    // (선택) 클라이언트에게 쿨다운 알림 메시지 전송
                    // ChatMessage cooldownAlert = new ChatMessage("System", "메시지를 너무 자주 보낼 수 없습니다. " + (CHAT_COOLDOWN_SECONDS - secondsSinceLastMessage) + "초 후 시도하세요.", ChatMessage.MessageType.SYSTEM_NOTICE, roomId);
                    // messagingTemplate.convertAndSendToUser(senderSessionId_or_username, "/queue/errors", cooldownAlert); // 특정 사용자에게 보내는 방법 필요
                    return; // 메시지 처리 중단
                }
            }
            lastMessageTimes.put(sender + ":" + roomId, currentTime); // 현재 메시지 전송 시간 기록 (방별)
        } else {
            System.out.println("방장(" + sender + ")은 쿨다운 면제됩니다 (방: " + roomId + ").");
        }

        chatMessage.setRoomId(roomId);
        // 메시지 DB 저장
        PersistedChatMessage messageToSave = new PersistedChatMessage(
                sender,
                chatMessage.getContent(),
                chatMessage.getType() != null ? chatMessage.getType() : ChatMessage.MessageType.CHAT // type이 null이면 CHAT으로
        );
        persistedChatMessageRepository.save(messageToSave);
        // IntelliJ 콘솔 한글 깨짐은 일단 무시
        System.out.println("메시지 저장됨 (방 ID: " + roomId + "): ID=" + messageToSave.getId() + ", 발신자=" + sender + ", 내용=" + messageToSave.getContent());

        messagingTemplate.convertAndSend("/topic/room/" + roomId, chatMessage);
    }

    // WebSocket 연결 해제 이벤트 리스너
    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        SimpMessageHeaderAccessor headerAccessor = SimpMessageHeaderAccessor.wrap(event.getMessage());
        Map<String, Object> sessionAttributes = headerAccessor.getSessionAttributes();

        if (sessionAttributes != null) {
            String username = (String) sessionAttributes.get("username");
            String roomId = (String) sessionAttributes.get("roomId"); // 세션에서 roomId 가져오기

            if (username != null && roomId != null) {
                System.out.println("User Disconnected Event: " + username + " from room: " + roomId);
                boolean removed = chatRoomUserService.removeUserFromRoom(roomId, username);

                if (removed) { // 실제로 사용자가 목록에서 제거된 경우에만 메시지 전송
                    ChatMessage leaveMessage = new ChatMessage();
                    leaveMessage.setType(ChatMessage.MessageType.LEAVE);
                    leaveMessage.setSender(username); // 시스템 메시지가 아닌, 나간 사용자의 이름으로
                    leaveMessage.setContent(username + " 님이 퇴장했습니다.");
                    leaveMessage.setRoomId(roomId);

                    messagingTemplate.convertAndSend("/topic/room/" + roomId, leaveMessage);
                    broadcastUserList(roomId); // 사용자 목록 및 인원 수 다시 브로드캐스트
                }
            }
        }
    }

    // 특정 방에 사용자 목록 및 인원 수를 브로드캐스트하는 헬퍼 메소드
    private void broadcastUserList(String roomId) {
        Set<String> usersInRoom = chatRoomUserService.getUsersInRoom(roomId);
        int userCount = usersInRoom.size();

        ChatMessage userListMessage = new ChatMessage();
        userListMessage.setType(ChatMessage.MessageType.USER_LIST_UPDATE);
        userListMessage.setRoomId(roomId);
        userListMessage.setSender("System"); // 이 메시지의 발신자는 시스템
        userListMessage.setContent("사용자 목록 업데이트"); // 이 내용은 클라이언트에서 직접 사용 안 할 수도 있음
        userListMessage.setUsers(usersInRoom); // 실제 사용자 목록 Set
        userListMessage.setUserCount(userCount); // 실제 사용자 수

        System.out.println("Broadcasting user list for room " + roomId + ": " + usersInRoom + " (Count: " + userCount + ")");
        messagingTemplate.convertAndSend("/topic/room/" + roomId, userListMessage);
    }

    // 방장 여부 확인 헬퍼 메소드
    private boolean isModerator(String username, String roomId) {
        if (username == null || roomId == null) return false;
        Optional<ChatRoom> roomOpt = chatRoomService.findChatRoomById(roomId);
        if (roomOpt.isPresent()) {
            ChatRoom room = roomOpt.get();
            // ChatRoom 엔티티에 ownerUsername 필드가 있고, DB에 잘 저장되어 있어야 함
            return username.equals(room.getOwnerUsername());
        }
        return false;
    }
}