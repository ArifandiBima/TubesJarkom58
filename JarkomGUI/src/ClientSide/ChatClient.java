package ClientSide;

import java.io.*;
import java.net.*;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicBoolean;

public class ChatClient {
    private static final String SERVER_IP = "localhost";//tolong diganti dengan ip server
    private static final int PORT = 1234;
    private static final AtomicBoolean running = new AtomicBoolean(true);
    private static PrintWriter out;
    private static BufferedReader in;
    private static Room room;
    private static Thread receiverThread;

    public static void main(String[] args) {
        try (
            Socket socket = new Socket(SERVER_IP, PORT);
            
        ) {
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            
            System.out.println(in.readLine());
            UsernameSubmit.getUsername();
            while(running.get()){
            
            }
        } catch (IOException e) {
            System.err.println("Client errorm: " + e.getMessage());
        }
        
    }

    private static void startReceiverThread() {
        receiverThread = new Thread(() -> {
            try {
                String serverMessage;
                while (running.get() && (serverMessage = in.readLine()) != null) {
                    if (serverMessage.startsWith("kickOut")){
                        room.exit();
                        room= null;
                        break;
                    }
                    if (serverMessage.startsWith("Members:")){
                        String members ="" ;
                        do{
                            members+=serverMessage+"\n";
                            serverMessage = in.readLine();
                        }while(!serverMessage.equals("done"));
                        room.listMembers(members);
                    }
                    else{
                        room.addMessage(serverMessage);
                    }
                }
            } catch (IOException e) {
                if (running.get()) {
                    System.out.println(e.getMessage());
                    System.out.println("Disconnected from server.");
                }
            }
        });
        receiverThread.start();
    }

    public static void processUserInput(String input) {
        try{
            if (input.equalsIgnoreCase("/exit")) {
                shutdown();
            } else if (input.equalsIgnoreCase("/help")) {
                printHelp();
            } else {
                out.println(input);
            }
        }catch(Exception e){
        }
    }

    private static void shutdown() {
        running.set(false);
        out.println("/exit");
    }

    private static void printHelp() {
        System.out.println("""
            Available Commands:
            /join [room]  - Join or create a room
            /leave        - Leave current room
            /rooms        - List all rooms and user count
            /kick [user]  - (Owner only) Kick a user from the room
            /close        - (Owner only) Close and delete the current room
            /help         - Show this help message
            /exit         - Quit the application
            """);
    }
    public static boolean usernameVerified(String username){
        if (username.trim().equals("")) return false;
        String serverMessage="";
        try {
            out.println(username);
            serverMessage = in.readLine();
        } catch (IOException e) {
            System.out.println("ana");
        }
        if (serverMessage.equals("Enter your username:")){
            System.out.println("retry");return false;
        }
        else{
            System.out.println("Success");
        }
        return true;
    }
    public static String listRooms(){
        processUserInput("/rooms");
        String roomList="";
        try {
            String serverMessage=in.readLine();
            do{
                System.out.println(serverMessage);
                roomList+=serverMessage+"\n";
                serverMessage = in.readLine();
            }while(!serverMessage.equals("done"));
        } catch (IOException e) {
            System.out.println("ana");
        }
        System.out.println("anoa");
        return roomList;
    }
    public static void enterRoom(String roomName){
        room = new Room(roomName);
        Room.enterRoom(room);
        startReceiverThread();
        processUserInput("/join "+roomName);
        processUserInput("/members");
    }
}