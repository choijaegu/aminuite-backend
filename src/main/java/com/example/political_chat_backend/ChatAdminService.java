package com.example.political_chat_backend;

import com.example.political_chat_backend.ChatRoomDto; // ChatRoomDto 임포트 가정
import com.example.political_chat_backend.ChatMessage;
import com.example.political_chat_backend.ChatRoom;
// import com.example.political_chat_backend.repository.ChatRoomRepository; // 직접 사용 안 함
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.Optional;

@Service
public class ChatAdminService {

    private static final Logger logger = LoggerFactory.getLogger(ChatAdminService.class);

    private final SimpMessagingTemplate messagingTemplate;
    private final ChatRoomUserService chatRoomUserService;
    private final ChatRoomService chatRoomService; // 방 정보를 가져오기 위해 주입

    @Autowired
    public ChatAdminService(SimpMessagingTemplate messagingTemplate,
                            ChatRoomUserService chatRoomUserService,
                            ChatRoomService chatRoomService) {
        this.messagingTemplate = messagingTemplate;
        this.chatRoomUserService = chatRoomUserService;
        this.chatRoomService = chatRoomService;
    }

    @Transactional
    public void kickUserFromRoom(String roomId, String kickerUsername, String usernameToKick) {
        logger.info("Processing kick request: kicker='{}', kicked='{}', room='{}'", kickerUsername, usernameToKick, roomId);

        if (kickerUsername.equals(usernameToKick)) {
            logger.warn("User '{}' attempted to kick self in room '{}'", kickerUsername, roomId);
            throw new IllegalArgumentException("자기 자신을 강퇴할 수 없습니다.");
        }

        if (!chatRoomUserService.getUsersInRoom(roomId).contains(usernameToKick)) {
            logger.warn("User '{}' not found in room '{}' for kicking.", usernameToKick, roomId);
            throw new IllegalArgumentException(usernameToKick + " 사용자는 현재 이 채팅방에 없습니다.");
        }

        // 강퇴 대상자에게 KICK 메시지 전송
        ChatMessage kickNotificationToUser = new ChatMessage();
        kickNotificationToUser.setType(ChatMessage.MessageType.KICK);
        kickNotificationToUser.setRoomId(roomId);
        kickNotificationToUser.setSender("SYSTEM");

        // 방 이름을 가져와서 메시지에 포함 (더 친절한 알림)
        Optional<ChatRoomDto> roomDtoOpt = chatRoomService.findChatRoomDtoById(roomId);
        String roomNameForNotification = roomDtoOpt.map(ChatRoomDto::getName).orElse(roomId); // 방 이름이 없으면 ID 사용

        kickNotificationToUser.setContent("채팅방 [" + roomNameForNotification + "] 에서 강퇴되었습니다. (관리자: " + kickerUsername + ")");

        // usernameToKick은 백엔드에서 인식하는 정확한 사용자 ID여야 합니다 (Principal.getName()과 일치).
        logger.info("Attempting to send KICK message to user: {} via /user/queue/private. Payload content: {}", usernameToKick, kickNotificationToUser.getContent());
        messagingTemplate.convertAndSendToUser(usernameToKick, "/queue/private", kickNotificationToUser);
        logger.info("KICK message directive sent to user: {}", usernameToKick);


        // 방에 있는 다른 사용자들에게 알림 메시지 전송
        ChatMessage systemMessageToRoom = new ChatMessage();
        systemMessageToRoom.setType(ChatMessage.MessageType.KICK); // 클라이언트에서 KICK 타입으로 다른 사용자의 강퇴도 처리 가능
        systemMessageToRoom.setRoomId(roomId);
        systemMessageToRoom.setSender("SYSTEM");
        systemMessageToRoom.setContent(usernameToKick + " 님이 " + kickerUsername + " 님에 의해 강퇴되었습니다.");
        // systemMessageToRoom.setKickedUser(usernameToKick); // ChatMessage 모델에 kickedUser 필드를 추가했다면 여기에 설정
        messagingTemplate.convertAndSend("/topic/room/" + roomId, systemMessageToRoom);
        logger.info("SYSTEM (KICK broadcast) message sent to room '{}' about user '{}'", roomId, usernameToKick);

        // ChatRoomUserService에서 사용자 제거
        chatRoomUserService.removeUserFromRoom(roomId, usernameToKick);
        logger.info("User '{}' removed from active list for room '{}'", usernameToKick, roomId);

        // 참여자 목록 업데이트 메시지 전송
        Set<String> currentUsersInRoom = chatRoomUserService.getUsersInRoom(roomId);
        ChatMessage userListUpdateMessage = new ChatMessage();
        userListUpdateMessage.setType(ChatMessage.MessageType.USER_LIST_UPDATE);
        userListUpdateMessage.setRoomId(roomId);
        userListUpdateMessage.setUsers(currentUsersInRoom);
        userListUpdateMessage.setUserCount(currentUsersInRoom.size());
        messagingTemplate.convertAndSend("/topic/room/" + roomId, userListUpdateMessage);
        logger.info("User list update sent to room '{}'. Current users: {}", roomId, currentUsersInRoom);

        logger.info("User '{}' kick process completed for room '{}'.", usernameToKick, roomId);
    }
}