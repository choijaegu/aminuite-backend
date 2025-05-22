package com.example.political_chat_backend;

import com.example.political_chat_backend.Announcement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
// 페이징 처리를 위해 Page, Pageable 임포트 (나중에 목록 조회 시 사용)
// import org.springframework.data.domain.Page;
// import org.springframework.data.domain.Pageable;

@Repository
public interface AnnouncementRepository extends JpaRepository<Announcement, Long> {

    // 필요에 따라 커스텀 쿼리 메소드 추가 가능
    // 예: 제목으로 검색 (페이징 포함)
    // Page<Announcement> findByTitleContainingIgnoreCase(String title, Pageable pageable);

    // 예: 중요 공지사항만 가져오기
    // List<Announcement> findByImportantTrueOrderByCreatedAtDesc();
}
