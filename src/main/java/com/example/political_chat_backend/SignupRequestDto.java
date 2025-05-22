package com.example.political_chat_backend;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class SignupRequestDto {

    @NotBlank(message = "사용자 아이디는 필수입니다.")
    @Size(min = 3, max = 20, message = "사용자 아이디는 3자 이상 20자 이하로 입력해주세요.")
    private String username;

    @NotBlank(message = "비밀번호는 필수입니다.")
    @Size(min = 6, max = 40, message = "비밀번호는 6자 이상 40자 이하로 입력해주세요.") // BCrypt 해시는 길어지므로 원본 비밀번호 길이 제한은 적절히
    private String password;

    // 추가적으로 받고 싶은 정보가 있다면 여기에 필드 추가 (예: email, nickname 등)
    // private String email;

    // Getters and Setters
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
        this.password = password;
    }

    // public String getEmail() { return email; }
    // public void setEmail(String email) { this.email = email; }
}
