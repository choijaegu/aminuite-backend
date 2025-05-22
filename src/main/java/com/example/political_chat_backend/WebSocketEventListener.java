package com.example.political_chat_backend;

import com.example.political_chat_backend.ChatRoom;
import com.example.political_chat_backend.ChatMessage; // ChatMessage 임포트
import com.example.political_chat_backend.ChatRoomRepository; // ChatRoomRepository 직접 사용 예시
import com.example.political_chat_backend.ChatRoomService; // 또는 ChatRoomService 사용
import com.example.political_chat_backend.ChatRoomUserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate; // 추가
import org.springframework.messaging.simp.stomp.StompHeaderAccessor; // StompHeaderAccessor 로 변경
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.Map; // Map 임포트
import java.util.Optional; // Optional 임포트

@Component
public class WebSocketEventListener {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketEventListener.class);

    @Autowired
    private ChatRoomUserService chatRoomUserService;

    @Autowired
    private ChatRoomService chatRoomService; // 방 정보를 가져오고, 방 삭제를 위해 주입

    @Autowired
    private SimpMessagingTemplate messagingTemplate; // LEAVE 메시지 및 USER_LIST_UPDATE 전송용

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        Map<String, Object> sessionAttributes = headerAccessor.getSessionAttributes();

        if (sessionAttributes != null) {
            String username = (String) sessionAttributes.get("username");
            String roomId = (String) sessionAttributes.get("roomId");

            if (username != null && roomId != null) {
                logger.info("User Disconnected: {} from room: {} (Session: {})", username, roomId, headerAccessor.getSessionId());

                // 1. ChatRoomUserService에서 사용자 제거
                boolean removed = chatRoomUserService.removeUserFromRoom(roomId, username);

                if (removed) {
                    // 2. LEAVE 메시지 브로드캐스팅
                    ChatMessage chatMessage = new ChatMessage();
                    chatMessage.setType(ChatMessage.MessageType.LEAVE);
                    chatMessage.setSender(username);
                    chatMessage.setContent(username + " 님이 퇴장했습니다.");
                    chatMessage.setRoomId(roomId);
                    messagingTemplate.convertAndSend("/topic/room/" + roomId, chatMessage);

                    // 3. 사용자 목록 업데이트 메시지 브로드캐스팅
                    ChatMessage userListUpdate = new ChatMessage();
                    userListUpdate.setType(ChatMessage.MessageType.USER_LIST_UPDATE);
                    userListUpdate.setUsers(chatRoomUserService.getUsersInRoom(roomId));
                    userListUpdate.setUserCount(chatRoomUserService.countUsersInRoom(roomId));
                    userListUpdate.setRoomId(roomId);
                    messagingTemplate.convertAndSend("/topic/room/" + roomId, userListUpdate);
                }

                // 4. 나간 사용자가 방장인지, 그리고 방이 비었는지 확인 후 삭제 로직
                Optional<ChatRoom> chatRoomOptional = chatRoomService.findChatRoomById(roomId); // ChatRoomService 사용
                if (chatRoomOptional.isPresent()) {
                    ChatRoom chatRoom = chatRoomOptional.get();
                    if (username.equals(chatRoom.getOwnerUsername())) { // 나간 사용자가 방장인가?
                        logger.info("Owner {} left room {}", username, roomId);
                        if (chatRoomUserService.countUsersInRoom(roomId) == 0) { // 방에 아무도 없는가?
                            logger.info("Room {} is empty after owner left. Deleting room.", roomId);
                            // chatRoomRepository.deleteById(roomId); // 직접 리포지토리 사용 또는
                            chatRoomService.deleteRoom(roomId); // 서비스 계층에 위임 (deleteRoom 메소드 필요)
                            logger.info("Room {} deleted.", roomId);
                        } else {
                            logger.info("Room {} is not empty. Owner left but other users remain.", roomId);
                        }
                    }
                } else {
                    logger.warn("Chat room with ID {} not found during disconnect handling.", roomId);
                }

            } else {
                logger.warn("Username or RoomID not found in session attributes during disconnect. SessionId: {}", headerAccessor.getSessionId());
            }
        } else {
            logger.warn("SessionAttributes is null during disconnect. SessionId: {}", headerAccessor.getSessionId());
        }
    }
}
