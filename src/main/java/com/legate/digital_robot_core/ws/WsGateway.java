package com.legate.digital_robot_core.ws;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class WsGateway implements WsOutbound {

    private final Set<WebSocketSession> sessions = ConcurrentHashMap.newKeySet();
    private final ObjectMapper om = new ObjectMapper();

    void register(WebSocketSession s) { sessions.add(s); }
    void unregister(WebSocketSession s) { sessions.remove(s); }

    @Override
    public void publishRaw(Object obj) {
        String json;
        try { json = om.writeValueAsString(obj); } catch (Exception e) { return; }
        for (var sess : sessions) {
            if (sess.isOpen()) {
                try { sess.sendMessage(new TextMessage(json)); } catch (IOException ignored) {}
            }
        }
    }

    @Override
    public void publishToAvatar(String id, String method, Map<String, Object> data) {
        String msgId = (id != null) ? id : UUID.randomUUID().toString();
        publishRaw(Map.of(
                "id", msgId,
                "op", "request_avatar",
                "method", method,
                "data", data != null ? data : Map.of()
        ));
    }
}
