package com.example.political_chat_backend;

import org.springframework.beans.factory.annotation.Autowired; // Autowired 추가
import org.springframework.messaging.handler.annotation.DestinationVariable; // DestinationVariable 추가
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
// import org.springframework.messaging.handler.annotation.SendTo; // @SendTo는 이제 사용 안 함
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate; // SimpMessagingTemplate 추가
import org.springframework.stereotype.Controller;

@Controller
public class ChatController {

    private final PersistedChatMessageRepository persistedChatMessageRepository;
    private final SimpMessagingTemplate messagingTemplate; // SimpMessagingTemplate 필드 추가

    // 생성자 수정: SimpMessagingTemplate 주입 추가
    @Autowired // 여러 빈을 주입받을 때 명확성을 위해 @Autowired 추가 (선택 사항이지만 권장)
    public ChatController(PersistedChatMessageRepository persistedChatMessageRepository,
                          SimpMessagingTemplate messagingTemplate) {
        this.persistedChatMessageRepository = persistedChatMessageRepository;
        this.messagingTemplate = messagingTemplate; // 주입받은 객체 할당
    }

    // "/app/chat.sendMessage/{roomId}" 형식의 경로로 메시지를 받습니다.
    // {roomId} 부분은 동적으로 변하는 경로 변수입니다.
    @MessageMapping("/chat.sendMessage/{roomId}")
    // @SendTo("/topic/public") // 더 이상 사용하지 않으므로 주석 처리 또는 삭제
    public void sendMessage(@Payload ChatMessage chatMessage,
                            @DestinationVariable String roomId, // 경로 변수 roomId를 파라미터로 받음
                            SimpMessageHeaderAccessor headerAccessor) { // 세션 속성 접근 위해 추가 (선택 사항)

        // (선택 사항) WebSocket 세션에서 사용자 이름 가져오기 (addUser에서 설정했다면)
        // String sessionUsername = (String) headerAccessor.getSessionAttributes().get("username");
        // if (sessionUsername != null) {
        //     chatMessage.setSender(sessionUsername); // 실제 발신자 정보로 덮어쓰기 (보안 강화)
        // }

        chatMessage.setRoomId(roomId); // DTO에도 roomId 설정 (클라이언트가 안 보냈을 경우 대비)

        // 1. 메시지 DB에 저장
        PersistedChatMessage messageToSave = new PersistedChatMessage(
                chatMessage.getSender(),
                chatMessage.getContent(),
                chatMessage.getType() != null ? chatMessage.getType() : ChatMessage.MessageType.CHAT // null 체크 추가
        );
        persistedChatMessageRepository.save(messageToSave);
        System.out.println("메시지 저장됨 (방 ID: " + roomId + "): ID=" + messageToSave.getId() + ", 발신자=" + messageToSave.getSender() + ", 내용=" + messageToSave.getContent());

        // 2. 해당 roomId를 가진 토픽으로 메시지를 브로드캐스트합니다.
        // 예: roomId가 "politics"이면 "/topic/room/politics"로 메시지 전송
        messagingTemplate.convertAndSend("/topic/room/" + roomId, chatMessage);
    }

    // 사용자가 채팅방에 참여했을 때 알림 메시지를 보내는 핸들러 (roomId 처리 추가)
    @MessageMapping("/chat.addUser/{roomId}")
    // @SendTo("/topic/public") // 동적 토픽 사용으로 변경
    public void addUser(@Payload ChatMessage chatMessage,
                        @DestinationVariable String roomId,
                        SimpMessageHeaderAccessor headerAccessor) {

        // 웹소켓 세션에 사용자 이름과 현재 방 ID를 추가합니다.
        if (headerAccessor.getSessionAttributes() != null) {
            headerAccessor.getSessionAttributes().put("username", chatMessage.getSender());
            headerAccessor.getSessionAttributes().put("roomId", roomId); // 현재 방 ID도 저장
        }
        chatMessage.setRoomId(roomId); // DTO에 roomId 설정
        chatMessage.setType(ChatMessage.MessageType.JOIN); // 메시지 타입을 JOIN으로 설정
        // chatMessage.setContent(chatMessage.getSender() + " 님이 입장했습니다."); // 서버에서 내용 설정 가능

        // JOIN 메시지도 DB에 저장하고 싶다면 여기서 PersistedChatMessage 만들고 save() 호출
        PersistedChatMessage joinMessageToSave = new PersistedChatMessage(
                chatMessage.getSender(),
                chatMessage.getSender() + " 님이 입장했습니다.", // 서버에서 입장 메시지 내용 생성
                chatMessage.getType()
        );
        // joinMessageToSave.setRoomId(roomId); // PersistedChatMessage에도 roomId 필드가 있다면 설정
        persistedChatMessageRepository.save(joinMessageToSave);
        System.out.println("입장 메시지 저장됨 (방 ID: " + roomId + "): " + joinMessageToSave.getSender());

        messagingTemplate.convertAndSend("/topic/room/" + roomId, chatMessage);
    }
}
