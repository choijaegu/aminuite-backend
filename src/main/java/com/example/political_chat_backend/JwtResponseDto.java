package com.example.political_chat_backend;

import java.util.List; // 역할 정보를 위해 추가 (선택 사항)

public class JwtResponseDto {
    private String token;
    private String type = "Bearer"; // JWT 토큰 타입
    private Long id;              // 사용자 ID (선택 사항)
    private String username;      // 사용자 이름 (선택 사항)
    private List<String> roles;   // 사용자 역할 목록 (선택 사항)

    // 생성자 (토큰만 필수, 나머지는 선택)
    public JwtResponseDto(String accessToken, Long id, String username, List<String> roles) {
        this.token = accessToken;
        this.id = id;
        this.username = username;
        this.roles = roles;
    }

    // Getters and Setters
    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public List<String> getRoles() {
        return roles;
    }

    public void setRoles(List<String> roles) {
        this.roles = roles;
    }
}
