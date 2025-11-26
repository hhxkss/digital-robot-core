package com.legate.digital_robot_core.chassis.record;

/** “到达成功”专用事件，便于业务直接订阅处理 */
public record NavArrived(
        String point,
        double mileage,
        long ts
) { }
