package com.example.political_chat_backend;

import org.springframework.data.jpa.repository.JpaRepository;

// JpaRepository를 상속받습니다.
// <TestMessage, Long>은 이 리포지토리가 TestMessage 엔티티를 다루고,
// TestMessage 엔티티의 ID 타입이 Long임을 의미합니다.
public interface TestMessageRepository extends JpaRepository<TestMessage, Long> {
    // 기본적인 CRUD 메소드(save, findById, findAll, deleteById 등)는
    // JpaRepository 인터페이스에 이미 정의되어 있으므로,
    // 특별한 경우가 아니면 이 안에는 아무것도 작성하지 않아도 됩니다.
}
