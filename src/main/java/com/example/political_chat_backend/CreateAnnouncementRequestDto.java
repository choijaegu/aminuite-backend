package com.example.political_chat_backend;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CreateAnnouncementRequestDto {

    @NotBlank(message = "공지사항 제목은 필수입니다.")
    @Size(min = 1, max = 255, message = "제목은 1자 이상 255자 이하로 입력해주세요.")
    private String title;

    @NotBlank(message = "공지사항 내용은 필수입니다.")
    private String content; // 내용은 @Lob 등을 사용하므로 길이 제한은 엔티티에서 처리

    // Getters and Setters
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}