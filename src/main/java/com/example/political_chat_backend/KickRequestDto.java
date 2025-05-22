package com.example.political_chat_backend;

public class KickRequestDto {
    private String usernameToKick; // 강퇴할 사용자의 username

    // 기본 생성자 (JSON 직렬화/역직렬화를 위해 필요할 수 있음)
    public KickRequestDto() {
    }

    // Getter
    public String getUsernameToKick() {
        return usernameToKick;
    }

    // Setter
    public void setUsernameToKick(String usernameToKick) {
        this.usernameToKick = usernameToKick;
    }
}
