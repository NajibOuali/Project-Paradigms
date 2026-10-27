package com.example.chat.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;

public class NetworkConfig {
    private static final String DEFAULT_CONFIG_FILE = "config.properties";
    private static final String DEFAULT_IP = "localhost";
    private static final int DEFAULT_PORT = 3000;

    private String serverIp;
    private int serverPort;

    public NetworkConfig() {
        this(DEFAULT_CONFIG_FILE);
    }

    public NetworkConfig(String configFile) {
        this.serverIp = DEFAULT_IP;
        this.serverPort = DEFAULT_PORT;
        loadConfig(configFile);
    }

    public NetworkConfig(String serverIp, int serverPort) {
        this.serverIp = (serverIp != null && !serverIp.isBlank()) ? serverIp : DEFAULT_IP;
        this.serverPort = (serverPort > 0 && serverPort <= 65535) ? serverPort : DEFAULT_PORT;
    }

    private void loadConfig(String configFile) {
        Properties props = new Properties();
        // Try file from file system first, then from classpath
        File file = new File(configFile);
        if (file.exists()) {
            try (FileInputStream fis = new FileInputStream(file)) {
                props.load(fis);
            } catch (Exception e) {
                System.err.println("[WARN] Failed to load config from filesystem: " + e.getMessage());
            }
        } else {
            try (InputStream is = NetworkConfig.class.getClassLoader().getResourceAsStream(configFile)) {
                if (is != null) {
                    props.load(is);
                }
            } catch (Exception e) {
                System.err.println("[WARN] Failed to load config from classpath: " + e.getMessage());
            }
        }

        if (props.containsKey("server.ip")) {
            this.serverIp = props.getProperty("server.ip").trim();
        }
        if (props.containsKey("server.port")) {
            try {
                this.serverPort = Integer.parseInt(props.getProperty("server.port").trim());
            } catch (NumberFormatException ignored) {
            }
        }
    }

    public String getServerIp() {
        return serverIp;
    }

    public void setServerIp(String serverIp) {
        this.serverIp = serverIp;
    }

    public int getServerPort() {
        return serverPort;
    }

    public void setServerPort(int serverPort) {
        this.serverPort = serverPort;
    }
}
