package com.example.political_chat_backend;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.hibernate.Hibernate; // Hibernate 프록시 초기화를 위해 추가

import java.util.List;
import java.util.Optional;

@Service
public class ChatRoomService {

    private final ChatRoomRepository chatRoomRepository;
    private final CommunityCategoryRepository communityCategoryRepository; // 추가

    @Autowired
    public ChatRoomService(ChatRoomRepository chatRoomRepository,
                           CommunityCategoryRepository communityCategoryRepository) { // 주입 추가
        this.chatRoomRepository = chatRoomRepository;
        this.communityCategoryRepository = communityCategoryRepository; // 할당 추가
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
    public ChatRoom createChatRoom(String categoryId, String roomId, String name) {
        CommunityCategory category = communityCategoryRepository.findByCategoryId(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 카테고리 ID 입니다: " + categoryId));

        if (chatRoomRepository.findByRoomId(roomId).isPresent()) {
            throw new IllegalArgumentException("이미 존재하는 방 ID 입니다: " + roomId);
        }
        ChatRoom newChatRoom = new ChatRoom(roomId, name, category);
        return chatRoomRepository.save(newChatRoom);
    }

    @Transactional(readOnly = true)
    public List<ChatRoom> findAllChatRooms() {
        return chatRoomRepository.findAll();
    }

    public Optional<ChatRoom> findChatRoomById(String roomId) {
        return chatRoomRepository.findByRoomId(roomId);
    }

    @Transactional(readOnly = true)
    public List<ChatRoom> findChatRoomsByCategoryId(String categoryId) {
        CommunityCategory category = communityCategoryRepository.findByCategoryId(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 카테고리 ID 입니다: " + categoryId));
        Hibernate.initialize(category.getChatRooms()); // 여기도 명시적 초기화
        return category.getChatRooms();
    }
}
