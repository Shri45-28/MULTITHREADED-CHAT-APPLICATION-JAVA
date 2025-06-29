import java.io.*;
import java.net.*;
import java.util.*;

public class ChatApp {
    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Usage: java ChatApp <server|client>");
            return;
        }

        if (args[0].equalsIgnoreCase("server")) {
            new ChatServer().startServer();
        } else if (args[0].equalsIgnoreCase("client")) {
            new ChatClient().startClient();
        } else {
            System.out.println("Invalid argument. Use 'server' or 'client'.");
        }
    }
}


class ChatServer {
    private static final int PORT = 1234;
    private static Set<ClientHandler> clients = Collections.synchronizedSet(new HashSet<>());

    public void startServer() {
        System.out.println("Chat server started on port " + PORT);
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                ClientHandler handler = new ClientHandler(clientSocket);
                clients.add(handler);
                new Thread(handler).start();
            }
        } catch (IOException e) {
            System.out.println("Server error: " + e.getMessage());
        }
    }

    static void broadcast(String message, ClientHandler sender) {
        for (ClientHandler client : clients) {
            if (client != sender) {
                client.sendMessage(message);
            }
        }
    }

    static void removeClient(ClientHandler client) {
        clients.remove(client);
    }

    static class ClientHandler implements Runnable {
        private Socket socket;
        private PrintWriter out;
        private BufferedReader in;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        public void run() {
            try {
                out = new PrintWriter(socket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out.println("Welcome to the Chat!");

                String message;
                while ((message = in.readLine()) != null) {
                    System.out.println("Client: " + message);
                    ChatServer.broadcast(message, this);
                }
            } catch (IOException e) {
                System.out.println("A client disconnected.");
            } finally {
                try {
                    socket.close();
                } catch (IOException ignored) {}
                ChatServer.removeClient(this);
            }
        }

        void sendMessage(String message) {
            out.println(message);
        }
    }
}


class ChatClient {
    private static final String SERVER_IP = "127.0.0.1";
    private static final int SERVER_PORT = 1234;

    public void startClient() {
        try (Socket socket = new Socket(SERVER_IP, SERVER_PORT);
             BufferedReader userInput = new BufferedReader(new InputStreamReader(System.in));
             BufferedReader serverInput = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter serverOut = new PrintWriter(socket.getOutputStream(), true)) {

            System.out.println("Connected to the chat server.");

            // Reader thread
            new Thread(() -> {
                try {
                    String msg;
                    while ((msg = serverInput.readLine()) != null) {
                        System.out.println(msg);
                    }
                } catch (IOException e) {
                    System.out.println("Connection closed.");
                }
            }).start();

            // Main thread sends user input
            String input;
            while ((input = userInput.readLine()) != null) {
                serverOut.println(input);
            }

        } catch (IOException e) {
            System.out.println("Could not connect to server: " + e.getMessage());
        }
    }
}

    

