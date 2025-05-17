package com.example.political_chat_backend;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api") // 기본 경로를 /api로 변경 (카테고리와 채팅방 구분 위해)
public class ChatRoomApiController {

    private final ChatRoomService chatRoomService;

    @Autowired
    public ChatRoomApiController(ChatRoomService chatRoomService) {
        this.chatRoomService = chatRoomService;
    }

    // --- 커뮤니티 카테고리 API ---
    @PostMapping("/categories")
    public ResponseEntity<?> createCommunityCategory(@RequestBody Map<String, String> payload) {
        String categoryId = payload.get("categoryId");
        String name = payload.get("name");
        String description = payload.get("description"); // 선택 사항

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
    // 채팅방 생성 시 이제 categoryId도 필요하므로 경로에 포함
    @PostMapping("/categories/{categoryId}/chatrooms")
    public ResponseEntity<?> createChatRoom(@PathVariable String categoryId, @RequestBody Map<String, String> payload) {
        // String roomId = payload.get("roomId"); // 클라이언트로부터 roomId를 받지 않음
        String name = payload.get("name");
        String ownerUsername = payload.get("ownerUsername");

        // roomId 유효성 검사 제거, name과 ownerUsername만 체크
        if (name == null || name.trim().isEmpty() ||
                ownerUsername == null || ownerUsername.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("방 이름과 생성자 닉네임은 필수입니다.");
        }
        try {
            // ChatRoomService 호출 시 roomId를 전달하지 않음
            ChatRoom createdChatRoom = chatRoomService.createChatRoom(categoryId, name, ownerUsername);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdChatRoom);
        } catch (IllegalArgumentException | IllegalStateException e) { // IllegalStateException 추가
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @GetMapping("/chatrooms")
    public ResponseEntity<List<ChatRoomDto>> getAllChatRooms() { // 반환 타입 변경
        List<ChatRoomDto> chatRoomDtos = chatRoomService.findAllChatRooms();
        return ResponseEntity.ok(chatRoomDtos);
    }

    @GetMapping("/categories/{categoryId}/chatrooms")
    public ResponseEntity<?> getChatRoomsByCategory(@PathVariable String categoryId) { // 반환 타입 변경 (List<ChatRoomDto>)
        try {
            List<ChatRoomDto> chatRoomDtos = chatRoomService.findChatRoomsByCategoryId(categoryId);
            return ResponseEntity.ok(chatRoomDtos);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage()); // 더 명확한 오류 응답
        }
    }

    @GetMapping("/chatrooms/{roomId}")
    public ResponseEntity<?> getChatRoomById(@PathVariable String roomId) {
        return chatRoomService.findChatRoomById(roomId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
