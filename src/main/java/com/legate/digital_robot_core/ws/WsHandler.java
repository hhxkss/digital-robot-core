package com.legate.digital_robot_core.ws;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.legate.digital_robot_core.parser.MessageParser;
import com.legate.digital_robot_core.service.AvatarService;
import com.legate.digital_robot_core.service.RobotService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;

import java.io.IOException;
import java.util.Map;

@Slf4j
@Component
public class WsHandler implements WebSocketHandler {

    private final ObjectMapper om = new ObjectMapper();
    private final WsGateway gateway;
    private final RobotService robot;
    private final AvatarService avatar;
    private final MessageParser parser;

    public WsHandler(WsGateway gateway, RobotService robot,
                     AvatarService avatar, MessageParser parser) {
        this.gateway = gateway;
        this.robot = robot;
        this.avatar = avatar;
        this.parser = parser;

        // 底盘上报 → 由 RobotService 提供监听
        robot.addListener(this::onSerial);
    }

    private void onSerial(String payload) {
        gateway.publishRaw(Map.of("type","serial","ts",System.currentTimeMillis(),"data",payload));
    }

    @Override public void afterConnectionEstablished(WebSocketSession s) { gateway.register(s); }

    @Override
    public void handleMessage(WebSocketSession s, WebSocketMessage<?> m) throws IOException {
        WsMessage msg;
        try {
            msg = parser.parse(m.getPayload().toString());
        } catch (IllegalArgumentException e) {
            s.sendMessage(new TextMessage(om.writeValueAsString(Map.of(
                    "code", 400, "message", e.getMessage()
            ))));
            return;
        } catch (Exception e) {
            s.sendMessage(new TextMessage(om.writeValueAsString(Map.of(
                    "code", 500, "message", "invalid payload"
            ))));
            return;
        }

        try {
            if (msg.isRobotRequest()) {
                robot.handle(msg.getMethod(), msg.getData());
                s.sendMessage(new TextMessage(om.writeValueAsString(Map.of(
                        "id", msg.getId(), "op", "robot_response", "method", msg.getMethod(),
                        "code", 0, "message", "accepted"
                ))));
                return;
            }

            if (msg.isAvatarRequest()) {
                // 主控仅转发 request_avatar，不直接造 avatar_response
                avatar.handle(msg.getMethod(), msg.getData());
                // 如需立刻给请求方一个“已转发”的 ACK，可打开下行：
                // s.sendMessage(new TextMessage(om.writeValueAsString(Map.of(
                //     "id", msg.getId(), "op", "avatar_relay_ack", "method", msg.getMethod(),
                //     "code", 0, "message", "forwarded"
                // ))));
                return;
            }

            if (msg.isAvatarResponse()) {
                log.info("Avatar Response: " + msg.toString());
                avatar.onAvatarResponse(msg.getRaw()); // 完成本地闭环（如有等待）
                gateway.publishRaw(msg.getRaw());      // 广播给所有订阅者
                return;
            }

            // 其余消息（状态事件、日志等）→ 广播
            gateway.publishRaw(msg.getRaw());

        } catch (IllegalArgumentException e) {
            s.sendMessage(new TextMessage(om.writeValueAsString(Map.of(
                    "id", msg.getId(), "code", 400, "message", e.getMessage()
            ))));
        } catch (Exception e) {
            s.sendMessage(new TextMessage(om.writeValueAsString(Map.of(
                    "id", msg.getId(), "code", 500, "message", "internal error"
            ))));
        }
    }

    @Override public void handleTransportError(WebSocketSession s, Throwable e) { close(s); }
    @Override public void afterConnectionClosed(WebSocketSession s, CloseStatus cs) { gateway.unregister(s); }
    @Override public boolean supportsPartialMessages() { return false; }

    private void close(WebSocketSession s) {
        try { s.close(); } catch (Exception ignored) {}
        gateway.unregister(s);
    }
}