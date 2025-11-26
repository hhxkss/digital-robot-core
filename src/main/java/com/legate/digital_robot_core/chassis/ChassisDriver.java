package com.legate.digital_robot_core.chassis;

import java.util.function.Consumer;

public interface ChassisDriver extends AutoCloseable {
    void open();
    void close();
    void sendAscii(String s);
    void addListener(Consumer<String> onFrame);
}