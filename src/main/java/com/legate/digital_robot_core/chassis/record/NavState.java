package com.legate.digital_robot_core.chassis.record;

/** 底盘导航状态（映射 nav_result 的 state 字段：1/2/3/4/5/6） */
public enum NavState {
    RECEIVED,   // 6：已接收未开始
    STARTED,    // 1：开始
    PAUSED,     // 2：暂停
    RESULT,     // 3：结果（成功/失败看 code）
    CANCELED,   // 4：取消
    RESUMED     // 5：恢复
}
