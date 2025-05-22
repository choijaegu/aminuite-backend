package com.example.political_chat_backend;

import com.example.political_chat_backend.Announcement; // Announcement 엔티티 임포트
import java.time.LocalDateTime;

public class AnnouncementDto {

    private Long id;
    private String title;
    private String content;
    private String author;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    // private int viewCount; // 필요 시 추가
    // private boolean important; // 필요 시 추가

    // 기본 생성자
    public AnnouncementDto() {}

    // 모든 필드를 받는 생성자 (선택 사항)
    public AnnouncementDto(Long id, String title, String content, String author, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.author = author;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // Announcement 엔티티를 AnnouncementDto로 변환하는 정적 팩토리 메소드
    public static AnnouncementDto fromEntity(Announcement announcement) {
        if (announcement == null) {
            return null;
        }
        return new AnnouncementDto(
                announcement.getId(),
                announcement.getTitle(),
                announcement.getContent(),
                announcement.getAuthor(),
                announcement.getCreatedAt(),
                announcement.getUpdatedAt()
                // announcement.getViewCount(), // 필요 시 추가
                // announcement.isImportant()  // 필요 시 추가
        );
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    // public int getViewCount() { return viewCount; }
    // public void setViewCount(int viewCount) { this.viewCount = viewCount; }
    // public boolean isImportant() { return important; }
    // public void setImportant(boolean important) { this.important = important; }
}
