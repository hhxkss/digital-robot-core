package com.legate.digital_robot_core.chassis.driver.yangchen;

import com.legate.digital_robot_core.chassis.ChassisDriver;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

@Slf4j
public class SerialChassisDriver implements ChassisDriver {

    private final SerialLink link;
    private final CopyOnWriteArrayList<Consumer<String>> listeners = new CopyOnWriteArrayList<>();

    public SerialChassisDriver(SerialLink link) {
        // 将底层回调转发给上层监听者
        this.link = link;
    }

    @Override public void open() {
        // 将 SerialLink 的回调指向本类的分发器
        link.open(); // 你现有的 SerialLink 构造中已经传入了回调（建议传 this::dispatch）
    }

    @Override public void close() { try { link.close(); } catch (Exception ignored) {} }
    @Override public void sendAscii(String s) { link.sendAscii(s); }
    @Override public void addListener(Consumer<String> onFrame) { if (onFrame != null) listeners.add(onFrame); }

    // 如果你的 SerialLink 是在构造时注入回调，这里需要一个供其回调的方法：
    public void dispatch(String raw) {
        for (var l : listeners) { try { l.accept(raw); } catch (Exception ignored) {} }
    }
}
