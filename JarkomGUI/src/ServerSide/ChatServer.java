package ServerSide;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class ChatServer {
    private static final int PORT = 1234;
    private static final Set<ClientHandler> clients = ConcurrentHashMap.newKeySet();
    private static final ExecutorService threadPool = Executors.newCachedThreadPool();

    public static void main(String[] args) {
        System.out.println("Chat Server starting...");
        
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                ClientHandler clientThread = new ClientHandler(clientSocket);
                clients.add(clientThread);
                threadPool.execute(clientThread);
            }
        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
        } finally {
            threadPool.shutdown();
        }
    }

    public static void broadcastSystemMessage(String message) {
        clients.forEach(client -> {
            if (!client.isClosed()) {
                client.sendMessage("[System] " + message);
            }
        });
    }

    public static synchronized void removeClient(ClientHandler client) {
        if (clients.remove(client)) {
            System.out.println(client.getUsername() + " disconnected. Active clients: " + clients.size());
        }
    }
}