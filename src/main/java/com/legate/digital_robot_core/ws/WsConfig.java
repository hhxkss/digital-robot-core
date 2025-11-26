package com.legate.digital_robot_core.ws;

import com.legate.digital_robot_core.config.AvatarProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.*;

@Configuration
@EnableWebSocket
@ConditionalOnProperty(prefix = "robot.avatar", name = "transport", havingValue = "websocket", matchIfMissing = true)
public class WsConfig implements WebSocketConfigurer {
    private final WsHandler handler;
    private final AvatarProperties avatarProperties;

    public WsConfig(WsHandler handler, AvatarProperties avatarProperties) {
        this.handler = handler;
        this.avatarProperties = avatarProperties;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(handler, avatarProperties.getWsPath())
                .setAllowedOrigins("*");
    }
}