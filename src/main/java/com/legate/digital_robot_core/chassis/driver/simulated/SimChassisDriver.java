package com.legate.digital_robot_core.chassis.driver.simulated;

import com.legate.digital_robot_core.chassis.ChassisDriver;
import lombok.extern.slf4j.Slf4j;

import java.util.Locale;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

@Slf4j
public class SimChassisDriver implements ChassisDriver {

    private final CopyOnWriteArrayList<Consumer<String>> listeners = new CopyOnWriteArrayList<>();
    private final ScheduledExecutorService es = Executors.newScheduledThreadPool(1, r -> {
        var t = new Thread(r, "sim-chassis"); t.setDaemon(true); return t;
    });

    // 导航模拟状态
    private volatile String target = null;
    private volatile double distToGoal = -1;
    private volatile double mileage = 0;
    private volatile double maxVel = 0.6;   // m/s，支持 max_vel 指令
    private final AtomicBoolean paused = new AtomicBoolean(false);
    private ScheduledFuture<?> tickTask;

    @Override public void open() { log.info("SimChassisDriver: open"); }
    @Override public void close() { if (tickTask != null) tickTask.cancel(true); es.shutdownNow(); }
    @Override public void addListener(Consumer<String> onFrame) { if (onFrame != null) listeners.add(onFrame); }

    @Override
    public void sendAscii(String s) {
        log.info("SIM << {}", s);
        s = s.trim();
        if (s.startsWith("nav_point[")) {
            // nav_point[NAME]
            String name = s.substring("nav_point[".length(), s.length()-1);
            startNav(name);
        } else if (s.equals("nav_pause")) {
            if (target != null && paused.compareAndSet(false, true)) {
                emitNav(2, 0, target, distToGoal, mileage); // PAUSED
            }
        } else if (s.equals("nav_resume")) {
            if (target != null && paused.compareAndSet(true, false)) {
                emitNav(5, 0, target, distToGoal, mileage); // RESUMED
            }
        } else if (s.equals("nav_cancel")) {
            cancelNav(false);
        } else if (s.startsWith("max_vel[")) {
            String v = s.substring("max_vel[".length(), s.length()-1);
            try {
                double nv = Double.parseDouble(v);
                if (nv >= 0.3 && nv <= 1.0) maxVel = nv;
            } catch (Exception ignored) {}
        } else if (s.equals("dock_start")) {
            emit("dock_state{1}"); // 简单模拟：1=回桩开始
        } else if (s.equals("dock_cancel") || s.equals("dock_stop")) {
            emit("dock_state{0}"); // 0=回桩取消/结束
        }
    }

    // ====== 导航模拟 ======
    private void startNav(String name) {
        cancelNav(true); // 取消上一次
        target = name;
        distToGoal = 8.0;   // 模拟 8 米路程（可按需调整/随机）
        mileage = 0;

        emitNav(6, 0, target, -1, 0); // RECEIVED
        emitNav(1, 0, target, distToGoal, mileage); // STARTED

        tickTask = es.scheduleAtFixedRate(() -> {
            if (paused.get()) return;
            double step = maxVel * 0.5; // 0.5s 一次
            distToGoal = Math.max(0, distToGoal - step);
            mileage += step;

            // 过程心跳（用 state=1 重复上报，带当前距离）
            emitNav(1, 0, target, distToGoal, mileage);

            if (distToGoal <= 0.01) {
                // 到达成功
                emitNav(3, 0, target, 0, mileage); // RESULT, code=0
                cancelNav(true);
            }
        }, 500, 500, TimeUnit.MILLISECONDS);
    }

    private void cancelNav(boolean silent) {
        if (tickTask != null) tickTask.cancel(false);
        if (target != null && !silent) {
            emitNav(4, 0, target, distToGoal, mileage); // CANCELED
        }
        target = null;
        distToGoal = -1;
        mileage = 0;
        paused.set(false);
    }

    // ====== 上报工具 ======
    private void emitNav(int state, int code, String name, double dist, double mile) {
        emit(String.format(Locale.ROOT, "nav_result{%d %d %s %.2f %.2f}", state, code, name, dist, mile));
    }

    private void emit(String s) {
        log.info("SIM >> {}", s);
        for (var l : listeners) { try { l.accept(s); } catch (Exception ignored) {} }
    }
}