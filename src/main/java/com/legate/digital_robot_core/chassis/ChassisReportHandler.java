package com.legate.digital_robot_core.chassis;

public interface ChassisReportHandler {

    boolean canHandle(String raw);

    void handle(String raw) throws Exception;
}
