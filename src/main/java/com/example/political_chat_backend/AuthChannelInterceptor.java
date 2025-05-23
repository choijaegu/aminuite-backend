package com.example.political_chat_backend;

import com.example.political_chat_backend.UserDetailsServiceImpl; // 실제 경로로 수정
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
import org.springframework.security.core.AuthenticationException; // 명시적 예외 발생시 필요
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

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
            // nativeHeaders는 STOMP JS 클라이언트가 connectHeaders에 설정한 값을 포함합니다.
            String authorizationHeader = accessor.getFirstNativeHeader("Authorization");
            logger.info("STOMP CONNECT attempt. Authorization header: {}", authorizationHeader);

            if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
                String jwt = authorizationHeader.substring(7);
                try {
                    if (jwtUtils.validateJwtToken(jwt)) {
                        String username = jwtUtils.getUserNameFromJwtToken(jwt);
                        UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                        UsernamePasswordAuthenticationToken authentication =
                                new UsernamePasswordAuthenticationToken(
                                        userDetails, null, userDetails.getAuthorities());

                        // 이 부분이 WebSocket 세션에 인증된 사용자(Principal)를 설정하는 핵심입니다.
                        accessor.setUser(authentication);
                        logger.info("User '{}' authenticated successfully for WebSocket session via STOMP CONNECT. Principal set.", username);
                    } else {
                        logger.warn("Invalid JWT token provided in STOMP CONNECT header. Authentication failed.");
                        // 여기서 AuthenticationException을 발생시켜 연결을 명시적으로 거부할 수 있습니다.
                        // throw new AuthenticationException("Invalid JWT token");
                    }
                } catch (Exception e) {
                    logger.error("Error during STOMP CONNECT JWT authentication: {}", e.getMessage(), e);
                    // throw new AuthenticationException("Authentication error: " + e.getMessage());
                }
            } else {
                logger.warn("No Bearer token found in STOMP CONNECT headers. Connection will be unauthenticated.");
                // 인증되지 않은 연결을 허용하지 않으려면 여기서 예외를 발생시킵니다.
                // throw new AuthenticationException("Missing JWT token for STOMP CONNECT");
            }
        } else if (accessor != null && (StompCommand.SEND.equals(accessor.getCommand()) || StompCommand.SUBSCRIBE.equals(accessor.getCommand()))) {
            // SEND나 SUBSCRIBE 시점에 사용자가 인증되지 않았다면 (accessor.getUser() == null)
            // 여기서도 요청을 거부하거나 경고를 로깅할 수 있습니다.
            if (accessor.getUser() == null) {
                logger.warn("Attempt to {} without proper authentication on STOMP session. User: {}", accessor.getCommand(), accessor.getUser());
                // throw new AuthenticationException("Not authenticated for " + accessor.getCommand());
            } else {
                logger.info("User '{}' attempting to {}. Destination: {}", accessor.getUser().getName(), accessor.getCommand(), accessor.getDestination());
            }
        }
        return message;
    }
}