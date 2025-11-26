package com.legate.digital_robot_core.ws;

import java.util.Map;

public interface WsOutbound {
    void publishRaw(Object obj);

    /** 主控 → 数字人：广播 request_avatar（id 原样透传以便闭环） */
    void publishToAvatar(String id, String method, Map<String, Object> data);
}
