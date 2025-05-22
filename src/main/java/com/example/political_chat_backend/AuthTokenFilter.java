package com.example.political_chat_backend;

import com.example.political_chat_backend.UserDetailsServiceImpl; // UserDetailsServiceImpl 임포트
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter; // OncePerRequestFilter 임포트

import java.io.IOException;

// @Component 어노테이션은 SecurityConfig에서 직접 빈으로 등록하고 필터 체인에 추가하므로 여기서는 필요 없습니다.
// 만약 Spring이 자동으로 이 필터를 스캔하도록 하려면 @Component를 사용할 수 있지만, 명시적 설정이 더 일반적입니다.
public class AuthTokenFilter extends OncePerRequestFilter { // OncePerRequestFilter를 상속

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private UserDetailsServiceImpl userDetailsService; // UserDetailsServiceImpl 주입

    private static final Logger logger = LoggerFactory.getLogger(AuthTokenFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String jwt = parseJwt(request);
            if (jwt != null && jwtUtils.validateJwtToken(jwt)) {
                String username = jwtUtils.getUserNameFromJwtToken(jwt);

                UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null, // 비밀번호는 사용하지 않으므로 null
                                userDetails.getAuthorities());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (Exception e) {
            logger.error("Cannot set user authentication: {}", e.getMessage());
            // 여기서 response에 401 오류를 직접 쓰지 않습니다.
            // 인증 실패 시 처리는 AuthenticationEntryPoint에서 담당하게 됩니다.
        }

        filterChain.doFilter(request, response); // 다음 필터로 요청 전달
    }

    // HttpServletRequest에서 JWT 토큰을 파싱하는 헬퍼 메소드
    private String parseJwt(HttpServletRequest request) {
        String headerAuth = request.getHeader("Authorization");

        if (StringUtils.hasText(headerAuth) && headerAuth.startsWith("Bearer ")) {
            return headerAuth.substring(7); // "Bearer " 다음의 토큰 문자열 반환
        }

        return null; // 토큰이 없거나 형식이 맞지 않으면 null 반환
    }
}
