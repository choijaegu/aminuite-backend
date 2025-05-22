package com.example.political_chat_backend; // 패키지 선언은 사용자님의 것과 동일하게

import com.example.political_chat_backend.AuthEntryPointJwt; // security.jwt 패키지로 가정
import com.example.political_chat_backend.AuthTokenFilter;   // security.jwt 패키지로 가정

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
    private AuthEntryPointJwt unauthorizedHandler; // AuthEntryPointJwt는 security.jwt 패키지에 있다고 가정

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
        // AuthTokenFilter가 JwtUtils와 UserDetailsServiceImpl을 @Autowired로 주입받는다고 가정
        return new AuthTokenFilter();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of(
                "http://localhost:8081", // 로컬 Vue 개발 서버
                "https://aminute.onrender.com"  // <<--- 실제 배포된 프론트엔드 URL (정확히 확인!)
        ));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
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
                        // Render 헬스 체크 및 기본 경로 허용 (GET 요청만)
                        .requestMatchers(HttpMethod.GET, "/").permitAll()
                        // 만약 Render 헬스 체크 경로가 다르다면 해당 경로로 수정 (예: /healthz)
                        // .requestMatchers(HttpMethod.GET, "/healthz").permitAll()

                        .requestMatchers("/ws/**").permitAll() // WebSocket
                        .requestMatchers(HttpMethod.POST, "/api/auth/signup").permitAll() // 회원가입
                        .requestMatchers(HttpMethod.POST, "/api/auth/signin").permitAll() // 로그인

                        // 공지사항 API 접근 권한 설정
                        .requestMatchers(HttpMethod.GET, "/api/announcements").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/announcements/*").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/announcements").hasAuthority("ROLE_ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/announcements/*").hasAuthority("ROLE_ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/announcements/*").hasAuthority("ROLE_ADMIN")

                        // 채팅방 관련 API 경로
                        .requestMatchers(HttpMethod.GET, "/api/categories").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/categories/*/chatrooms").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/chatrooms/*").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/categories/*/chatrooms").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/chatrooms/*/admin/kick").authenticated()

                        // 위의 구체적인 경로 규칙들 이후, 나머지 /api/** 경로에 대한 일반 규칙
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().authenticated() // 다른 모든 요청도 인증 요구
                )
                .httpBasic(basic -> basic.disable()); // HTTP Basic 인증 비활성화

        http.addFilterBefore(authenticationJwtTokenFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}