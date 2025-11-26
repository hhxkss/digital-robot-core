package com.legate.digital_robot_core.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "robot.avatar")
public class AvatarProperties {
    /** Transport type for avatar communication: websocket (default) or disabled/custom. */
    private String transport = "websocket";
    /** WebSocket path for avatar streaming. */
    private String wsPath = "/api/v1/stream";

    public String getTransport() {
        return transport;
    }

    public void setTransport(String transport) {
        this.transport = transport;
    }

    public String getWsPath() {
        return wsPath;
    }

    public void setWsPath(String wsPath) {
        this.wsPath = wsPath;
    }
}
