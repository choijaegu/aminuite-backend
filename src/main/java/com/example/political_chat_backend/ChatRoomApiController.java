package com.example.political_chat_backend;

import com.example.political_chat_backend.ChatRoomDto; // ChatRoomDto 임포트
import com.example.political_chat_backend.ChatRoom;
import com.example.political_chat_backend.CommunityCategory;
import com.example.political_chat_backend.ChatRoomService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page; // Page 임포트
import org.springframework.data.domain.Pageable; // Pageable 임포트
import org.springframework.data.web.PageableDefault; // PageableDefault 임포트
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication; // 인증된 사용자 정보 가져오기 위해 추가
import org.springframework.web.bind.annotation.*;

import java.util.List; // List는 이제 Page로 대체될 수 있음
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ChatRoomApiController {

    private final ChatRoomService chatRoomService;

    @Autowired
    public ChatRoomApiController(ChatRoomService chatRoomService) {
        this.chatRoomService = chatRoomService;
    }

    // --- 커뮤니티 카테고리 API (기존과 동일하거나, 필요시 페이징 적용) ---
    @PostMapping("/categories")
    public ResponseEntity<?> createCommunityCategory(@RequestBody Map<String, String> payload) {
        String categoryId = payload.get("categoryId");
        String name = payload.get("name");
        String description = payload.get("description");

        if (categoryId == null || categoryId.trim().isEmpty() || name == null || name.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("카테고리 ID와 이름은 필수입니다.");
        }
        try {
            CommunityCategory createdCategory = chatRoomService.createCommunityCategory(categoryId, name, description);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdCategory);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        }
    }

    @GetMapping("/categories")
    public ResponseEntity<List<CommunityCategory>> getAllCategories() {
        // 카테고리 수가 매우 많지 않다면 페이징 없이 List로 반환해도 무방할 수 있습니다.
        // 많다면 이 부분도 페이징 적용 필요.
        List<CommunityCategory> categories = chatRoomService.findAllCategories();
        return ResponseEntity.ok(categories);
    }

    @GetMapping("/categories/{categoryId}")
    public ResponseEntity<?> getCategoryById(@PathVariable String categoryId) {
        return chatRoomService.findCategoryById(categoryId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // --- 채팅방 API ---
    @PostMapping("/categories/{categoryId}/chatrooms")
    public ResponseEntity<?> createChatRoom(@PathVariable String categoryId,
                                            @RequestBody Map<String, String> payload,
                                            Authentication authentication) { // Authentication 객체로 현재 사용자 정보 가져오기
        String name = payload.get("name");
        // String ownerUsername = payload.get("ownerUsername"); // 클라이언트에서 보내는 대신 인증된 사용자 정보 사용

        if (name == null || name.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("방 이름은 필수입니다.");
        }
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("채팅방을 생성하려면 로그인이 필요합니다.");
        }
        String ownerUsername = authentication.getName(); // 인증된 사용자의 이름을 방장으로 설정

        try {
            ChatRoom createdChatRoom = chatRoomService.createChatRoom(categoryId, name, ownerUsername);
            // 생성된 ChatRoom 객체 대신 ChatRoomDto로 변환하여 반환하는 것이 더 일반적일 수 있습니다.
            // 여기서는 간단히 ChatRoom 객체 반환.
            return ResponseEntity.status(HttpStatus.CREATED).body(createdChatRoom);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @GetMapping("/chatrooms") // 전체 채팅방 목록 (페이징 적용)
    public ResponseEntity<Page<ChatRoomDto>> getAllChatRooms(
            @PageableDefault(size = 10, sort = "createdAt") Pageable pageable // 기본 10개, 생성일 기준 정렬
    ) {
        Page<ChatRoomDto> chatRoomDtosPage = chatRoomService.findAllChatRooms(pageable);
        return ResponseEntity.ok(chatRoomDtosPage);
    }

    @GetMapping("/categories/{categoryId}/chatrooms") // 카테고리별 채팅방 목록 (페이징 적용)
    public ResponseEntity<?> getChatRoomsByCategory(
            @PathVariable String categoryId,
            @PageableDefault(size = 10, sort = "createdAt") Pageable pageable // 기본 10개, 생성일 기준 정렬
    ) {
        try {
            Page<ChatRoomDto> chatRoomDtosPage = chatRoomService.findChatRoomsByCategoryId(categoryId, pageable);
            return ResponseEntity.ok(chatRoomDtosPage);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @GetMapping("/chatrooms/{roomId}") // 단일 채팅방 조회 (DTO 반환)
    public ResponseEntity<?> getChatRoomById(@PathVariable String roomId) {
        // ChatRoomService에 findChatRoomDtoById 메소드가 ChatRoomDto를 반환한다고 가정
        return chatRoomService.findChatRoomDtoById(roomId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
