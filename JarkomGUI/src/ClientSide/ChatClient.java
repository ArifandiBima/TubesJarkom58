package ClientSide;

import java.io.*;
import java.net.*;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicBoolean;

public class ChatClient {
    private static final String SERVER_IP = "localhost";
    private static final int PORT = 1234;
    private static final AtomicBoolean running = new AtomicBoolean(true);

    public static void main(String[] args) {
        try (
            Socket socket = new Socket(SERVER_IP, PORT);
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        ) {
            startReceiverThread(in);
            printHelp();
            processUserInput(out);
        } catch (IOException e) {
            System.err.println("Client error: " + e.getMessage());
        }
    }

    private static void startReceiverThread(BufferedReader in) {
        new Thread(() -> {
            try {
                String serverMessage;
                while (running.get() && (serverMessage = in.readLine()) != null) {
                    System.out.println(serverMessage);
                }
            } catch (IOException e) {
                if (running.get()) {
                    System.out.println("Disconnected from server.");
                }
            }
        }).start();
    }

    private static void processUserInput(PrintWriter out) {
        try (Scanner scanner = new Scanner(System.in)) {
            while (running.get()) {
                String input = scanner.nextLine();
                if (input.equalsIgnoreCase("/exit")) {
                    shutdown(out);
                    break;
                } else if (input.equalsIgnoreCase("/help")) {
                    printHelp();
                } else {
                    out.println(input);
                }
            }
        }
    }

    private static void shutdown(PrintWriter out) {
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
}