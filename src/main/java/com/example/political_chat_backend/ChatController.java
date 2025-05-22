package com.example.political_chat_backend; // 패키지 선언은 사용자님의 것과 동일하게

import com.example.political_chat_backend.ChatRoom; // ChatRoom 임포트
import com.example.political_chat_backend.ChatMessage; // ChatMessage 임포트
import com.example.political_chat_backend.PersistedChatMessageRepository; // PersistedChatMessageRepository 임포트
import com.example.political_chat_backend.ChatRoomService; // ChatRoomService 임포트
import com.example.political_chat_backend.ChatRoomUserService; // ChatRoomUserService 임포트

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.security.Principal; // Principal 임포트
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
    private final ChatRoomUserService chatRoomUserService;

    private final Map<String, LocalDateTime> lastMessageTimes = new ConcurrentHashMap<>();
    private static final long CHAT_COOLDOWN_SECONDS = 5;

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

    @MessageMapping("/chat.addUser/{roomId}")
    public void addUser(@Payload ChatMessage chatMessage,
                        @DestinationVariable String roomId,
                        SimpMessageHeaderAccessor headerAccessor,
                        Principal principal) { // Principal 추가
        // Principal에서 사용자 이름을 가져오는 것이 더 안전합니다.
        String username = (principal != null) ? principal.getName() : chatMessage.getSender();
        chatMessage.setSender(username); // 발신자를 인증된 사용자로 설정

        Map<String, Object> sessionAttributes = headerAccessor.getSessionAttributes();
        if (sessionAttributes != null) {
            sessionAttributes.put("username", username);
            sessionAttributes.put("roomId", roomId);
        } else {
            System.err.println("ChatController: SessionAttributes is null for user " + username + " in room " + roomId);
        }

        chatRoomUserService.addUserToRoom(roomId, username);
        chatMessage.setType(ChatMessage.MessageType.JOIN);
        chatMessage.setContent(username + " 님이 입장했습니다.");
        chatMessage.setRoomId(roomId);

        messagingTemplate.convertAndSend("/topic/room/" + roomId, chatMessage);
        broadcastUserList(roomId);
        System.out.println("User added to room " + roomId + ": " + username + " (Session ID: " + headerAccessor.getSessionId() + ")");
    }

    @MessageMapping("/chat.sendMessage/{roomId}")
    public void sendMessage(@Payload ChatMessage chatMessage,
                            @DestinationVariable String roomId,
                            Principal principal) { // Principal 추가
        // Principal에서 사용자 이름을 가져오는 것이 더 안전합니다.
        String sender = (principal != null) ? principal.getName() : chatMessage.getSender();
        chatMessage.setSender(sender); // 발신자를 인증된 사용자로 설정

        LocalDateTime currentTime = LocalDateTime.now();
        boolean isOwner = isRoomOwner(sender, roomId); // 방장 여부 확인 (isModerator 대신 isRoomOwner 사용)

        if (!isOwner) {
            LocalDateTime lastMessageTime = lastMessageTimes.get(sender + ":" + roomId);
            if (lastMessageTime != null) {
                long secondsSinceLastMessage = ChronoUnit.SECONDS.between(lastMessageTime, currentTime);
                if (secondsSinceLastMessage < CHAT_COOLDOWN_SECONDS) {
                    System.out.println("쿨다운 적용 (방: " + roomId + "): " + sender + " 님은 " + (CHAT_COOLDOWN_SECONDS - secondsSinceLastMessage) + "초 후에 메시지를 보낼 수 있습니다.");
                    // 클라이언트에게 쿨다운 알림 메시지 전송 로직 (선택 사항)
                    return;
                }
            }
            lastMessageTimes.put(sender + ":" + roomId, currentTime);
        } else {
            System.out.println("방장(" + sender + ")은 쿨다운 면제됩니다 (방: " + roomId + ").");
        }

        chatMessage.setRoomId(roomId);
        PersistedChatMessage messageToSave = new PersistedChatMessage(
                sender,
                chatMessage.getContent(),
                chatMessage.getType() != null ? chatMessage.getType() : ChatMessage.MessageType.CHAT
        );
        persistedChatMessageRepository.save(messageToSave);
        System.out.println("메시지 저장됨 (방 ID: " + roomId + "): ID=" + messageToSave.getId() + ", 발신자=" + sender + ", 내용=" + messageToSave.getContent());

        messagingTemplate.convertAndSend("/topic/room/" + roomId, chatMessage);
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        SimpMessageHeaderAccessor headerAccessor = SimpMessageHeaderAccessor.wrap(event.getMessage());
        Map<String, Object> sessionAttributes = headerAccessor.getSessionAttributes();

        if (sessionAttributes == null) {
            System.err.println("ChatController (Disconnect): SessionAttributes is null. SessionId: " + headerAccessor.getSessionId());
            return;
        }

        String username = (String) sessionAttributes.get("username");
        String roomId = (String) sessionAttributes.get("roomId");

        if (username != null && roomId != null) {
            System.out.println("User Disconnected Event: " + username + " from room: " + roomId + " (Session ID: " + headerAccessor.getSessionId() + ")");

            boolean actuallyRemoved = chatRoomUserService.removeUserFromRoom(roomId, username);

            if (actuallyRemoved) { // 실제로 목록에서 제거된 경우에만 후속 처리
                // 방장 여부 확인 및 방이 비었는지 확인
                boolean wasOwner = isRoomOwner(username, roomId);
                int remainingUsers = chatRoomUserService.countUsersInRoom(roomId);

                if (wasOwner && remainingUsers == 0) {
                    System.out.println("Owner " + username + " left room " + roomId + " and it's now empty. Deleting room.");
                    chatRoomService.deleteRoom(roomId); // ChatRoomService에 deleteRoom 메소드 필요
                    System.out.println("Room " + roomId + " deleted.");
                    // 방이 삭제되었으므로, 이 방에 대한 LEAVE 나 USER_LIST_UPDATE 메시지는 보낼 필요 없음
                } else {
                    // 방이 삭제되지 않은 경우 (방장이 아니었거나, 방장이 나갔지만 다른 사용자가 남음)
                    // LEAVE 메시지 브로드캐스팅
                    ChatMessage leaveMessage = new ChatMessage();
                    leaveMessage.setType(ChatMessage.MessageType.LEAVE);
                    leaveMessage.setSender(username);
                    leaveMessage.setContent(username + " 님이 퇴장했습니다.");
                    leaveMessage.setRoomId(roomId);
                    messagingTemplate.convertAndSend("/topic/room/" + roomId, leaveMessage);

                    // 사용자 목록 및 인원 수 다시 브로드캐스트
                    broadcastUserList(roomId);
                }
            } else {
                System.out.println("User " + username + " was not in active list for room " + roomId + " or already removed.");
            }
        } else {
            System.out.println("ChatController (Disconnect): Username or RoomID not found in session attributes. SessionId: " + headerAccessor.getSessionId());
        }
    }

    private void broadcastUserList(String roomId) {
        Set<String> usersInRoom = chatRoomUserService.getUsersInRoom(roomId);
        int userCount = usersInRoom.size();

        ChatMessage userListMessage = new ChatMessage();
        userListMessage.setType(ChatMessage.MessageType.USER_LIST_UPDATE);
        userListMessage.setRoomId(roomId);
        userListMessage.setSender("System");
        userListMessage.setContent("사용자 목록 업데이트");
        userListMessage.setUsers(usersInRoom);
        userListMessage.setUserCount(userCount);

        System.out.println("Broadcasting user list for room " + roomId + ": " + usersInRoom + " (Count: " + userCount + ")");
        messagingTemplate.convertAndSend("/topic/room/" + roomId, userListMessage);
    }

    // isModerator 대신 isRoomOwner로 명칭 변경 (ChatRoomService의 메소드와 일관성)
    private boolean isRoomOwner(String username, String roomId) {
        if (username == null || roomId == null) return false;
        // ChatRoomService의 isRoomOwner 메소드 사용
        return chatRoomService.isRoomOwner(roomId, username);
    }
}