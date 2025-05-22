package com.example.political_chat_backend;

import com.example.political_chat_backend.AuthEntryPointJwt;
import com.example.political_chat_backend.AuthTokenFilter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private AuthEntryPointJwt unauthorizedHandler;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public AuthTokenFilter authenticationJwtTokenFilter() {
        return new AuthTokenFilter();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of("http://localhost:8081","https://aminute.onrender.com")); // 프론트엔드 주소
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration); // 모든 경로에 대해 이 CORS 설정을 적용
        return source;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .exceptionHandling(exception -> exception.authenticationEntryPoint(unauthorizedHandler))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/ws/**").permitAll() // WebSocket
                        .requestMatchers(HttpMethod.POST, "/api/auth/signup").permitAll() // 회원가입
                        .requestMatchers(HttpMethod.POST, "/api/auth/signin").permitAll() // 로그인

                        // 공지사항 API 접근 권한 설정
                        .requestMatchers(HttpMethod.GET, "/api/announcements").permitAll()          // 공지사항 목록 조회
                        .requestMatchers(HttpMethod.GET, "/api/announcements/*").permitAll()       // 공지사항 상세 조회 (ID가 있는 경우, 단일 세그먼트)
                        .requestMatchers(HttpMethod.POST, "/api/announcements").hasAuthority("ROLE_ADMIN") // 공지사항 생성
                        .requestMatchers(HttpMethod.PUT, "/api/announcements/*").hasAuthority("ROLE_ADMIN")  // 공지사항 수정 (ID가 있는 경우)
                        .requestMatchers(HttpMethod.DELETE, "/api/announcements/*").hasAuthority("ROLE_ADMIN") // 공지사항 삭제 (ID가 있는 경우)

                        // 채팅방 관련 API 경로 수정
                        .requestMatchers(HttpMethod.GET, "/api/categories").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/categories/*/chatrooms").permitAll() // {categoryId} 대신 * 사용
                        .requestMatchers(HttpMethod.GET, "/api/chatrooms/*").authenticated()       // {roomId} 대신 * 사용
                        .requestMatchers(HttpMethod.POST, "/api/categories/*/chatrooms").authenticated() // {categoryId} 대신 * 사용
                        .requestMatchers(HttpMethod.POST, "/api/chatrooms/*/admin/kick").authenticated() // {roomId} 대신 * 사용

                        // 위의 구체적인 경로 규칙들 이후, 나머지 /api/** 경로에 대한 일반 규칙
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().authenticated()
                )
                .httpBasic(basic -> basic.disable()); // HTTP Basic 인증 비활성화 (JWT 사용)

        http.addFilterBefore(authenticationJwtTokenFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}

