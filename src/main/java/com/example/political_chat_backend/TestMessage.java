package com.example.political_chat_backend;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity // 이 클래스가 JPA 엔티티임을 나타냅니다. 데이터베이스 테이블과 매핑됩니다.
public class TestMessage {

    @Id // 이 필드가 테이블의 기본 키(Primary Key)임을 나타냅니다.
    @GeneratedValue(strategy = GenerationType.IDENTITY) // 기본 키 값을 데이터베이스가 자동으로 생성하도록 합니다 (PostgreSQL의 경우 IDENTITY 전략 사용).
    private Long id;

    private String content; // 메시지 내용을 저장할 필드

    // JPA는 기본 생성자를 필요로 합니다.
    protected TestMessage() {}

    // 내용을 받아 객체를 생성하는 생성자
    public TestMessage(String content) {
        this.content = content;
    }

    // Getter 메소드 (필요에 따라 Setter도 추가할 수 있습니다)
    public Long getId() {
        return id;
    }

    public String getContent() {
        return content;
    }

    @Override
    public String toString() {
        return "TestMessage{" +
                "id=" + id +
                ", content='" + content + '\'' +
                '}';
    }
}
