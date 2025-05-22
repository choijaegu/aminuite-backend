package com.example.political_chat_backend;

import com.example.political_chat_backend.AuthChannelInterceptor; // AuthChannelInterceptor 임포트
import org.springframework.beans.factory.annotation.Autowired; // @Autowired 추가
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration; // ChannelRegistration 임포트
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker // WebSocket 메시지 브로커 활성화
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Autowired // AuthChannelInterceptor 주입
    private AuthChannelInterceptor authChannelInterceptor;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // 클라이언트가 메시지를 구독할 수 있는 경로(/topic, /queue 등) 설정
        config.enableSimpleBroker("/topic", "/queue");
        // 클라이언트가 서버로 메시지를 보낼 때 사용할 접두사(/app) 설정
        config.setApplicationDestinationPrefixes("/app");
        // (선택) /user 경로를 사용하는 사용자 특정 메시징을 위한 설정
        // config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // 클라이언트가 WebSocket 연결을 시작할 엔드포인트 설정
        // SockJS를 사용하면 WebSocket을 지원하지 않는 브라우저에서도 유사한 경험 제공
        registry.addEndpoint("/ws") // 프론트엔드에서 SockJS('http://localhost:8080/ws')로 연결하는 경로
                .setAllowedOriginPatterns("*") // 모든 출처에서의 연결 허용 (개발 중, 프로덕션에서는 특정 출처 지정 권장)
                .withSockJS();
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        // 클라이언트로부터 들어오는 메시지를 처리하는 채널에 인터셉터 등록
        registration.interceptors(authChannelInterceptor);
    }
}
