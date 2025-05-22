package com.example.political_chat_backend;

import com.example.political_chat_backend.ChatRoomDto; // ChatRoomDto 임포트 필요
import com.example.political_chat_backend.ChatRoom;
import com.example.political_chat_backend.CommunityCategory;
import com.example.political_chat_backend.ChatRoomRepository;
import com.example.political_chat_backend.CommunityCategoryRepository;
// import com.example.political_chat_backend.service.ChatRoomUserService; // ChatRoomUserService는 이미 주입되어 있음

import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;       // Page 임포트
import org.springframework.data.domain.Pageable;  // Pageable 임포트
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
// stream().map().collect() 사용 시 필요할 수 있으나, Page.map() 사용 시 직접 필요 없을 수 있음
// import java.util.stream.Collectors;

@Service
public class ChatRoomService {

    private final ChatRoomRepository chatRoomRepository;
    private final CommunityCategoryRepository communityCategoryRepository;
    private final ChatRoomUserService chatRoomUserService;

    @Autowired
    public ChatRoomService(ChatRoomRepository chatRoomRepository,
                           CommunityCategoryRepository communityCategoryRepository,
                           ChatRoomUserService chatRoomUserService) {
        this.chatRoomRepository = chatRoomRepository;
        this.communityCategoryRepository = communityCategoryRepository;
        this.chatRoomUserService = chatRoomUserService;
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
        // LAZY 로딩인 chatRooms를 사용하기 전에 초기화 (DTO 변환 시점에 따라 필요 없을 수도 있음)
        // 만약 Category DTO를 사용하고 거기서 chatRooms를 제외한다면 이 초기화는 불필요
        for (CommunityCategory category : categories) {
            Hibernate.initialize(category.getChatRooms());
        }
        return categories;
    }

    @Transactional(readOnly = true)
    public Optional<CommunityCategory> findCategoryById(String categoryId) {
        Optional<CommunityCategory> categoryOpt = communityCategoryRepository.findByCategoryId(categoryId);
        if (categoryOpt.isPresent()) {
            Hibernate.initialize(categoryOpt.get().getChatRooms());
        }
        return categoryOpt;
    }

    /**
     * 새로운 채팅방을 특정 카테고리 하위에 생성합니다.
     * ownerUsername은 컨트롤러에서 인증된 사용자로부터 가져와서 전달하는 것을 권장합니다.
     */
    @Transactional
    public ChatRoom createChatRoom(String categoryId, String name, String ownerUsername) {
        CommunityCategory category = communityCategoryRepository.findByCategoryId(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 카테고리 ID 입니다: " + categoryId));

        String newRoomId = UUID.randomUUID().toString().substring(0, 8);
        int attempt = 0;
        while (chatRoomRepository.findByRoomId(newRoomId).isPresent() && attempt < 5) {
            newRoomId = UUID.randomUUID().toString().substring(0, 8) + "-" + attempt;
            attempt++;
        }
        if (chatRoomRepository.findByRoomId(newRoomId).isPresent()){
            throw new IllegalStateException("채팅방 ID 생성에 반복적으로 실패했습니다. 잠시 후 다시 시도해주세요.");
        }

        ChatRoom newChatRoom = new ChatRoom(newRoomId, name, category, ownerUsername);
        return chatRoomRepository.save(newChatRoom);
    }

    /**
     * 모든 채팅방 목록을 페이징하여 조회합니다.
     * @param pageable 페이징 정보 (페이지 번호, 페이지 크기, 정렬)
     * @return 페이징 처리된 ChatRoomDto 목록
     */
    @Transactional(readOnly = true)
    public Page<ChatRoomDto> findAllChatRooms(Pageable pageable) { // Pageable 파라미터 추가, Page<ChatRoomDto> 반환
        Page<ChatRoom> roomsPage = chatRoomRepository.findAll(pageable); // Repository는 Page<ChatRoom> 반환
        // Page 객체의 map 함수를 사용하여 Page<ChatRoom>을 Page<ChatRoomDto>로 변환
        return roomsPage.map(room -> ChatRoomDto.fromEntity(room, chatRoomUserService.countUsersInRoom(room.getRoomId())));
    }

    /**
     * 특정 채팅방 정보를 ID로 조회합니다. (엔티티 반환)
     * @param roomId 조회할 채팅방 ID
     * @return Optional<ChatRoom>
     */
    public Optional<ChatRoom> findChatRoomById(String roomId) {
        return chatRoomRepository.findByRoomId(roomId);
    }

    /**
     * 특정 채팅방 정보를 ID로 조회하여 DTO로 반환합니다. (DTO 반환)
     * @param roomId 조회할 채팅방 ID
     * @return Optional<ChatRoomDto>
     */
    @Transactional(readOnly = true)
    public Optional<ChatRoomDto> findChatRoomDtoById(String roomId) {
        return chatRoomRepository.findByRoomId(roomId)
                .map(room -> ChatRoomDto.fromEntity(room, chatRoomUserService.countUsersInRoom(roomId)));
    }


    /**
     * 특정 카테고리에 속한 채팅방 목록을 페이징하여 조회합니다.
     * @param categoryId 조회할 카테고리 ID
     * @param pageable 페이징 정보
     * @return 페이징 처리된 ChatRoomDto 목록
     */
    @Transactional(readOnly = true)
    public Page<ChatRoomDto> findChatRoomsByCategoryId(String categoryId, Pageable pageable) { // Pageable 파라미터 추가
        CommunityCategory category = communityCategoryRepository.findByCategoryId(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 카테고리 ID 입니다: " + categoryId));

        Page<ChatRoom> roomsInCategoryPage = chatRoomRepository.findByCategory(category, pageable); // Repository는 Page<ChatRoom> 반환

        return roomsInCategoryPage.map(room -> ChatRoomDto.fromEntity(room, chatRoomUserService.countUsersInRoom(room.getRoomId())));
    }

    /**
     * 사용자가 특정 채팅방의 소유자인지 확인합니다.
     * @param roomId 채팅방 ID
     * @param username 확인할 사용자 이름
     * @return 소유자이면 true, 아니면 false
     */
    @Transactional(readOnly = true)
    public boolean isRoomOwner(String roomId, String username) {
        return chatRoomRepository.findByRoomId(roomId)
                .map(room -> room.getOwnerUsername().equals(username))
                .orElse(false);
    }

    @Transactional // 데이터 변경이 있으므로 트랜잭션 처리
    public void deleteRoom(String roomId) {
        // 방을 찾아서 존재하는지 확인 (선택적이지만 안전)
        ChatRoom roomToDelete = chatRoomRepository.findByRoomId(roomId)
                .orElseThrow(() -> new IllegalArgumentException("삭제할 채팅방을 찾을 수 없습니다: " + roomId));

        // TODO: 채팅방 삭제 전에 해당 방의 메시지, 참여자 정보 등 관련 데이터도 함께 삭제하는 로직 필요
        // 예를 들어, ChatMessageRepository.deleteByRoomId(roomId); 등

        chatRoomRepository.delete(roomToDelete); // 또는 deleteById(roomId)
        // System.out.println("Chat room deleted: " + roomId); // 간단한 로그
    }
}
