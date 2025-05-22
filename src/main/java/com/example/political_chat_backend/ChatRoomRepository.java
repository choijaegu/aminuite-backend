package com.example.political_chat_backend;

import com.example.political_chat_backend.ChatRoom;
import com.example.political_chat_backend.CommunityCategory;
import org.springframework.data.domain.Page; // Page 임포트
import org.springframework.data.domain.Pageable; // Pageable 임포트
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ChatRoomRepository extends JpaRepository<ChatRoom, String> {

    Optional<ChatRoom> findByRoomId(String roomId);

    // 특정 카테고리에 속한 채팅방 목록을 페이징하여 조회
    Page<ChatRoom> findByCategory(CommunityCategory category, Pageable pageable);

    // 모든 채팅방 목록을 페이징하여 조회 (기존 findAll() 메소드를 오버라이드하는 형태)
    @Override // JpaRepository의 findAll(Pageable)을 명시적으로 사용
    Page<ChatRoom> findAll(Pageable pageable);
}