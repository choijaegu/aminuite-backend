package com.example.political_chat_backend;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**") // 모든 경로에 대해 CORS 허용 (더 관대하게)
                // 또는 registry.addMapping("/api/**") 기존 설정에 다음 줄 추가
                // .addMapping("/ws/**") // WebSocket 엔드포인트도 추가
                .allowedOrigins("http://localhost:8081") // Vue 개발 서버 주소
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
}