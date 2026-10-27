package com.example.chat.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class ClientHandler implements Runnable {
    private final Socket socket;
    private final TCPServerModel server;
    private PrintWriter out;
    private BufferedReader in;
    private String username;
    private boolean isReadOnly = false;
    private volatile boolean running = true;

    public ClientHandler(Socket socket, TCPServerModel server) {
        this.socket = socket;
        this.server = server;
    }

    @Override
    public void run() {
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            out = new PrintWriter(socket.getOutputStream(), true, StandardCharsets.UTF_8);

            // First line from client is the username / handshake
            // Protocol:
            // "USER:<username>" or "USER:" (for empty / read-only) or raw username
            String initialMessage = in.readLine();
            if (initialMessage == null) {
                close();
                return;
            }

            String requestedName = initialMessage.trim();
            if (requestedName.startsWith("USER:")) {
                requestedName = requestedName.substring(5).trim();
            }

            if (requestedName.isEmpty() || requestedName.equalsIgnoreCase("READ_ONLY") || requestedName.equalsIgnoreCase("ANONYMOUS")) {
                this.isReadOnly = true;
                this.username = "ReadOnlyUser_" + socket.getPort();
            } else {
                this.isReadOnly = false;
                this.username = requestedName;
            }

            server.registerClient(this);

            if (isReadOnly) {
                sendMessage("[SYSTEM] You are connected in READ-ONLY MODE. You cannot send messages.");
            } else {
                sendMessage("[SYSTEM] Welcome " + username + "! You are connected to the chat.");
            }

            String message;
            while (running && (message = in.readLine()) != null) {
                message = message.trim();
                if (message.isEmpty()) {
                    continue;
                }

                // Handle commands
                if (message.equalsIgnoreCase("end") || message.equalsIgnoreCase("bye") || message.equalsIgnoreCase("/leave")) {
                    sendMessage("[SYSTEM] Disconnecting...");
                    break;
                } else if (message.equalsIgnoreCase("allUsers") || message.equalsIgnoreCase("/allUsers")) {
                    String activeUsers = server.getActiveUsersString();
                    sendMessage("[SERVER - Active Users] " + activeUsers);
                } else {
                    if (isReadOnly) {
                        sendMessage("[ERROR] You are in READ-ONLY MODE. Sending messages is not permitted.");
                    } else {
                        server.broadcastMessage(username, message);
                    }
                }
            }
        } catch (IOException e) {
            // Client abruptly disconnected or read error
        } finally {
            close();
        }
    }

    public synchronized void sendMessage(String message) {
        if (out != null) {
            out.println(message);
        }
    }

    public void close() {
        if (!running) return;
        running = false;
        try {
            server.unregisterClient(this);
        } catch (Exception ignored) {
        }
        try {
            if (in != null) in.close();
        } catch (IOException ignored) {}
        try {
            if (out != null) out.close();
        } catch (Exception ignored) {}
        try {
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException ignored) {}
    }

    public String getUsername() {
        return username;
    }

    public boolean isReadOnly() {
        return isReadOnly;
    }

    public Socket getSocket() {
        return socket;
    }
}
