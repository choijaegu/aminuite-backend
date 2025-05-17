package com.example.political_chat_backend;

import java.time.LocalDateTime;

public class ChatRoomDto {
    private String roomId;
    private String name;
    private LocalDateTime createdAt;
    private String categoryId; // 부모 카테고리 ID (선택적)
    private int currentUserCount; // 현재 사용자 수

    // 기본 생성자
    public ChatRoomDto() {}

    // 모든 필드를 받는 생성자 (필요에 따라 만듦)
    public ChatRoomDto(String roomId, String name, LocalDateTime createdAt, String categoryId, int currentUserCount) {
        this.roomId = roomId;
        this.name = name;
        this.createdAt = createdAt;
        this.categoryId = categoryId;
        this.currentUserCount = currentUserCount;
    }

    // ChatRoom 엔티티를 ChatRoomDto로 변환하는 정적 메소드 (선택적이지만 편리함)
    public static ChatRoomDto fromEntity(ChatRoom chatRoom, int currentUserCount) {
        return new ChatRoomDto(
                chatRoom.getRoomId(),
                chatRoom.getName(),
                chatRoom.getCreatedAt(),
                chatRoom.getCategory() != null ? chatRoom.getCategory().getCategoryId() : null,
                currentUserCount
        );
    }

    // Getters and Setters
    public String getRoomId() { return roomId; }
    public void setRoomId(String roomId) { this.roomId = roomId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public String getCategoryId() { return categoryId; }
    public void setCategoryId(String categoryId) { this.categoryId = categoryId; }
    public int getCurrentUserCount() { return currentUserCount; }
    public void setCurrentUserCount(int currentUserCount) { this.currentUserCount = currentUserCount; }
}
