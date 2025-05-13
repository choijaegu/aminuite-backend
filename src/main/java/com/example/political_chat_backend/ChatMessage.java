package com.example.political_chat_backend;

public class ChatMessage {

    private String sender;    // 메시지를 보낸 사람
    private String content;   // 메시지 내용
    private MessageType type; // 메시지 타입 (예: CHAT, JOIN, LEAVE) - 일단 CHAT만 사용
    private String roomId;

    // 메시지 타입을 위한 Enum (선택 사항이지만, 메시지 종류를 구분할 때 유용)
    public enum MessageType {
        CHAT,
        JOIN,
        LEAVE
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
}
