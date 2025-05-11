package com.example.political_chat_backend;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

@Controller // 이 클래스가 Spring MVC 컨트롤러 역할을 함 (여기서는 WebSocket 메시지 처리)
public class ChatController {

    // 클라이언트가 "/app/chat.sendMessage" 경로로 메시지를 보내면 이 메소드가 호출됩니다.
    @MessageMapping("/chat.sendMessage")
    // 이 메소드에서 반환되는 값은 "/topic/public"을 구독하고 있는 모든 클라이언트에게 전달됩니다.
    @SendTo("/topic/public")
    public ChatMessage sendMessage(@Payload ChatMessage chatMessage) {
        // @Payload 어노테이션은 메시지의 본문(payload)을 ChatMessage 객체로 변환해줍니다.
        // 현재는 받은 메시지를 그대로 다시 브로드캐스트합니다.
        // 나중에 여기에 메시지 저장 로직, 필터링, 사용자 정보 추가 등을 할 수 있습니다.
        return chatMessage;
    }

    // (참고) 사용자가 채팅방에 참여했을 때 알림 메시지를 보내는 핸들러 예시 (나중에 구현)
    /*
    @MessageMapping("/chat.addUser")
    @SendTo("/topic/public")
    public ChatMessage addUser(@Payload ChatMessage chatMessage,
                               SimpMessageHeaderAccessor headerAccessor) {
        // 웹소켓 세션에 사용자 이름을 추가합니다 (나중에 특정 사용자를 식별할 때 사용 가능)
        if (headerAccessor.getSessionAttributes() != null) {
            headerAccessor.getSessionAttributes().put("username", chatMessage.getSender());
        }
        chatMessage.setType(ChatMessage.MessageType.JOIN); // 메시지 타입을 JOIN으로 설정
        return chatMessage;
    }
    */
}
