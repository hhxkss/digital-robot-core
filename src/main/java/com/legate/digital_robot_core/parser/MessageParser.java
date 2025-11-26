package com.legate.digital_robot_core.parser;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.legate.digital_robot_core.ws.WsMessage;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Component
public class MessageParser {

    private final ObjectMapper om = new ObjectMapper();

    @SuppressWarnings("unchecked")
    public WsMessage parse(String payload) throws java.io.IOException {
        // 1) 统一先读成 Map
        Map<String, Object> m = om.readValue(payload, new TypeReference<>() {});
        String id     = asString(m.get("id"));
        String op     = asString(m.get("op"));
        String method = asString(m.get("method"));
        Map<String, Object> data = asMap(m.get("data"));

        // 3) 生成/归一化字段
        if (id == null || id.isBlank()) id = UUID.randomUUID().toString();

        // 4) 最基本的校验
        if (( "request_robot".equals(op) || "request_avatar".equals(op) ) && (method == null || method.isBlank())) {
            throw new IllegalArgumentException("Missing 'method' for request");
        }
        if (data == null) data = Map.of();

        return new WsMessage(id, op, method, data, m);
    }

    private static String asString(Object v) { return v == null ? null : String.valueOf(v); }
    @SuppressWarnings("unchecked")
    private static Map<String, Object> asMap(Object v) { return (v instanceof Map) ? (Map<String,Object>) v : null; }
}
