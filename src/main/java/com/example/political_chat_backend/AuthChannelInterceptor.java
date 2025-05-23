package com.example.political_chat_backend;

import com.example.political_chat_backend.UserDetailsServiceImpl;
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
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
// import org.springframework.security.core.AuthenticationException; // 필요시 명시적 예외 발생용

@Component
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

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            String authorizationHeader = accessor.getFirstNativeHeader("Authorization");
            logger.info("STOMP CONNECT - Authorization header: {}", authorizationHeader);

            if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
                String jwt = authorizationHeader.substring(7);
                try {
                    if (jwtUtils.validateJwtToken(jwt)) {
                        String username = jwtUtils.getUserNameFromJwtToken(jwt);
                        UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                        UsernamePasswordAuthenticationToken authentication =
                                new UsernamePasswordAuthenticationToken(
                                        userDetails, null, userDetails.getAuthorities());

                        accessor.setUser(authentication); // WebSocket 세션에 인증된 사용자 정보 설정
                        logger.info("User '{}' authenticated successfully for WebSocket session via STOMP CONNECT.", username);
                    } else {
                        logger.warn("Invalid JWT token provided in STOMP CONNECT header.");
                        // throw new AuthenticationException("Invalid JWT token"); // 연결 거부 시
                    }
                } catch (Exception e) {
                    logger.error("Error during STOMP CONNECT JWT authentication: {}", e.getMessage(), e);
                    // throw new AuthenticationException("Authentication error: " + e.getMessage()); // 연결 거부 시
                }
            } else {
                logger.warn("No Bearer token found in STOMP CONNECT headers. Unauthenticated connection.");
                // throw new AuthenticationException("Missing JWT token"); // 인증되지 않은 연결 거부 시
            }
        } else if (accessor != null && (StompCommand.SEND.equals(accessor.getCommand()) || StompCommand.SUBSCRIBE.equals(accessor.getCommand()))) {
            // SEND나 SUBSCRIBE 시점에 사용자가 인증되지 않았다면 (accessor.getUser() == null)
            // 여기서도 요청을 거부할 수 있습니다.
            if (accessor.getUser() == null) {
                logger.warn("Attempt to {} without proper authentication on STOMP session.", accessor.getCommand());
                // throw new AuthenticationException("Not authenticated for " + accessor.getCommand());
            }
        }
        return message;
    }
}