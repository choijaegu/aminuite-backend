package com.example.political_chat_backend;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration // 이 클래스가 Spring 설정 클래스임을 나타냅니다.
@EnableWebSocketMessageBroker // WebSocket 메시지 브로커 기능을 활성화합니다. (STOMP 사용 가능)
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // 메시지 브로커가 /topic 프리픽스가 붙은 목적지를 처리하도록 설정합니다.
        // 클라이언트는 이 프리픽스가 붙은 주제를 구독(subscribe)하여 메시지를 받을 수 있습니다.
        config.enableSimpleBroker("/topic");

        // 클라이언트가 서버로 메시지를 보낼 때 사용할 프리픽스를 설정합니다.
        // 예를 들어, 클라이언트가 /app/hello 라는 경로로 메시지를 보내면,
        // 해당 메시지는 @MessageMapping("/hello") 어노테이션이 붙은 메소드로 라우팅됩니다.
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // STOMP WebSocket 엔드포인트를 등록합니다.
        // 클라이언트가 WebSocket 연결을 시작할 때 사용할 경로입니다.
        // 예를 들어, JavaScript 클라이언트는 "new SockJS('/ws')" 와 같이 이 경로로 접속합니다.
        // withSockJS()는 WebSocket을 지원하지 않는 브라우저에서도 유사한 경험을 제공하기 위한 옵션입니다.
        registry.addEndpoint("/ws").withSockJS();
    }
}
