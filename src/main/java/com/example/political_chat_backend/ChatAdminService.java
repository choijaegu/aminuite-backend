package com.example.political_chat_backend; // 실제 서비스 패키지 경로로 수정

import com.example.political_chat_backend.ChatMessage;
import com.example.political_chat_backend.ChatRoom; // ChatRoom 모델 임포트
import com.example.political_chat_backend.ChatRoomRepository; // ChatRoomRepository 임포트 (필요시)
// import com.example.political_chat_backend.repository.KickLogRepository; // (선택) 강퇴 로그 저장 시
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; // 필요시 트랜잭션 관련

import java.util.Set;
// import java.time.LocalDateTime; // KickLog 등에 타임스탬프 사용 시

@Service
public class ChatAdminService {

    private static final Logger logger = LoggerFactory.getLogger(ChatAdminService.class);

    private final SimpMessagingTemplate messagingTemplate;
    private final ChatRoomUserService chatRoomUserService; // 사용자 목록 관리
    private final ChatRoomService chatRoomService;     // 방 정보 조회 (예: 방장 확인)

    // private final KickLogRepository kickLogRepository; // (선택 사항) 강퇴 로그 저장용 리포지토리

    @Autowired
    public ChatAdminService(SimpMessagingTemplate messagingTemplate,
                            ChatRoomUserService chatRoomUserService,
                            ChatRoomService chatRoomService
            /*, KickLogRepository kickLogRepository (선택 사항) */) {
        this.messagingTemplate = messagingTemplate;
        this.chatRoomUserService = chatRoomUserService;
        this.chatRoomService = chatRoomService;
        // this.kickLogRepository = kickLogRepository;
    }

    /**
     * 특정 사용자를 채팅방에서 강퇴 처리합니다.
     * @param roomId 강퇴가 일어나는 방 ID
     * @param kickerUsername 강퇴를 실행하는 관리자(방장)의 사용자 이름
     * @param usernameToKick 강퇴 대상 사용자의 이름
     */
    @Transactional // 여러 DB 작업 및 메시지 발송이 있으므로 트랜잭션으로 묶는 것이 좋을 수 있음
    public void kickUserFromRoom(String roomId, String kickerUsername, String usernameToKick) {
        logger.info("Processing kick request: kicker='{}', kicked='{}', room='{}'", kickerUsername, usernameToKick, roomId);

        // 0. 자기 자신을 강퇴할 수 없는 등의 기본 유효성 검사는 컨트롤러나 여기서 초기에 수행 가능
        if (kickerUsername.equals(usernameToKick)) {
            logger.warn("Attempt to kick self by user '{}' in room '{}'", kickerUsername, roomId);
            throw new IllegalArgumentException("자기 자신을 강퇴할 수 없습니다.");
        }

        // 1. 대상 사용자가 현재 해당 채팅방에 있는지 확인 (ChatRoomUserService 활용)
        if (!chatRoomUserService.getUsersInRoom(roomId).contains(usernameToKick)) {
            logger.warn("User '{}' not found in room '{}' for kicking.", usernameToKick, roomId);
            throw new IllegalArgumentException(usernameToKick + " 사용자는 현재 이 채팅방에 없습니다.");
        }

        // (선택) 2. 강퇴 로그 저장
        // KickLog log = new KickLog(roomId, usernameToKick, kickerUsername, LocalDateTime.now());
        // kickLogRepository.save(log);
        // logger.info("Kick event logged for user '{}' in room '{}' by '{}'", usernameToKick, roomId, kickerUsername);

        // 3. 강퇴 대상 사용자에게 개인 KICK 메시지 전송
        ChatMessage kickNotificationToUser = new ChatMessage();
        kickNotificationToUser.setType(ChatMessage.MessageType.KICK);
        kickNotificationToUser.setRoomId(roomId); // 클라이언트가 어떤 방에서 강퇴당했는지 알 수 있도록
        kickNotificationToUser.setSender("SYSTEM");
        kickNotificationToUser.setContent("채팅방 [" + (chatRoomService.findChatRoomDtoById(roomId).map(ChatRoomDto::getName).orElse(roomId)) + "] 에서 강퇴되었습니다. (관리자: " + kickerUsername + ")");

        logger.info("Attempting to send KICK message to user: {} via /user/queue/private. Payload: {}", usernameToKick, kickNotificationToUser.getContent());
        messagingTemplate.convertAndSendToUser(usernameToKick, "/queue/private", kickNotificationToUser);
        logger.info("KICK message directive sent to user: {}", usernameToKick);

        // 4. 방에 있는 다른 사용자들에게 알림 메시지 전송 (SYSTEM 메시지)
        //    이 메시지는 ChatMessage에 kickedUser 같은 필드를 추가하여 누가 강퇴당했는지 명시적으로 보내는 것이 더 좋을 수 있습니다.
        ChatMessage systemMessageToRoom = new ChatMessage();
        systemMessageToRoom.setType(ChatMessage.MessageType.KICK); // 또는 SYSTEM, 클라이언트에서 KICK 타입으로 처리하면 일관성 있음
        systemMessageToRoom.setRoomId(roomId);
        systemMessageToRoom.setSender("SYSTEM"); // 시스템이 보낸 메시지
        systemMessageToRoom.setContent(usernameToKick + " 님이 " + kickerUsername + " 님에 의해 강퇴되었습니다.");
        // systemMessageToRoom.setKickedUser(usernameToKick); // ChatMessage 모델에 kickedUser 필드가 있다면 설정

        messagingTemplate.convertAndSend("/topic/room/" + roomId, systemMessageToRoom);
        logger.info("SYSTEM (KICK broadcast) message sent to room '{}' about user '{}'", roomId, usernameToKick);

        // 5. ChatRoomUserService에서 사용자 제거
        //    이 작업은 USER_LIST_UPDATE를 트리거할 수 있지만, 명시적으로 한번 더 보냅니다.
        boolean removed = chatRoomUserService.removeUserFromRoom(roomId, usernameToKick);
        if(removed){
            logger.info("User '{}' removed from active list for room '{}'", usernameToKick, roomId);
        } else {
            logger.warn("User '{}' was not found in active list for room '{}' upon removal attempt or already removed.", usernameToKick, roomId);
        }


        // 6. 참여자 목록 업데이트 메시지 전송
        Set<String> currentUsersInRoom = chatRoomUserService.getUsersInRoom(roomId);
        ChatMessage userListUpdateMessage = new ChatMessage();
        userListUpdateMessage.setType(ChatMessage.MessageType.USER_LIST_UPDATE);
        userListUpdateMessage.setRoomId(roomId);
        userListUpdateMessage.setUsers(currentUsersInRoom);
        userListUpdateMessage.setUserCount(currentUsersInRoom.size());
        messagingTemplate.convertAndSend("/topic/room/" + roomId, userListUpdateMessage);
        logger.info("User list update sent to room '{}'. Current users: {}", roomId, currentUsersInRoom);

        logger.info("User '{}' kick process completed for room '{}'.", usernameToKick, roomId);

        // 중요: 실제 서버에서 사용자의 WebSocket 연결을 강제로 끊는 것은
        // SimpUserRegistry를 사용하거나 WebSocket 세션을 직접 관리해야 하는 더 복잡한 로직이 필요할 수 있습니다.
        // 현재는 클라이언트가 KICK 메시지를 받고 스스로 연결을 끊도록 유도하는 방식입니다.
    }
}