package com.example.chat.server;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TCPServerModel {
    private final String host;
    private final int port;
    private ServerSocket serverSocket;
    private final List<ClientHandler> clients = new CopyOnWriteArrayList<>();
    private final List<ServerEventListener> listeners = new CopyOnWriteArrayList<>();
    private ExecutorService executorService;
    private volatile boolean running = false;
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

    public TCPServerModel(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public void addListener(ServerEventListener listener) {
        listeners.add(listener);
    }

    public void removeListener(ServerEventListener listener) {
        listeners.remove(listener);
    }

    public synchronized void start() throws IOException {
        if (running) return;

        executorService = Executors.newVirtualThreadPerTaskExecutor();
        if (host != null && !host.isBlank() && !host.equalsIgnoreCase("0.0.0.0")) {
            serverSocket = new ServerSocket(port, 50, InetAddress.getByName(host));
        } else {
            serverSocket = new ServerSocket(port);
        }
        running = true;

        log("Server Started on " + (host != null ? host : "0.0.0.0") + ":" + port);
        log("Waiting for Client connections...");

        // Start server accept loop in a virtual thread
        executorService.submit(this::acceptClientsLoop);
    }

    private void acceptClientsLoop() {
        while (running) {
            try {
                Socket clientSocket = serverSocket.accept();
                if (!running) break;
                ClientHandler handler = new ClientHandler(clientSocket, this);
                // Submit handler execution using virtual thread per client
                executorService.submit(handler);
            } catch (IOException e) {
                if (running) {
                    log("[ERROR] Error accepting client: " + e.getMessage());
                }
            }
        }
    }

    public synchronized void registerClient(ClientHandler handler) {
        clients.add(handler);
        String name = handler.getUsername();
        log("Welcome " + name + " connected from " + handler.getSocket().getRemoteSocketAddress());
        
        // Notify other clients about the new user joining
        broadcastSystemMessage("[SYSTEM] " + name + " has joined the chat.");

        for (ServerEventListener listener : listeners) {
            listener.onUserJoined(name);
            listener.onUserListUpdated(getActiveUsernames());
        }
    }

    public synchronized void unregisterClient(ClientHandler handler) {
        if (clients.remove(handler)) {
            String name = handler.getUsername();
            log("Client disconnected: " + name);
            broadcastSystemMessage("[SYSTEM] " + name + " has left the chat.");

            for (ServerEventListener listener : listeners) {
                listener.onUserLeft(name);
                listener.onUserListUpdated(getActiveUsernames());
            }
        }
    }

    public void broadcastMessage(String senderUsername, String message) {
        String timestamp = LocalTime.now().format(TIME_FORMATTER);
        String formattedMessage = "[" + timestamp + "] " + senderUsername + ": " + message;
        log("[CHAT] " + formattedMessage);
        for (ClientHandler client : clients) {
            client.sendMessage(formattedMessage);
        }
    }

    public void broadcastSystemMessage(String message) {
        log("[BROADCAST] " + message);
        for (ClientHandler client : clients) {
            client.sendMessage(message);
        }
    }

    public List<String> getActiveUsernames() {
        List<String> list = new ArrayList<>();
        for (ClientHandler client : clients) {
            list.add(client.getUsername());
        }
        return Collections.unmodifiableList(list);
    }

    public String getActiveUsersString() {
        List<String> list = getActiveUsernames();
        if (list.isEmpty()) {
            return "No active users.";
        }
        return String.join(", ", list) + " (" + list.size() + " online)";
    }

    public synchronized void stop() {
        if (!running) return;
        running = false;
        log("Stopping server...");

        for (ClientHandler client : new ArrayList<>(clients)) {
            client.close();
        }
        clients.clear();

        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException ignored) {}

        if (executorService != null) {
            executorService.shutdownNow();
        }

        log("Server stopped.");
    }

    public boolean isRunning() {
        return running;
    }

    public int getPort() {
        return port;
    }

    public String getHost() {
        return host;
    }

    private void log(String message) {
        System.out.println(message);
        for (ServerEventListener listener : listeners) {
            listener.onLog(message);
        }
    }
}
