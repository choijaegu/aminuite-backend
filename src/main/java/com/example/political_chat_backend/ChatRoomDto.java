package com.example.political_chat_backend; // 사용자님의 패키지 경로 유지

import com.example.political_chat_backend.ChatRoom; // ChatRoom 엔티티 임포트 (경로 확인 필요)
import java.time.LocalDateTime;

public class ChatRoomDto {
    private String roomId;
    private String name;
    private LocalDateTime createdAt;
    private String categoryId;    // 부모 카테고리 ID
    private String ownerUsername; // <<--- 채팅방 소유자 아이디 필드 추가!
    private int currentUserCount; // 현재 사용자 수

    // 기본 생성자
    public ChatRoomDto() {}

    // 모든 필드를 받는 생성자 (ownerUsername 추가)
    public ChatRoomDto(String roomId, String name, LocalDateTime createdAt, String categoryId, String ownerUsername, int currentUserCount) {
        this.roomId = roomId;
        this.name = name;
        this.createdAt = createdAt;
        this.categoryId = categoryId;
        this.ownerUsername = ownerUsername; // ownerUsername 초기화
        this.currentUserCount = currentUserCount;
    }

    // ChatRoom 엔티티를 ChatRoomDto로 변환하는 정적 메소드 (ownerUsername 추가)
    public static ChatRoomDto fromEntity(ChatRoom chatRoom, int currentUserCount) {
        if (chatRoom == null) {
            return null;
        }
        return new ChatRoomDto(
                chatRoom.getRoomId(),
                chatRoom.getName(),
                chatRoom.getCreatedAt(),
                chatRoom.getCategory() != null ? chatRoom.getCategory().getCategoryId() : null,
                chatRoom.getOwnerUsername(), // <<--- chatRoom 엔티티에서 ownerUsername 가져오기!
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

    public String getOwnerUsername() { return ownerUsername; } // ownerUsername getter 추가
    public void setOwnerUsername(String ownerUsername) { this.ownerUsername = ownerUsername; } // ownerUsername setter 추가

    public int getCurrentUserCount() { return currentUserCount; }
    public void setCurrentUserCount(int currentUserCount) { this.currentUserCount = currentUserCount; }
}