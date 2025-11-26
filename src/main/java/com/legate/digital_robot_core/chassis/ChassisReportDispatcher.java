package com.legate.digital_robot_core.chassis;

import com.legate.digital_robot_core.service.RobotService;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.OrderComparator;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class ChassisReportDispatcher {

    private final RobotService robot;
    private final List<ChassisReportHandler> handlers; // Spring 自动注入所有实现

    public ChassisReportDispatcher(RobotService robot, List<ChassisReportHandler> handlers) {
        // 可按 @Order 指定处理先后
        this.robot = robot;
        this.handlers = handlers.stream().sorted(OrderComparator.INSTANCE).toList();
    }

    @PostConstruct
    void init() {
        // 订阅原始上报：每来一条，轮询处理器
        robot.addListener(this::dispatch);
        log.info("ChassisReportDispatcher registered {} handlers", handlers.size());
    }

    private void dispatch(String raw) {
        for (var h : handlers) {
            try {
                if (h.canHandle(raw)) {
                    h.handle(raw);
                    break;
                }
            } catch (Exception e) {
                log.warn("handler {} error: {}", h.getClass().getSimpleName(), e.getMessage(), e);
            }
        }
    }
}