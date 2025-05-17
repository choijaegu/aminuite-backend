package com.example.political_chat_backend;

import java.util.Set;

public class ChatMessage {

    private String sender;    // 메시지를 보낸 사람
    private String content;   // 메시지 내용
    private MessageType type; // 메시지 타입 (예: CHAT, JOIN, LEAVE) - 일단 CHAT만 사용
    private String roomId;
    private Set<String> users; // 현재 사용자 목록 (USER_LIST_UPDATE 메시지용)
    private int userCount;     // 현재 사용자 수 (USER_LIST_UPDATE 메시지용)
    // 메시지 타입을 위한 Enum (선택 사항이지만, 메시지 종류를 구분할 때 유용)
    public enum MessageType {
        CHAT,
        JOIN,
        LEAVE,
        USER_LIST_UPDATE // 사용자 목록/인원수 업데이트용 타입 추가
    }

    // 기본 생성자
    public ChatMessage() {
    }

    // 모든 필드를 받는 생성자 (편의상)
    public ChatMessage(String sender, String content, MessageType type, String roomId) {
        this.sender = sender;
        this.content = content;
        this.type = type;
        this.roomId = roomId; // <<--- roomId 초기화 추가
    }

    // Getter와 Setter 메소드들
    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public MessageType getType() {
        return type;
    }

    public void setType(MessageType type) {
        this.type = type;
    }

    public String getRoomId() {
        return roomId;
    }
    // <<--- Getter 추가
    public void setRoomId(String roomId) {
        this.roomId = roomId;
    } // <<--- Setter 추가
    public Set<String> getUsers() { return users; }
    public void setUsers(Set<String> users) { this.users = users; }
    public int getUserCount() { return userCount; }
    public void setUserCount(int userCount) { this.userCount = userCount; }
}
