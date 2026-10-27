package com.example.chat.server;

import java.util.List;

public interface ServerEventListener {
    void onLog(String message);
    void onUserJoined(String username);
    void onUserLeft(String username);
    void onUserListUpdated(List<String> activeUsers);
}
