package com.legate.digital_robot_core.service;

import com.legate.digital_robot_core.ws.WsOutbound;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;

@Service
public class AvatarService {

    private final WsOutbound outbound;
    private final ConcurrentHashMap<String, CompletableFuture<Map<String, Object>>> pending = new ConcurrentHashMap<>();
    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(5);

    public AvatarService(WsOutbound outbound) {
        this.outbound = outbound; // 可在单测里传入一个假的 WsOutbound
    }

    // —— 强类型接口（单测可直接调用） ——
    public void narrateStart(Map<String, Object> data) {
        outbound.publishToAvatar(UUID.randomUUID().toString(), "avatar.narrate.start", data);
    }
    public void narratePause(Map<String, Object> data) {
        outbound.publishToAvatar(UUID.randomUUID().toString(), "avatar.narrate.pause", data);
    }
    public void narrateResume(Map<String, Object> data) {
        outbound.publishToAvatar(UUID.randomUUID().toString(), "avatar.narrate.resume", data);
    }
    public void narrateStop(Map<String, Object> data) {
        outbound.publishToAvatar(UUID.randomUUID().toString(), "avatar.narrate.stop", data);
    }

    /** 发送 request_avatar 并等待 avatar_response（闭环） */
    public Map<String, Object> request(String method, Map<String, Object> data, Duration timeout)
            throws InterruptedException, ExecutionException, TimeoutException {
        String id = data != null && data.get("id") instanceof String ? (String) data.get("id") : UUID.randomUUID().toString();
        var fut = new CompletableFuture<Map<String, Object>>();
        pending.put(id, fut);
        outbound.publishToAvatar(id, method, data);
        try {
            return fut.get(timeout != null ? timeout.toMillis() : DEFAULT_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            pending.remove(id);
            throw e;
        }
    }

    /** WsHandler 收到 avatar_response 时回调 */
    public void onAvatarResponse(Map<String, Object> resp) {
        Object idObj = resp.get("id");
        if (!(idObj instanceof String id)) return;
        var fut = pending.remove(id);
        if (fut != null && !fut.isDone()) fut.complete(resp);
    }

    // —— 动态分发（给 WsHandler 用；默认走 fire-and-forget） ——
    public Map<String, Object> handle(String method, Map<String, Object> data) {
        outbound.publishToAvatar(UUID.randomUUID().toString(), method, data);
        return Map.of("published", true);
    }
}
