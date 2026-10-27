package com.example.chat.client;

public interface ClientEventListener {
    void onMessageReceived(String message);
    void onConnected();
    void onDisconnected(String reason);
    void onError(String errorMessage);
}
