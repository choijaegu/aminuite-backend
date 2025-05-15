package com.example.political_chat_backend;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional; // Optional 사용

public interface ChatRoomRepository extends JpaRepository<ChatRoom, String> {
    // JpaRepository<ChatRoom, String>에서 String은 ChatRoom 엔티티의 @Id 필드 타입입니다.

    // 방 ID로 채팅방을 찾는 메소드 (이미 JpaRepository에 findById가 있지만, 명시적으로 선언 가능)
    Optional<ChatRoom> findByRoomId(String roomId);

    // 방 이름으로 채팅방을 찾는 메소드 (필요하다면)
    // Optional<ChatRoom> findByName(String name);
}