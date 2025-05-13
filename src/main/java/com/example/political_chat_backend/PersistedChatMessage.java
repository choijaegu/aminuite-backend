package com.example.political_chat_backend;


import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.EnumType; // EnumType을 위해 추가
import jakarta.persistence.Enumerated; // Enumerated를 위해 추가
import java.time.LocalDateTime;

@Entity // 이 클래스가 JPA 엔티티임을 나타냅니다.
public class PersistedChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String sender;
    private String content;

    @Enumerated(EnumType.STRING) // Enum 타입을 문자열로 저장합니다.
    private ChatMessage.MessageType messageType; // ChatMessage DTO의 MessageType enum 재사용

    private LocalDateTime timestamp; // 메시지 수신 또는 생성 시간

    protected PersistedChatMessage() {} // JPA를 위한 기본 생성자

    public PersistedChatMessage(String sender, String content, ChatMessage.MessageType messageType) {
        this.sender = sender;
        this.content = content;
        this.messageType = messageType;
        this.timestamp = LocalDateTime.now(); // 객체 생성 시 현재 시간으로 타임스탬프 설정
    }

    // Getter와 Setter 메소드들 (필요에 따라 추가)
    public Long getId() { return id; }
    public String getSender() { return sender; }
    public String getContent() { return content; }
    public ChatMessage.MessageType getMessageType() { return messageType; }
    public LocalDateTime getTimestamp() { return timestamp; }

    // Setter는 필요시 추가합니다. 예를 들어, ID는 자동 생성되므로 보통 Setter가 필요 없습니다.
    // public void setSender(String sender) { this.sender = sender; }
    // public void setContent(String content) { this.content = content; }
    // public void setMessageType(ChatMessage.MessageType messageType) { this.messageType = messageType; }
    // public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}
