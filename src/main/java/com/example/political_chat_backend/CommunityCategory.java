package com.example.political_chat_backend;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany; // OneToMany 추가
import java.time.LocalDateTime;
import java.util.ArrayList; // ArrayList 추가
import java.util.List; // List 추가
import com.fasterxml.jackson.annotation.JsonManagedReference; // 순환 참조 방지용  

@Entity
public class CommunityCategory {

    @Id
    @Column(unique = true, nullable = false)
    private String categoryId; // 예: "politics", "humor", "horror"

    @Column(nullable = false)
    private String name; // 예: "정치", "유머", "공포"

    private String description; // 카테고리 설명 (선택 사항)

    private LocalDateTime createdAt;

    // ChatRoom과의 관계 설정 (하나의 카테고리는 여러 채팅방을 가질 수 있음)
    @OneToMany(mappedBy = "category") // ChatRoom 엔티티의 'category' 필드와 매핑됨
    @JsonManagedReference // 순환 참조 방지: Category -> ChatRoom 방향은 직렬화
    private List<ChatRoom> chatRooms = new ArrayList<>();

    protected CommunityCategory() {
    }

    public CommunityCategory(String categoryId, String name, String description) {
        this.categoryId = categoryId;
        this.name = name;
        this.description = description;
        this.createdAt = LocalDateTime.now();
    }

    // Getters (Setters는 필요에 따라)
    public String getCategoryId() {
        return categoryId;
    }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public List<ChatRoom> getChatRooms() { return chatRooms; }

    public void setName(String name) { this.name = name; }
    public void setDescription(String description) { this.description = description; }
}

