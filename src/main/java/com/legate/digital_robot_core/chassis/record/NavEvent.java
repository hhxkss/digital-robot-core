package com.legate.digital_robot_core.chassis.record;

public record NavEvent(
        NavState state,
        int code,
        String name,
        double distToGoal,
        double mileage,
        long ts
) { }