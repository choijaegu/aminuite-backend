package com.example.political_chat_backend;

import com.example.political_chat_backend.ChatMessage;         // 방금 수정한 ChatMessage
import com.example.political_chat_backend.ChatRoomUserService;      // 제공해주신 ChatRoomUserService
// import com.example.political_chat_backend.repository.KickLogRepository; // (선택) 강퇴 로그 저장 시

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import java.util.Set;

@Service
public class ChatAdminService {

    private final SimpMessagingTemplate messagingTemplate;
    private final ChatRoomUserService chatRoomUserService;
    // private final KickLogRepository kickLogRepository; // (선택 사항)

    @Autowired
    public ChatAdminService(SimpMessagingTemplate messagingTemplate,
                            ChatRoomUserService chatRoomUserService) { // KickLogRepository는 선택적이므로 주석 처리
        this.messagingTemplate = messagingTemplate;
        this.chatRoomUserService = chatRoomUserService;
    }

    public void kickUserFromRoom(String roomId, String kickerUsername, String usernameToKick) {
        if (kickerUsername.equals(usernameToKick)) {
            throw new IllegalArgumentException("자기 자신을 강퇴할 수 없습니다.");
        }

        // 사용자가 실제로 방에 있는지 확인 (ChatRoomUserService 사용)
        if (!chatRoomUserService.getUsersInRoom(roomId).contains(usernameToKick)) {
            throw new IllegalArgumentException(usernameToKick + " 사용자는 현재 이 채팅방에 없습니다.");
        }

        // 1. 강퇴 대상 사용자에게 알림 (KICK 메시지)
        ChatMessage kickNotificationToUser = new ChatMessage();
        kickNotificationToUser.setType(ChatMessage.MessageType.KICK);
        kickNotificationToUser.setRoomId(roomId);
        kickNotificationToUser.setSender("SYSTEM"); // 시스템 메시지 발신자
        kickNotificationToUser.setContent("당신은 채팅방 '" + roomId + "' 에서 강퇴되었습니다. (관리자: " + kickerUsername + ")");
        // STOMP의 user destination을 사용하여 특정 사용자에게 메시지 전송
        // 프론트엔드에서는 /user/queue/private 과 같은 경로로 구독 설정 필요
        messagingTemplate.convertAndSendToUser(usernameToKick, "/queue/private", kickNotificationToUser);
        System.out.println("Sent KICK notification to user: " + usernameToKick);


        // 2. 방에 있는 다른 사용자들에게 알림 (SYSTEM 메시지)
        ChatMessage systemMessageToRoom = new ChatMessage();
        systemMessageToRoom.setType(ChatMessage.MessageType.SYSTEM); // 또는 LEAVE 타입과 유사하게 처리 가능
        systemMessageToRoom.setRoomId(roomId);
        systemMessageToRoom.setSender("SYSTEM");
        systemMessageToRoom.setContent(usernameToKick + " 님이 " + kickerUsername + " 님에 의해 강퇴되었습니다.");
        messagingTemplate.convertAndSend("/topic/room/" + roomId, systemMessageToRoom);
        System.out.println("Sent SYSTEM message to room " + roomId + " about " + usernameToKick + " being kicked.");


        // 3. 강퇴된 사용자를 채팅방의 활성 사용자 목록에서 제거 (ChatRoomUserService 사용)
        boolean removed = chatRoomUserService.removeUserFromRoom(roomId, usernameToKick);
        if (removed) {
            System.out.println(usernameToKick + " removed from active user list for room " + roomId);
        } else {
            System.out.println("Could not remove " + usernameToKick + " from room " + roomId + " (already removed or not found).");
        }
        // 참고: 여기서 서버 측 WebSocket 세션 강제 종료 로직이 추가되면 더 강력해집니다 (추후 개선 사항).

        // 4. 참여자 목록 업데이트 메시지 전송 (USER_LIST_UPDATE 메시지)
        Set<String> currentUsersInRoom = chatRoomUserService.getUsersInRoom(roomId); // 최신 사용자 목록
        ChatMessage userListUpdateMessage = new ChatMessage();
        userListUpdateMessage.setType(ChatMessage.MessageType.USER_LIST_UPDATE);
        userListUpdateMessage.setRoomId(roomId);
        userListUpdateMessage.setUsers(currentUsersInRoom);
        userListUpdateMessage.setUserCount(currentUsersInRoom.size());
        messagingTemplate.convertAndSend("/topic/room/" + roomId, userListUpdateMessage);
        System.out.println("Sent USER_LIST_UPDATE to room " + roomId);

        // (선택) 강퇴 로그 저장
        // KickLog log = new KickLog(roomId, usernameToKick, kickerUsername, LocalDateTime.now());
        // kickLogRepository.save(log);
    }
}
