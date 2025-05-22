package com.example.political_chat_backend;

import com.example.political_chat_backend.KickRequestDto;
import com.example.political_chat_backend.ChatAdminService;
import com.example.political_chat_backend.ChatRoomService; // isRoomOwner 및 방 존재 확인용

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication; // Spring Security 사용 시 (없으면 주석 처리 또는 다른 인증 방식 사용)
// import org.springframework.security.access.prepost.PreAuthorize; // 메소드 레벨 보안 사용 시
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chatrooms/{roomId}/admin") // 방 ID별 관리 기능의 기본 경로
public class ChatRoomAdminController {

    private final ChatAdminService chatAdminService;
    private final ChatRoomService chatRoomService;

    @Autowired
    public ChatRoomAdminController(ChatAdminService chatAdminService, ChatRoomService chatRoomService) {
        this.chatAdminService = chatAdminService;
        this.chatRoomService = chatRoomService;
    }

    @PostMapping("/kick")
    // @PreAuthorize("isAuthenticated()") // Spring Security와 메소드 레벨 보안을 사용한다면 주석 해제
    public ResponseEntity<?> kickUser(
            @PathVariable String roomId,
            @RequestBody KickRequestDto kickRequest,
            Authentication authentication // Spring Security를 사용한다면 주입됨, 아니라면 다른 방법으로 요청자 정보 확인
    ) {
        // Spring Security를 사용하지 않는 경우, 요청자(kicker) 정보를 다른 방식으로 전달받거나 확인해야 합니다.
        // 예를 들어, 요청 헤더의 토큰에서 추출하거나, 세션 정보를 활용할 수 있습니다.
        // 여기서는 Spring Security의 Authentication 객체를 사용하는 것으로 가정합니다.
        // 만약 Spring Security를 사용하지 않는다면, 이 부분을 적절히 수정해야 합니다.
        if (authentication == null || !authentication.isAuthenticated()) {
            // 이 부분은 Spring Security 미사용 시, 또는 인증 필터가 없을 경우 필요합니다.
            // SecurityConfig에서 모든 /api/** 경로에 대해 인증을 요구하도록 설정했다면,
            // 인증되지 않은 요청은 여기까지 도달하지 않을 수 있습니다.
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인이 필요합니다.");
        }
        String requesterUsername = authentication.getName(); // 강퇴를 시도하는 사람 (로그인한 사용자)

        // 0. 입력값 검증
        if (kickRequest.getUsernameToKick() == null || kickRequest.getUsernameToKick().trim().isEmpty()) {
            return ResponseEntity.badRequest().body("강퇴할 사용자의 아이디(username)는 필수입니다.");
        }

        // 1. 채팅방 존재 여부 확인
        if (!chatRoomService.findChatRoomById(roomId).isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("채팅방을 찾을 수 없습니다: " + roomId);
        }

        // 2. 강퇴 권한 확인 (요청자가 해당 방의 방장인지)
        if (!chatRoomService.isRoomOwner(roomId, requesterUsername)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("이 채팅방에서 사용자를 강퇴할 권한이 없습니다.");
        }

        // 3. 강퇴 서비스 호출
        try {
            chatAdminService.kickUserFromRoom(roomId, requesterUsername, kickRequest.getUsernameToKick());
            return ResponseEntity.ok().body(kickRequest.getUsernameToKick() + " 사용자를 성공적으로 강퇴했습니다.");
        } catch (IllegalArgumentException e) { // 서비스 로직에서 발생할 수 있는 예외 (예: 대상 유저 없음, 자신을 강퇴)
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            // 예상치 못한 기타 서버 오류 로깅
            // logger.error("Error kicking user {} from room {}: {}", kickRequest.getUsernameToKick(), roomId, e.getMessage(), e);
            System.err.println("Error during kick operation: " + e.getMessage());
            e.printStackTrace(); // 개발 중에는 스택 트레이스 출력, 운영 환경에서는 로거 사용
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("사용자 강퇴 처리 중 서버 내부 오류가 발생했습니다.");
        }
    }
}
