package com.example.political_chat_backend;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.JoinColumn;
import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonBackReference;

@Entity
public class ChatRoom {

    @Id
    @Column(unique = true, nullable = false)
    private String roomId;

    @Column(nullable = false)
    private String name;

    private LocalDateTime createdAt;

    @ManyToOne
    @JoinColumn(name = "category_id", nullable = false)
    @JsonBackReference
    private CommunityCategory category;

    @Column(nullable = false) // 방장은 필수 정보라고 가정
    private String ownerUsername; // 방 생성자 (방장)의 사용자 이름

    protected ChatRoom() {}

    // 생성자 수정: ownerUsername 파라미터 추가
    public ChatRoom(String roomId, String name, CommunityCategory category, String ownerUsername) {
        this.roomId = roomId;
        this.name = name;
        this.category = category;
        this.ownerUsername = ownerUsername; // ownerUsername 설정
        this.createdAt = LocalDateTime.now();
    }

    // Getters
    public String getRoomId() { return roomId; }
    public String getName() { return name; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public CommunityCategory getCategory() { return category; }
    public String getOwnerUsername() { return ownerUsername; } // ownerUsername getter 추가

    // Setters
    public void setName(String name) { this.name = name; }
    public void setCategory(CommunityCategory category) { this.category = category; }
    public void setOwnerUsername(String ownerUsername) { this.ownerUsername = ownerUsername; } // ownerUsername setter 추가
}
