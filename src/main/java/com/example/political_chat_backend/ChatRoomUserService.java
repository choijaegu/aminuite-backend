package com.example.political_chat_backend;

import org.springframework.stereotype.Service;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ChatRoomUserService {

    // Key: roomId, Value: 해당 방에 접속한 사용자 닉네임 Set
    private final Map<String, Set<String>> roomUsers = new ConcurrentHashMap<>();

    /**
     * 특정 방에 사용자를 추가합니다.
     * @param roomId 방 ID
     * @param username 사용자 닉네임
     * @return 사용자가 성공적으로 추가되었으면 true, 이미 존재하면 false
     */
    public boolean addUserToRoom(String roomId, String username) {
        return roomUsers.computeIfAbsent(roomId, k -> Collections.synchronizedSet(new HashSet<>()))
                .add(username);
    }

    /**
     * 특정 방에서 사용자를 제거합니다.
     * @param roomId 방 ID
     * @param username 사용자 닉네임
     * @return 사용자가 성공적으로 제거되었으면 true, 해당 사용자가 없었으면 false
     */
    public boolean removeUserFromRoom(String roomId, String username) {
        Set<String> usersInRoom = roomUsers.get(roomId);
        if (usersInRoom != null) {
            return usersInRoom.remove(username);
        }
        return false;
    }

    /**
     * 특정 방의 모든 사용자 닉네임 목록을 가져옵니다.
     * @param roomId 방 ID
     * @return 해당 방의 사용자 닉네임 Set (없으면 빈 Set)
     */
    public Set<String> getUsersInRoom(String roomId) {
        return roomUsers.getOrDefault(roomId, Collections.emptySet());
    }

    /**
     * 특정 방의 현재 사용자 수를 가져옵니다.
     * @param roomId 방 ID
     * @return 해당 방의 사용자 수
     */
    public int countUsersInRoom(String roomId) {
        return getUsersInRoom(roomId).size();
    }

    /**
     * 사용자가 나갈 때, 모든 방에서 해당 사용자를 찾아 제거합니다.
     * (한 사용자는 여러 방에 동시에 접속할 수 없다는 가정 하에, 또는 세션 ID 기반으로 관리 시 더 유용)
     * 지금은 특정 방에서만 제거하는 로직을 주로 사용하게 됩니다.
     * @param username 사용자 닉네임
     */
    public void removeUserFromAllRooms(String username) {
        roomUsers.forEach((roomId, users) -> users.remove(username));
    }
}
