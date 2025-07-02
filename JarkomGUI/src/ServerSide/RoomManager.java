package ServerSide;

import java.util.*;
import java.util.concurrent.*;

public class RoomManager {
    private static final ConcurrentMap<String, ChatRoom> rooms = new ConcurrentHashMap<>();

    public static ChatRoom getOrCreateRoom(String name, ClientHandler owner) {
        return rooms.computeIfAbsent(name, k -> new ChatRoom(name, owner));
    }

    public static Map<String, Integer> getRoomInfo() {
        Map<String, Integer> info = new HashMap<>();
        rooms.forEach((name, room) -> info.put(name, room.getMemberCount()));
        return info;
    }

    public static void removeRoomIfEmpty(String name) {
        rooms.computeIfPresent(name, (k, v) -> v.getMemberCount() > 0 ? v : null);
    }
    
}