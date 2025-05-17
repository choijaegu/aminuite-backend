package com.example.political_chat_backend;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional; // Optional 사용
import java.util.List;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, String> {
    Optional<ChatRoom> findByRoomId(String roomId);
    List<ChatRoom> findByCategory(CommunityCategory category); // 추가
}