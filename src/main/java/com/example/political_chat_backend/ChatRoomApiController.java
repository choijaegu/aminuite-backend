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
        String roomId = payload.get("roomId");
        String name = payload.get("name");

        if (roomId == null || roomId.trim().isEmpty() || name == null || name.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("방 ID와 방 이름은 필수입니다.");
        }
        try {
            ChatRoom createdChatRoom = chatRoomService.createChatRoom(categoryId, roomId, name);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdChatRoom);
        } catch (IllegalArgumentException e) {
            // 카테고리가 없거나, 방 ID가 중복되거나 할 때 예외 메시지를 그대로 전달
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    // 모든 채팅방 목록 (이것은 유지하거나, 카테고리별 목록 조회로 대체/보강)
    @GetMapping("/chatrooms")
    public ResponseEntity<List<ChatRoom>> getAllChatRooms() {
        List<ChatRoom> chatRooms = chatRoomService.findAllChatRooms();
        return ResponseEntity.ok(chatRooms);
    }

    // 특정 카테고리 내의 채팅방 목록 조회
    @GetMapping("/categories/{categoryId}/chatrooms")
    public ResponseEntity<?> getChatRoomsByCategory(@PathVariable String categoryId) {
        try {
            List<ChatRoom> chatRooms = chatRoomService.findChatRoomsByCategoryId(categoryId);
            return ResponseEntity.ok(chatRooms);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build(); // 해당 카테고리가 없을 경우
        }
    }

    @GetMapping("/chatrooms/{roomId}")
    public ResponseEntity<?> getChatRoomById(@PathVariable String roomId) {
        return chatRoomService.findChatRoomById(roomId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
