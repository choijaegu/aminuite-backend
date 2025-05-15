package com.example.political_chat_backend;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne; // ManyToOne 추가
import jakarta.persistence.JoinColumn; // JoinColumn 추가
import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonBackReference; // 순환 참조 방지용

@Entity
public class ChatRoom {

    @Id
    @Column(unique = true, nullable = false)
    private String roomId;

    @Column(nullable = false)
    private String name;

    private LocalDateTime createdAt;

    // CommunityCategory와의 관계 설정 (여러 채팅방은 하나의 카테고리에 속할 수 있음)
    @ManyToOne // N:1 관계
    @JoinColumn(name = "category_id", nullable = false) // 외래키 컬럼 이름 및 not null 설정
    @JsonBackReference // 순환 참조 방지: ChatRoom -> Category 방향은 직렬화에서 제외될 수 있음
    private CommunityCategory category;

    protected ChatRoom() {}

    // 생성자 수정: CommunityCategory 파라미터 추가
    public ChatRoom(String roomId, String name, CommunityCategory category) {
        this.roomId = roomId;
        this.name = name;
        this.category = category;
        this.createdAt = LocalDateTime.now();
    }

    // Getters (Setters는 필요에 따라)
    public String getRoomId() { return roomId; }
    public String getName() { return name; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public CommunityCategory getCategory() { return category; } // category getter 추가

    public void setName(String name) { this.name = name; }
    public void setCategory(CommunityCategory category) { this.category = category; } // category setter 추가
}
