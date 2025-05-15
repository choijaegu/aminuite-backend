package com.example.political_chat_backend;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit; // ChronoUnit 추가
import java.util.Map; // Map 추가
import java.util.concurrent.ConcurrentHashMap; // ConcurrentHashMap 추가

@Controller
public class ChatController {

    private final PersistedChatMessageRepository persistedChatMessageRepository;
    private final SimpMessagingTemplate messagingTemplate;

    // 사용자별 마지막 메시지 전송 시간을 저장할 Map
    // Key: sender (사용자 이름), Value: 마지막 메시지 전송 시간
    private final Map<String, LocalDateTime> lastMessageTimes = new ConcurrentHashMap<>();
    private static final long CHAT_COOLDOWN_SECONDS = 60; // 채팅 쿨다운 시간 (초) - 예: 60초

    @Autowired
    public ChatController(PersistedChatMessageRepository persistedChatMessageRepository,
                          SimpMessagingTemplate messagingTemplate) {
        this.persistedChatMessageRepository = persistedChatMessageRepository;
        this.messagingTemplate = messagingTemplate;
    }

    @MessageMapping("/chat.sendMessage/{roomId}")
    public void sendMessage(@Payload ChatMessage chatMessage,
                            @DestinationVariable String roomId,
                            SimpMessageHeaderAccessor headerAccessor) {

        String sender = chatMessage.getSender();
        LocalDateTime currentTime = LocalDateTime.now();

        // (임시) 방장은 쿨다운 적용 안 함 - 사용자 이름이 "방장" 또는 "admin" 등일 경우
        boolean isModerator = sender != null && (sender.equalsIgnoreCase("방장") || sender.equalsIgnoreCase("admin"));

        if (!isModerator) { // 방장이 아닐 경우에만 쿨다운 체크
            LocalDateTime lastMessageTime = lastMessageTimes.get(sender);
            if (lastMessageTime != null) {
                long secondsSinceLastMessage = ChronoUnit.SECONDS.between(lastMessageTime, currentTime);
                if (secondsSinceLastMessage < CHAT_COOLDOWN_SECONDS) {
                    System.out.println("쿨다운 적용: " + sender + " 님은 " + (CHAT_COOLDOWN_SECONDS - secondsSinceLastMessage) + "초 후에 메시지를 보낼 수 있습니다.");
                    // 클라이언트에게 쿨다운 알림을 보내는 로직을 추가할 수 있습니다 (예: 특정 사용자에게만 에러 메시지 전송)
                    // messagingTemplate.convertAndSendToUser(sender, "/queue/errors", "메시지를 너무 자주 보낼 수 없습니다.");
                    return; // 메시지 처리 중단
                }
            }
        }

        // 쿨다운 통과 또는 방장일 경우
        lastMessageTimes.put(sender, currentTime); // 현재 메시지 전송 시간 기록

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

    @MessageMapping("/chat.addUser/{roomId}")
    public void addUser(@Payload ChatMessage chatMessage,
                        @DestinationVariable String roomId,
                        SimpMessageHeaderAccessor headerAccessor) {

        if (headerAccessor.getSessionAttributes() != null) {
            headerAccessor.getSessionAttributes().put("username", chatMessage.getSender());
            headerAccessor.getSessionAttributes().put("roomId", roomId);
        }
        chatMessage.setRoomId(roomId);
        chatMessage.setType(ChatMessage.MessageType.JOIN);

        PersistedChatMessage joinMessageToSave = new PersistedChatMessage(
                chatMessage.getSender(),
                chatMessage.getSender() + " 님이 입장했습니다.",
                chatMessage.getType()
        );
        persistedChatMessageRepository.save(joinMessageToSave);
        System.out.println("입장 메시지 저장됨 (방 ID: " + roomId + "): " + joinMessageToSave.getSender());

        messagingTemplate.convertAndSend("/topic/room/" + roomId, chatMessage);
    }
}
