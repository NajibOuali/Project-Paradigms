package com.example.chat;

import com.example.chat.client.ClientEventListener;
import com.example.chat.client.TCPClientModel;
import com.example.chat.config.NetworkConfig;
import com.example.chat.server.ServerEventListener;
import com.example.chat.server.TCPServerModel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

public class ChatIntegrationTest {

    private int testPort;
    private TCPServerModel serverModel;

    @BeforeEach
    void setUp() throws IOException {
        // Find a free port
        try (ServerSocket socket = new ServerSocket(0)) {
            testPort = socket.getLocalPort();
        }
        serverModel = new TCPServerModel("localhost", testPort);
        serverModel.start();
    }

    @AfterEach
    void tearDown() {
        if (serverModel != null) {
            serverModel.stop();
        }
    }

    @Test
    void testNetworkConfigLoading() {
        NetworkConfig config = new NetworkConfig();
        assertNotNull(config.getServerIp());
        assertTrue(config.getServerPort() > 0);

        NetworkConfig customConfig = new NetworkConfig("127.0.0.1", 8080);
        assertEquals("127.0.0.1", customConfig.getServerIp());
        assertEquals(8080, customConfig.getServerPort());
    }

    @Test
    void testClientConnectionAndMessaging() throws Exception {
        CountDownLatch aliceJoinedLatch = new CountDownLatch(1);
        CountDownLatch bobReceivedMessageLatch = new CountDownLatch(1);
        List<String> bobReceivedMessages = new CopyOnWriteArrayList<>();

        TCPClientModel alice = new TCPClientModel("localhost", testPort, "Alice");
        TCPClientModel bob = new TCPClientModel("localhost", testPort, "Bob");

        alice.addListener(new ClientEventListener() {
            @Override
            public void onMessageReceived(String message) {}
            @Override
            public void onConnected() {
                aliceJoinedLatch.countDown();
            }
            @Override
            public void onDisconnected(String reason) {}
            @Override
            public void onError(String errorMessage) {}
        });

        bob.addListener(new ClientEventListener() {
            @Override
            public void onMessageReceived(String message) {
                bobReceivedMessages.add(message);
                if (message.contains("Hello from Alice!")) {
                    bobReceivedMessageLatch.countDown();
                }
            }
            @Override
            public void onConnected() {}
            @Override
            public void onDisconnected(String reason) {}
            @Override
            public void onError(String errorMessage) {}
        });

        alice.connect();
        bob.connect();

        assertTrue(aliceJoinedLatch.await(3, TimeUnit.SECONDS));
        Thread.sleep(200);

        alice.sendMessage("Hello from Alice!");

        assertTrue(bobReceivedMessageLatch.await(3, TimeUnit.SECONDS));
        assertTrue(bobReceivedMessages.stream().anyMatch(m -> m.contains("Alice: Hello from Alice!")));

        alice.disconnect("Test ended");
        bob.disconnect("Test ended");
    }

    @Test
    void testActiveUserInquiryCommand() throws Exception {
        CountDownLatch usersReceivedLatch = new CountDownLatch(1);
        List<String> receivedMessages = new CopyOnWriteArrayList<>();

        TCPClientModel alice = new TCPClientModel("localhost", testPort, "Alice");
        TCPClientModel bob = new TCPClientModel("localhost", testPort, "Bob");

        alice.addListener(new ClientEventListener() {
            @Override
            public void onMessageReceived(String message) {
                receivedMessages.add(message);
                if (message.contains("[SERVER - Active Users]") && message.contains("Alice") && message.contains("Bob")) {
                    usersReceivedLatch.countDown();
                }
            }
            @Override
            public void onConnected() {}
            @Override
            public void onDisconnected(String reason) {}
            @Override
            public void onError(String errorMessage) {}
        });

        alice.connect();
        bob.connect();
        Thread.sleep(200);

        alice.requestActiveUsers();

        assertTrue(usersReceivedLatch.await(3, TimeUnit.SECONDS));

        alice.disconnect("Test ended");
        bob.disconnect("Test ended");
    }

    @Test
    void testReadOnlyModeCannotSendMessage() throws Exception {
        CountDownLatch errorLatch = new CountDownLatch(1);
        List<String> clientErrors = new CopyOnWriteArrayList<>();

        TCPClientModel readOnlyClient = new TCPClientModel("localhost", testPort, ""); // empty username = read-only
        assertTrue(readOnlyClient.isReadOnly());

        readOnlyClient.addListener(new ClientEventListener() {
            @Override
            public void onMessageReceived(String message) {}
            @Override
            public void onConnected() {}
            @Override
            public void onDisconnected(String reason) {}
            @Override
            public void onError(String errorMessage) {
                clientErrors.add(errorMessage);
                errorLatch.countDown();
            }
        });

        readOnlyClient.connect();
        Thread.sleep(200);

        readOnlyClient.sendMessage("Should not be sent");

        assertTrue(errorLatch.await(3, TimeUnit.SECONDS));
        assertTrue(clientErrors.stream().anyMatch(e -> e.contains("READ-ONLY MODE")));

        readOnlyClient.disconnect("Test ended");
    }

    @Test
    void testDisconnectionCommands() throws Exception {
        CountDownLatch disconnectLatch = new CountDownLatch(1);

        TCPClientModel alice = new TCPClientModel("localhost", testPort, "Alice");
        alice.addListener(new ClientEventListener() {
            @Override
            public void onMessageReceived(String message) {}
            @Override
            public void onConnected() {}
            @Override
            public void onDisconnected(String reason) {
                disconnectLatch.countDown();
            }
            @Override
            public void onError(String errorMessage) {}
        });

        alice.connect();
        Thread.sleep(200);

        alice.sendMessage("bye");

        assertTrue(disconnectLatch.await(3, TimeUnit.SECONDS));
        assertFalse(alice.isConnected());
    }

    @Test
    void testServerEventListenerCallbacks() throws Exception {
        CountDownLatch userJoinedLatch = new CountDownLatch(1);
        CountDownLatch userLeftLatch = new CountDownLatch(1);

        serverModel.addListener(new ServerEventListener() {
            @Override
            public void onLog(String message) {}
            @Override
            public void onUserJoined(String username) {
                if ("Charlie".equals(username)) {
                    userJoinedLatch.countDown();
                }
            }
            @Override
            public void onUserLeft(String username) {
                if ("Charlie".equals(username)) {
                    userLeftLatch.countDown();
                }
            }
            @Override
            public void onUserListUpdated(List<String> activeUsers) {}
        });

        TCPClientModel charlie = new TCPClientModel("localhost", testPort, "Charlie");
        charlie.connect();

        assertTrue(userJoinedLatch.await(3, TimeUnit.SECONDS));

        charlie.sendMessage("end");

        assertTrue(userLeftLatch.await(3, TimeUnit.SECONDS));
    }
}
