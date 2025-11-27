package com.legate.digital_robot_core.avatar;

import java.util.Map;

/**
 * Abstraction over the outbound channel to the avatar so transports can be swapped via configuration.
 */
public interface AvatarGateway {
    void publishRaw(Object obj);

    void publishToAvatar(String id, String method, Map<String, Object> data);
}
