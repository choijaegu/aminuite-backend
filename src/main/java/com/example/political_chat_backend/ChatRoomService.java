package com.example.political_chat_backend;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.hibernate.Hibernate; // Hibernate 프록시 초기화를 위해 추가

import java.util.List;
import java.util.UUID; // UUID 임포트
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ChatRoomService {

    private final ChatRoomRepository chatRoomRepository;
    private final CommunityCategoryRepository communityCategoryRepository; // 추가
    private final ChatRoomUserService chatRoomUserService;

    @Autowired
    public ChatRoomService(ChatRoomRepository chatRoomRepository,
                           CommunityCategoryRepository communityCategoryRepository,
                           ChatRoomUserService chatRoomUserService) { // ChatRoomUserService 주입 확인
        this.chatRoomRepository = chatRoomRepository;
        this.communityCategoryRepository = communityCategoryRepository;
        this.chatRoomUserService = chatRoomUserService; // 주입 확인
    }

    /**
     * 새로운 커뮤니티 카테고리를 생성합니다.
     */
    @Transactional
    public CommunityCategory createCommunityCategory(String categoryId, String name, String description) {
        if (communityCategoryRepository.findByCategoryId(categoryId).isPresent()) {
            throw new IllegalArgumentException("이미 존재하는 카테고리 ID 입니다: " + categoryId);
        }
        CommunityCategory newCategory = new CommunityCategory(categoryId, name, description);
        return communityCategoryRepository.save(newCategory);
    }

    @Transactional(readOnly = true)
    public List<CommunityCategory> findAllCategories() {
        List<CommunityCategory> categories = communityCategoryRepository.findAll();
        // 각 카테고리의 chatRooms 컬렉션을 명시적으로 초기화합니다.
        for (CommunityCategory category : categories) {
            Hibernate.initialize(category.getChatRooms());
            // 또는 category.getChatRooms().size(); 와 같이 컬렉션에 접근하는 코드를 사용해도 됩니다.
            // .size() 호출은 컬렉션의 크기를 알아내기 위해 실제 데이터를 로드하게 만듭니다.
        }
        return categories;
    }

    @Transactional(readOnly = true)
    public Optional<CommunityCategory> findCategoryById(String categoryId) {
        Optional<CommunityCategory> categoryOpt = communityCategoryRepository.findByCategoryId(categoryId);
        // 특정 카테고리를 조회할 때도 해당 카테고리의 chatRooms를 초기화합니다.
        if (categoryOpt.isPresent()) {
            Hibernate.initialize(categoryOpt.get().getChatRooms());
            // 또는 categoryOpt.get().getChatRooms().size();
        }
        return categoryOpt;
    }


    /**
     * 새로운 채팅방을 특정 카테고리 하위에 생성합니다.
     * @param categoryId 이 채팅방이 속할 카테고리 ID
     * @param roomId 방 ID
     * @param name 방 이름
     * @return 생성된 ChatRoom 객체
     */

    @Transactional
    public ChatRoom createChatRoom(String categoryId, String name, String ownerUsername) { // roomId 파라미터 제거됨
        CommunityCategory category = communityCategoryRepository.findByCategoryId(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 카테고리 ID 입니다: " + categoryId));

        // roomId 자동 생성 (예: UUID 사용)
        String newRoomId = UUID.randomUUID().toString().substring(0, 8); // UUID 앞 8자리만 사용 (예시)
        // 또는 방 이름을 기반으로 간단한 ID 생성 로직 추가 가능 (중복 및 특수문자 처리 필요)
        // 예: String newRoomId = name.trim().toLowerCase().replaceAll("\\s+", "-").replaceAll("[^a-z0-9-]", "");
        //     if (newRoomId.length() > 50) newRoomId = newRoomId.substring(0, 50); // 길이 제한
        //     newRoomId = newRoomId + "-" + System.currentTimeMillis() % 1000;


        // 생성된 roomId가 혹시라도 중복되는지 확인 (UUID는 거의 중복 안됨)
        // 실제 서비스에서는 중복 방지를 위한 더 강력한 로직이 필요할 수 있습니다.
        int tentativo = 0;
        while (chatRoomRepository.findByRoomId(newRoomId).isPresent() && tentativo < 5) {
            newRoomId = UUID.randomUUID().toString().substring(0, 8) + "-" + tentativo;
            tentativo++;
        }
        if (chatRoomRepository.findByRoomId(newRoomId).isPresent()){
            throw new IllegalStateException("채팅방 ID 생성에 반복적으로 실패했습니다. 잠시 후 다시 시도해주세요.");
        }


        ChatRoom newChatRoom = new ChatRoom(newRoomId, name, category, ownerUsername);
        return chatRoomRepository.save(newChatRoom);
    }

    @Transactional(readOnly = true)
    public List<ChatRoomDto> findAllChatRooms() { // 반환 타입 변경
        List<ChatRoom> rooms = chatRoomRepository.findAll();
        return rooms.stream()
                .map(room -> ChatRoomDto.fromEntity(room, chatRoomUserService.countUsersInRoom(room.getRoomId())))
                .collect(Collectors.toList());
    }

    public Optional<ChatRoom> findChatRoomById(String roomId) {
        return chatRoomRepository.findByRoomId(roomId);
    }

    @Transactional(readOnly = true)
    public List<ChatRoomDto> findChatRoomsByCategoryId(String categoryId) { // 반환 타입 변경
        CommunityCategory category = communityCategoryRepository.findByCategoryId(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 카테고리 ID 입니다: " + categoryId));
        // Hibernate.initialize(category.getChatRooms()); // 이 방식 대신 직접 조회 후 DTO 매핑

        List<ChatRoom> roomsInCategory = chatRoomRepository.findByCategory(category); // 이 메소드가 ChatRoomRepository에 필요함

        return roomsInCategory.stream()
                .map(room -> ChatRoomDto.fromEntity(room, chatRoomUserService.countUsersInRoom(room.getRoomId())))
                .collect(Collectors.toList());
    }
}
