package com.legate.digital_robot_core.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.legate.digital_robot_core.chassis.ChassisDriver;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

@Slf4j
@Service
@lombok.RequiredArgsConstructor
public class RobotService {

    // 串口名通过配置注入：linux 常用 /dev/ttyUSB0 /dev/ttyS0，Windows 常用 COM3 等
    @Value("${robot.chassis.port:/dev/ttyUSB0}")
    private String portName;

    // 启动时自动打开串口（可配置为 false）
    @Value("${robot.chassis.autoOpen:true}")
    private boolean autoOpen;

    private final ChassisDriver driver;
    private final CopyOnWriteArrayList<Consumer<String>> listeners = new CopyOnWriteArrayList<>();
    private final ObjectMapper om = new ObjectMapper();

    @PostConstruct
    public void init() {
        driver.addListener(this::onFrame);
        if (autoOpen) driver.open();
    }

    @PreDestroy
    public void shutdown() {
        try { driver.close(); } catch (Exception ignored) {}
    }

    /** 导航到目标点：nav_point[名称] */
    public void navToPoint(String point) {
        String p = (point == null || point.isBlank()) ? "entr" : point;
        sendCmd("nav_point[" + p + "]"); // 文档定义 :contentReference[oaicite:8]{index=8}
    }

    /** 暂停导航：nav_pause */
    public void navPause() {
        sendCmd("nav_pause");            // 文档定义 :contentReference[oaicite:9]{index=9}
    }

    /** 恢复导航：nav_resume */
    public void navResume() {
        sendCmd("nav_resume");           // 文档定义 :contentReference[oaicite:10]{index=10}
    }

    /** 取消导航：nav_cancel */
    public void navCancel() {
        sendCmd("nav_cancel");           // 文档定义 :contentReference[oaicite:11]{index=11}
    }

    public void dockStart() {
        sendCmd("dock_start");
    }

    /** 取消回桩：dock_cancel（若底盘协议是 dock_stop，也可在这里换成 dock_stop） */
    public void dockCancel() {
        sendCmd("dock_cancel");
    }

    public void setMaxVel(double vel) {
        if (Double.isNaN(vel) || vel < 0.3 || vel > 1.0) {
            throw new IllegalArgumentException("max_vel out of range [0.3, 1.0]: " + vel);
        }
        double v = Math.round(vel * 10.0) / 10.0;
        sendCmd("max_vel[" + v + "]");
    }

    /** 供上层订阅到底盘上报（这里先原样转发；如需可在此解析成结构化事件） */
    public void addListener(Consumer<String> onFrame) {
        if (onFrame != null) listeners.add(onFrame);
    }

    // —— WsHandler 动态分发用（保持与你现有 method 名称一致） ——
    public Map<String,Object> handle(String method, Map<String,Object> data) {
        switch (Objects.requireNonNull(method)) {
            case "robot.nav.toPoint" -> { navToPoint(data != null ? (String) data.get("point") : null); return Map.of(); }
            case "robot.nav.cancel"  -> { navCancel();  return Map.of(); }
            case "robot.nav.pause"   -> { navPause();   return Map.of(); }
            case "robot.nav.resume"  -> { navResume();  return Map.of(); }
            case "robot.docker.start"-> { dockStart();  return Map.of(); }
            case "robot.docker.stop" -> { dockCancel();   return Map.of(); }

            // === 新增：设置最大速度 ===
            case "robot.nav.setMaxVel", "robot.nav.setMaxSpeed" -> {
                // 兼容多种字段名：value / vel / speed
                Object val = (data != null) ? (data.getOrDefault("value",
                        data.getOrDefault("vel", data.get("speed")))) : null;
                if (val == null) throw new IllegalArgumentException("missing 'value' for setMaxVel");
                double v = (val instanceof Number) ? ((Number) val).doubleValue() : Double.parseDouble(String.valueOf(val));
                setMaxVel(v);
                return java.util.Map.of();
            }

            default -> throw new IllegalArgumentException("unknown robot method: " + method);
        }
    }

    private void onFrame(String raw) {
        log.debug("<< chassis {}", raw);
        for (var l : listeners) {
            try { l.accept(raw); } catch (Exception ignored) {}
        }
    }

    // === 发送助手 ===
    private void sendCmd(String cmd) {
        if (driver == null) throw new IllegalStateException("serial not initialized");
        driver.sendAscii(cmd);
    }
}