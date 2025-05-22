package com.example.political_chat_backend;

import com.example.political_chat_backend.UserDetailsServiceImpl; // UserDetailsServiceImpl 임포트
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException; // AuthenticationException 임포트
import org.springframework.security.core.context.SecurityContextHolder; // 필요시 사용 (일반적으로 STOMP에서는 accessor.setUser로 충분)
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component; // @Component 추가하여 빈으로 등록

@Component // Spring 컨테이너가 관리하는 빈으로 등록
public class AuthChannelInterceptor implements ChannelInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(AuthChannelInterceptor.class);

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private UserDetailsServiceImpl userDetailsService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            logger.info("STOMP CONNECT attempt.");
            // "nativeHeaders"에서 Authorization 헤더 가져오기 (STOMP JS 클라이언트가 이렇게 보냄)
            String authorizationHeader = accessor.getFirstNativeHeader("Authorization");
            logger.debug("Authorization header: {}", authorizationHeader);

            if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
                String jwt = authorizationHeader.substring(7);
                try {
                    if (jwtUtils.validateJwtToken(jwt)) {
                        String username = jwtUtils.getUserNameFromJwtToken(jwt);
                        UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                        UsernamePasswordAuthenticationToken authentication =
                                new UsernamePasswordAuthenticationToken(
                                        userDetails, null, userDetails.getAuthorities());

                        // STOMP 세션에 사용자 정보(Principal) 설정
                        // 이렇게 하면 @MessageMapping 메소드에서 Principal principal 파라미터를 통해 인증된 사용자 정보 접근 가능
                        accessor.setUser(authentication);
                        logger.info("User {} authenticated for WebSocket session.", username);
                    } else {
                        logger.warn("Invalid JWT token received for STOMP CONNECT.");
                        // 유효하지 않은 토큰이면 연결을 거부해야 하지만, ChannelInterceptor에서 직접 연결을 끊는 것은 복잡함.
                        // 보통은 에러를 던지거나, 메시지 헤더에 오류 플래그를 설정.
                        // 여기서는 그냥 로그만 남기고, 실제 연결 거부는 서버의 WebSocket 보안 설정에 따라 다를 수 있음.
                        // 더 강력하게 하려면 여기서 AuthenticationException을 던질 수 있음.
                        // throw new AuthenticationException("Invalid JWT token"); // 예외 발생 시 연결 실패
                    }
                } catch (Exception e) {
                    logger.error("Error during STOMP CONNECT JWT authentication: {}", e.getMessage());
                    // throw new AuthenticationException("Authentication error: " + e.getMessage()); // 예외 발생 시 연결 실패
                }
            } else {
                logger.warn("No Bearer token found in STOMP CONNECT headers.");
                // 토큰이 없는 연결 시도에 대한 처리 (예: 익명 사용자 허용 안 함)
                // throw new AuthenticationException("Missing JWT token"); // 예외 발생 시 연결 실패
            }
        }
        // 다른 STOMP 명령어(SUBSCRIBE, SEND 등)에 대해서도 필요하다면 여기서 추가 로직 구현 가능
        // 예를 들어, SEND 시 현재 accessor.getUser()를 통해 인증된 사용자인지 확인
        else if (StompCommand.SEND.equals(accessor.getCommand()) || StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            if (accessor.getUser() == null) {
                logger.warn("Attempt to {} without authentication.", accessor.getCommand());
                // throw new AuthenticationException("Not authenticated for " + accessor.getCommand()); // 예외 발생 시 해당 작업 실패
            }
        }


        return message; // 메시지 계속 처리
    }
}