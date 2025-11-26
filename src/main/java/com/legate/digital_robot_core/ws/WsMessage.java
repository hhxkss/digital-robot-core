package com.legate.digital_robot_core.ws;

import lombok.ToString;

import java.util.Map;

@ToString
public class WsMessage {
    private final String id;
    private final String op;
    private final String method;
    private final Map<String, Object> data;
    private final Map<String, Object> raw; // 原始Map，方便透传/调试

    public WsMessage(String id, String op, String method, Map<String, Object> data, Map<String, Object> raw) {
        this.id = id;
        this.op = op;
        this.method = method;
        this.data = data;
        this.raw = raw;
    }

    public String getId() { return id; }
    public String getOp() { return op; }
    public String getMethod() { return method; }
    public Map<String, Object> getData() { return data; }
    public Map<String, Object> getRaw() { return raw; }

    public boolean isRobotRequest() {
        return "request_robot".equals(op) || (method != null && method.startsWith("robot."));
    }
    public boolean isAvatarRequest() {
        return "request_avatar".equals(op) || (method != null && method.startsWith("avatar."));
    }
    public boolean isAvatarResponse() {
        return "avatar_response".equals(op);
    }
}