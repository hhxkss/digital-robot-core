package com.legate.digital_robot_core.ws;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.*;

@Configuration
@EnableWebSocket
public class WsConfig implements WebSocketConfigurer {
    private final WsHandler handler;
    public WsConfig(WsHandler handler) { this.handler = handler; }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(handler, "/api/v1/stream")
                .setAllowedOrigins("*");
    }
}