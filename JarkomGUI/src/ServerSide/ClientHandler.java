package ServerSide;

import java.io.*;
import java.net.*;
import java.util.concurrent.atomic.*;

public class ClientHandler implements Runnable {
    private final Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private String username;
    private ChatRoom currentRoom;
    private final AtomicBoolean running = new AtomicBoolean(true);

    public ClientHandler(Socket socket) {
        this.socket = socket;
        try{
            initializeStreams();
            authenticateUser();
        }
        catch(IOException e){
            
        }
    }

    public boolean isClosed() {
        return socket.isClosed() || !running.get();
    }

    public void run() {
        try {

            processMessages();
        } catch (IOException e) {
            handleError(e);
        } finally {
            shutdown();
        }
    }

    private void initializeStreams() throws IOException {
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new PrintWriter(socket.getOutputStream(), true);
    }

    private void authenticateUser() throws IOException {
        boolean taken = false;
        do{
            out.println("Enter your username:");
            username = in.readLine();
            taken = ChatServer.isTaken(this);
        } while (taken);
        
        out.println(username + " connected");
    }

    private void processMessages() throws IOException {
        String inputLine;
        while (running.get() && (inputLine = in.readLine()) != null) {
            if (inputLine.equalsIgnoreCase("/exit")) {
                break;
            }
            handleCommand(inputLine);
        }
    }

    private void handleCommand(String input) {
        if (input.startsWith("/join ")) {
            joinRoom(input.substring(6).trim());
        } else if (input.equals("/leave")) {
            if(currentRoom.isOwner(this)) currentRoom.closeRoom();
            else leaveRoom();
        } else if (input.equals("/rooms")) {
            listRooms();
        } else if (input.equals("/close")) {
            if (currentRoom != null && currentRoom.isOwner(this)) {
                currentRoom.closeRoom();
                currentRoom = null;
            } else {
                sendMessage("You are not the room owner.");
            }
        } else if (input.startsWith("/kick ")) {
            if (currentRoom != null && currentRoom.isOwner(this)) {
                String target = input.substring(6).trim();
                boolean kicked = currentRoom.kickUser(target);
                if (kicked) {
                    sendMessage("User '" + target + "' has been kicked.");
                } else {
                    sendMessage("User not found in the room.");
                }
            } else {
                sendMessage("Only room owners can kick users.");
            }
        } else if (input.equals("/help")) {
            sendMessage("""
                Commands:
        /join [room] - Join or create a room
        /leave       - Leave current room
        /rooms       - List all rooms
        /kick [user] - (Owner only) Kick user from room
        /close       - (Owner only) Close and delete room
        /exit        - Exit the chat
        /help        - Show this help message
        """);
        } else if (currentRoom != null) {
            currentRoom.broadcast(username + ": " + input);
        } else {
            sendMessage("You must join a room first (/join roomname)");
        }
    }


    private void joinRoom(String roomName) {
        if(roomName.isEmpty()){
            sendMessage("Room name cannot be empty");
            return;
        }

        if (currentRoom != null) {
            currentRoom.memberLeft(this);
            sendMessage("Left room: " + currentRoom.getName());
        }
        currentRoom = RoomManager.getOrCreateRoom(roomName, this);
        sendMessage("Joined room: " + roomName);
        currentRoom.memberJoined(this);

    }

    private void leaveRoom() {
        if (currentRoom != null) {
            currentRoom.memberLeft(this);
            sendMessage("Left room: " + currentRoom.getName());
            currentRoom = null;
        }
        else{
            sendMessage("Not in any room");
        }
    }

    public void kickFromRoom() {
        currentRoom = null;
        sendMessage("kickOut");
    }

    private void listRooms() {
        StringBuilder sb = new StringBuilder("Available rooms:\n");
        RoomManager.getRoomInfo().forEach((name, count) -> 
            sb.append("- ").append(name).append(" (")
            .append(count).append(" user").append(count != 1 ? "s" : "")
            .append(")\n"));
        sb.append("done\n");
        sendMessage(sb.toString());
    }

    public void sendMessage(String message) {
        if (!isClosed()) {
            out.println(message);
        }
    }

    private void handleError(IOException e) {
        if (running.get()) {
            System.out.println("Client error: " + (username != null ? username : "unknown") + 
                             " - " + e.getMessage());
        }
    }

    private void shutdown() {
        if (running.compareAndSet(true, false)) {
            try {
                if (currentRoom != null) {
                    currentRoom.memberLeft(this);
                }
                ChatServer.removeClient(this);
                if (!socket.isClosed()) {
                    socket.close();
                }
                ChatServer.broadcastSystemMessage(username + " disconnected");
            } catch (IOException e) {
                System.out.println("Shutdown error: " + e.getMessage());
            }
        }
    }

    public String getUsername() {
        return username;
    }
    public boolean equals(Object other){
        if (!(other instanceof ClientHandler)) return false;
        return this.username.equals(((ClientHandler)other).username);
    }
    public int hashCode(){
        return this.username.hashCode();
    }
    public boolean inRoom(){
        return this.currentRoom!=null;
    }
}