package com.example.political_chat_backend;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.CreationTimestamp; // 생성 시간 자동 관리
import org.hibernate.annotations.UpdateTimestamp;  // 수정 시간 자동 관리

import java.time.LocalDateTime;

@Entity
@Table(name = "announcements")
public class Announcement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "제목은 필수입니다.")
    @Size(max = 255)
    @Column(nullable = false)
    private String title;

    @NotBlank(message = "내용은 필수입니다.")
    @Lob // 긴 텍스트 내용을 위해 LOB (Large Object) 타입 지정
    @Column(nullable = false, columnDefinition = "TEXT") // DB 컬럼 타입을 TEXT로 명시 (PostgreSQL의 경우)
    private String content;

    @Size(max = 50)
    @Column(nullable = false)
    private String author; // 작성자 (예: 관리자 아이디 또는 이름)
    // 추후 User 엔티티와 연관관계를 맺을 수도 있습니다.

    @CreationTimestamp // 엔티티 생성 시 자동으로 현재 시간 저장
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp // 엔티티 수정 시 자동으로 현재 시간 저장
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    // 조회수, 중요 공지 여부 등 필요에 따라 필드 추가 가능
    // private int viewCount = 0;
    // private boolean important = false;

    // 기본 생성자 (JPA 명세)
    public Announcement() {
    }

    // 필수 필드를 받는 생성자 (선택 사항)
    public Announcement(String title, String content, String author) {
        this.title = title;
        this.content = content;
        this.author = author;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    // public int getViewCount() { return viewCount; }
    // public void setViewCount(int viewCount) { this.viewCount = viewCount; }
    // public boolean isImportant() { return important; }
    // public void setImportant(boolean important) { this.important = important; }
}
