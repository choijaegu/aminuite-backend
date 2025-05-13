package com.example.political_chat_backend;

import org.springframework.data.jpa.repository.JpaRepository;
// 필요하다면 나중에 특정 조건으로 메시지를 찾는 메소드를 추가할 수 있습니다.
// import java.util.List;

public interface PersistedChatMessageRepository extends JpaRepository<PersistedChatMessage, Long> {
    // JpaRepository를 상속받는 것만으로 기본적인 DB 작업(save, findById, findAll 등)이 가능합니다.
    // 예시: 특정 사용자의 메시지 찾기 (나중에 필요하면 주석 해제 후 사용)
    // List<PersistedChatMessage> findBySenderOrderByTimestampDesc(String sender);
}
