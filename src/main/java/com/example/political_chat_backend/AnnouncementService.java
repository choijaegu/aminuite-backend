package com.example.political_chat_backend;

import com.example.political_chat_backend.AnnouncementDto;
import com.example.political_chat_backend.CreateAnnouncementRequestDto;
import com.example.political_chat_backend.ResourceNotFoundException; // 사용자 정의 예외 (아래 참고)
import com.example.political_chat_backend.Announcement;
import com.example.political_chat_backend.AnnouncementRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime; // 필요시 사용

@Service
public class AnnouncementService {

    private final AnnouncementRepository announcementRepository;

    @Autowired
    public AnnouncementService(AnnouncementRepository announcementRepository) {
        this.announcementRepository = announcementRepository;
    }

    /**
     * 모든 공지사항을 페이징하여 조회합니다.
     * @param pageable 페이징 정보
     * @return 페이징된 공지사항 DTO 목록
     */
    @Transactional(readOnly = true)
    public Page<AnnouncementDto> getAllAnnouncements(Pageable pageable) {
        Page<Announcement> announcementsPage = announcementRepository.findAll(pageable);
        return announcementsPage.map(AnnouncementDto::fromEntity); // 엔티티를 DTO로 변환
    }

    /**
     * ID로 특정 공지사항을 조회합니다.
     * @param id 조회할 공지사항 ID
     * @return AnnouncementDto (Optional 대신 예외 처리 사용 가능)
     * @throws ResourceNotFoundException 해당 ID의 공지사항이 없을 경우
     */
    @Transactional(readOnly = true)
    public AnnouncementDto getAnnouncementById(Long id) {
        Announcement announcement = announcementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Announcement not found with id: " + id));
        return AnnouncementDto.fromEntity(announcement);
    }

    /**
     * 새 공지사항을 생성합니다.
     * @param requestDto 공지사항 생성 요청 DTO (title, content)
     * @param authorUsername 작성자 사용자 이름 (인증된 사용자로부터 가져옴)
     * @return 생성된 공지사항 DTO
     */
    @Transactional
    public AnnouncementDto createAnnouncement(CreateAnnouncementRequestDto requestDto, String authorUsername) {
        Announcement announcement = new Announcement();
        announcement.setTitle(requestDto.getTitle());
        announcement.setContent(requestDto.getContent());
        announcement.setAuthor(authorUsername); // 작성자 설정
        // createdAt, updatedAt은 @CreationTimestamp, @UpdateTimestamp에 의해 자동 관리

        Announcement savedAnnouncement = announcementRepository.save(announcement);
        return AnnouncementDto.fromEntity(savedAnnouncement);
    }

    /**
     * 기존 공지사항을 수정합니다.
     * @param id 수정할 공지사항 ID
     * @param requestDto 수정할 내용 DTO (title, content)
     * @param authorUsername 수정자 사용자 이름 (권한 확인 또는 로그용, 현재는 미사용)
     * @return 수정된 공지사항 DTO
     * @throws ResourceNotFoundException 해당 ID의 공지사항이 없을 경우
     */
    @Transactional
    public AnnouncementDto updateAnnouncement(Long id, CreateAnnouncementRequestDto requestDto, String authorUsername) {
        Announcement announcement = announcementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Announcement not found with id: " + id));

        // TODO: 여기서 authorUsername을 사용하여 수정 권한을 확인할 수 있습니다.
        // (예: if (!announcement.getAuthor().equals(authorUsername) && !isAdmin(authorUsername)) throw new AccessDeniedException(...) )
        // 지금은 컨트롤러에서 ADMIN 역할만 이 메소드를 호출한다고 가정합니다.

        announcement.setTitle(requestDto.getTitle());
        announcement.setContent(requestDto.getContent());
        // updatedAt은 @UpdateTimestamp에 의해 자동 업데이트됨
        // author는 최초 작성자를 유지하거나, 'lastModifiedBy' 필드를 따로 둘 수 있습니다. 여기서는 원본 author 유지.

        Announcement updatedAnnouncement = announcementRepository.save(announcement);
        return AnnouncementDto.fromEntity(updatedAnnouncement);
    }

    /**
     * ID로 공지사항을 삭제합니다.
     * @param id 삭제할 공지사항 ID
     * @param authorUsername 삭제자 사용자 이름 (권한 확인 또는 로그용, 현재는 미사용)
     * @throws ResourceNotFoundException 해당 ID의 공지사항이 없을 경우
     */
    @Transactional
    public void deleteAnnouncement(Long id, String authorUsername) {
        Announcement announcement = announcementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Announcement not found with id: " + id));

        // TODO: 여기서 authorUsername을 사용하여 삭제 권한을 확인할 수 있습니다.
        // 지금은 컨트롤러에서 ADMIN 역할만 이 메소드를 호출한다고 가정합니다.

        announcementRepository.delete(announcement);
    }
}
