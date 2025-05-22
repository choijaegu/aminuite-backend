package com.example.political_chat_backend;

import com.example.political_chat_backend.AnnouncementDto; // DTO 임포트 경로 확인
import com.example.political_chat_backend.CreateAnnouncementRequestDto; // DTO 임포트 경로 확인
import com.example.political_chat_backend.AnnouncementService; // 서비스 임포트 경로 확인
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort; // Sort 임포트 추가
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/announcements")
public class AnnouncementController {

    private final AnnouncementService announcementService;

    @Autowired
    public AnnouncementController(AnnouncementService announcementService) {
        this.announcementService = announcementService;
    }

    /**
     * 모든 공지사항 목록을 페이징하여 조회합니다. (누구나 접근 가능)
     */
    @GetMapping
    public ResponseEntity<Page<AnnouncementDto>> getAllAnnouncements(
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        //                                  ^^^^^^^^^^^^^^  ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
        //                                  sort 필드명만, direction으로 정렬 방향 지정
        Page<AnnouncementDto> announcements = announcementService.getAllAnnouncements(pageable);
        return ResponseEntity.ok(announcements);
    }

    /**
     * ID로 특정 공지사항을 조회합니다. (누구나 접근 가능)
     */
    @GetMapping("/{id}")
    public ResponseEntity<AnnouncementDto> getAnnouncementById(@PathVariable Long id) {
        AnnouncementDto announcementDto = announcementService.getAnnouncementById(id);
        return ResponseEntity.ok(announcementDto);
    }

    /**
     * 새 공지사항을 생성합니다. (관리자만 가능)
     */
    @PostMapping
    public ResponseEntity<?> createAnnouncement(@Valid @RequestBody CreateAnnouncementRequestDto requestDto,
                                                Authentication authentication) {
        if (!isAdmin(authentication)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("공지사항 작성 권한이 없습니다.");
        }
        String authorUsername = authentication.getName();
        AnnouncementDto createdAnnouncement = announcementService.createAnnouncement(requestDto, authorUsername);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdAnnouncement);
    }

    /**
     * 기존 공지사항을 수정합니다. (관리자만 가능)
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateAnnouncement(@PathVariable Long id,
                                                @Valid @RequestBody CreateAnnouncementRequestDto requestDto,
                                                Authentication authentication) {
        if (!isAdmin(authentication)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("공지사항 수정 권한이 없습니다.");
        }
        String authorUsername = authentication.getName();
        try {
            AnnouncementDto updatedAnnouncement = announcementService.updateAnnouncement(id, requestDto, authorUsername);
            return ResponseEntity.ok(updatedAnnouncement);
        } catch (Exception e) { // 예를 들어 ResourceNotFoundException
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    /**
     * ID로 공지사항을 삭제합니다. (관리자만 가능)
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteAnnouncement(@PathVariable Long id,
                                                Authentication authentication) {
        if (!isAdmin(authentication)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("공지사항 삭제 권한이 없습니다.");
        }
        String authorUsername = authentication.getName();
        try {
            announcementService.deleteAnnouncement(id, authorUsername);
            return ResponseEntity.ok().body("공지사항이 성공적으로 삭제되었습니다. (ID: " + id + ")");
        } catch (Exception e) { // 예를 들어 ResourceNotFoundException
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    /**
     * 현재 인증된 사용자가 관리자(ROLE_ADMIN) 권한을 가지고 있는지 확인하는 헬퍼 메소드
     */
    private boolean isAdmin(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(role -> role.equals("ROLE_ADMIN"));
    }
}