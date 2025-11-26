package com.legate.digital_robot_core.chassis.handler;

import com.legate.digital_robot_core.chassis.ChassisReportHandler;
import com.legate.digital_robot_core.service.AvatarService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.annotation.Order;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.regex.Pattern;

@Slf4j
@Component
@Order(100)
public class NavResultHandler implements ChassisReportHandler {

    private static final Pattern NAV_RESULT = Pattern.compile(
            "^\\s*nav_result\\{\\s*(\\d+)\\s+(\\-?\\d+)\\s+([^\\s{}]+)\\s+([\\-\\d\\.]+)\\s+([\\-\\d\\.]+)\\s*}\\s*$"
    );

    private final AvatarService avatar;
    private final TaskExecutor navExecutor;  // 用于异步执行，避免阻塞串口读线程

    public NavResultHandler(
            AvatarService avatar,
            @Qualifier("navExecutor") TaskExecutor navExecutor) {
        this.avatar = avatar;
        this.navExecutor = navExecutor;
    }

    @Override
    public boolean canHandle(String raw) {
        return raw != null && raw.startsWith("nav_result");
    }

    @Override
    public void handle(String raw) {
        log.info(raw);
        var m = NAV_RESULT.matcher(raw);
        if (!m.matches()) return;

        int state = Integer.parseInt(m.group(1)); // 3=结果
        int code  = Integer.parseInt(m.group(2)); // 0=成功
        String point = m.group(3);

        if (state == 3 && code == 0) {
            // 异步触发，防止卡住串口读线程
            navExecutor.execute(() -> {
                try {
                    avatar.narrateStart(Map.of("point", point));
                    log.info("NavResultHandler: auto-narrate on arrival, point={}", point);
                } catch (Exception e) {
                    log.warn("auto-narrate failed: {}", e.getMessage(), e);
                }
            });
        }
    }
}