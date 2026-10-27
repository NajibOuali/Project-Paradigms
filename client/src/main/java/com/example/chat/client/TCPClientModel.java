package com.example.chat.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class TCPClientModel {
    private final String host;
    private final int port;
    private final String username;
    private final boolean isReadOnly;
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private volatile boolean connected = false;
    private final List<ClientEventListener> listeners = new CopyOnWriteArrayList<>();
    private Thread listenerThread;

    public TCPClientModel(String host, int port, String username) {
        this.host = host;
        this.port = port;
        this.username = (username != null) ? username.trim() : "";
        this.isReadOnly = this.username.isEmpty();
    }

    public void addListener(ClientEventListener listener) {
        listeners.add(listener);
    }

    public void removeListener(ClientEventListener listener) {
        listeners.remove(listener);
    }

    public synchronized void connect() throws IOException {
        if (connected) return;

        socket = new Socket(host, port);
        out = new PrintWriter(socket.getOutputStream(), true, StandardCharsets.UTF_8);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        connected = true;

        // Send handshake username
        if (isReadOnly) {
            out.println("USER:");
        } else {
            out.println("USER:" + username);
        }

        notifyConnected();

        // Start listening in background virtual thread
        listenerThread = Thread.ofVirtual().start(this::listenForMessages);
    }

    private void listenForMessages() {
        try {
            String message;
            while (connected && (message = in.readLine()) != null) {
                notifyMessageReceived(message);
            }
        } catch (IOException e) {
            if (connected) {
                notifyError("Connection lost: " + e.getMessage());
            }
        } finally {
            disconnect("Connection closed");
        }
    }

    public synchronized void sendMessage(String message) {
        if (!connected) {
            notifyError("Not connected to server.");
            return;
        }
        if (isReadOnly) {
            notifyError("Cannot send message: You are in READ-ONLY MODE.");
            return;
        }
        if (message == null || message.trim().isEmpty()) {
            return;
        }

        out.println(message.trim());

        if (message.trim().equalsIgnoreCase("end") || message.trim().equalsIgnoreCase("bye")) {
            disconnect("Disconnected by user command (" + message.trim() + ")");
        }
    }

    public synchronized void requestActiveUsers() {
        if (!connected) {
            notifyError("Not connected to server.");
            return;
        }
        out.println("allUsers");
    }

    public synchronized void disconnect(String reason) {
        if (!connected) return;
        connected = false;

        try {
            if (out != null) {
                out.println("end");
            }
        } catch (Exception ignored) {}

        try {
            if (in != null) in.close();
        } catch (IOException ignored) {}
        try {
            if (out != null) out.close();
        } catch (Exception ignored) {}
        try {
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException ignored) {}

        notifyDisconnected(reason);
    }

    private void notifyConnected() {
        for (ClientEventListener listener : listeners) {
            listener.onConnected();
        }
    }

    private void notifyMessageReceived(String message) {
        for (ClientEventListener listener : listeners) {
            listener.onMessageReceived(message);
        }
    }

    private void notifyDisconnected(String reason) {
        for (ClientEventListener listener : listeners) {
            listener.onDisconnected(reason);
        }
    }

    private void notifyError(String errorMessage) {
        for (ClientEventListener listener : listeners) {
            listener.onError(errorMessage);
        }
    }

    public boolean isConnected() {
        return connected;
    }

    public boolean isReadOnly() {
        return isReadOnly;
    }

    public String getUsername() {
        return username;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }
}
