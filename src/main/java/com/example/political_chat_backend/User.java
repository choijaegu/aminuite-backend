package com.example.political_chat_backend;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank; // 유효성 검사용 (선택 사항)
import jakarta.validation.constraints.Size;   // 유효성 검사용 (선택 사항)

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "users", // 테이블 이름을 'users'로 지정 (user는 예약어인 경우가 많음)
        uniqueConstraints = { // 유니크 제약 조건 설정
                @UniqueConstraint(columnNames = "username")
                // 필요하다면 email 등 다른 필드에도 유니크 제약 추가 가능
        })
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // DB가 ID 자동 생성
    private Long id;

    @NotBlank
    @Size(max = 20)
    @Column(nullable = false, unique = true)
    private String username; // 사용자 아이디 (로그인 시 사용)

    @NotBlank
    @Size(max = 120) // BCrypt 해시 길이를 고려하여 충분히 길게 설정
    @Column(nullable = false)
    private String password; // 암호화된 비밀번호 저장

    // 역할(Role) 관리 - 간단하게 문자열 Set으로 시작 (추후 Role 엔티티로 분리 가능)
    // 예: "ROLE_USER", "ROLE_ADMIN"
    @ElementCollection(fetch = FetchType.EAGER) // 사용자가 로드될 때 역할도 함께 로드
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "role")
    private Set<String> roles = new HashSet<>();

    // 기본 생성자 (JPA 명세상 필요)
    public User() {
    }

    // 필수 필드를 받는 생성자
    public User(String username, String password) {
        this.username = username;
        this.password = password;
    }

    // Getters and Setters
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

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        // 비밀번호는 서비스 단에서 암호화하여 설정해야 함
        this.password = password;
    }

    public Set<String> getRoles() {
        return roles;
    }

    public void setRoles(Set<String> roles) {
        this.roles = roles;
    }
}
