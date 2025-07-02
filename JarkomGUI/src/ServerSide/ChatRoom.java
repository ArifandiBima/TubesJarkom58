package ServerSide;

import java.util.*;
import java.util.concurrent.*;

public class ChatRoom {
    private final String name;
    private final ClientHandler owner;
    private final Set<ClientHandler> members = ConcurrentHashMap.newKeySet();

    public ChatRoom(String name, ClientHandler owner) {
        this.name = name;
        this.owner = owner;
    }

    public synchronized void memberJoined(ClientHandler client) {
        members.add(client);
        broadcast(client.getUsername() + " joined " + name);
    }

    public synchronized void memberLeft(ClientHandler client) {
        if (members.remove(client)) {
            broadcast(client.getUsername() + " left " + name);
            RoomManager.removeRoomIfEmpty(name);
        }
        this.someOneJoined();
    }

    public boolean isOwner(ClientHandler user) {
    return owner.equals(user);
}

    public synchronized void closeRoom() {
        for (ClientHandler member : members) {
            member.sendMessage("[Room Closed] Room '" + name + "' has been closed by the owner.");
            member.kickFromRoom();
        }
        members.clear();
        RoomManager.removeRoomIfEmpty(name);
    }

    public synchronized boolean kickUser(String username) {
        for (ClientHandler member : members) {
            if (member.getUsername().equalsIgnoreCase(username)) {
                member.kickFromRoom();
                memberLeft(member);
                return true;
            }
        }
        return false;
    }


    public synchronized void broadcast(String message) {
        members.removeIf(ClientHandler::isClosed);
        members.forEach(member -> member.sendMessage("[" + name + "] " + message));
    }

    public String getName() {
        return name;
    }

    public int getMemberCount() {
        return members.size();
    }
    public String getMembers(){
        String res="";
        for (ClientHandler member:members){
            res+=member.getUsername()+"\n";
        }
        return res;
    }
    public void someOneJoined(){
        for(ClientHandler member:members){
            member.listMembers();
        }
    }
}