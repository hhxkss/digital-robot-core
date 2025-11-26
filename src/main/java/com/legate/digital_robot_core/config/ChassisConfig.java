package com.legate.digital_robot_core.config;

import com.legate.digital_robot_core.chassis.*;
import com.legate.digital_robot_core.chassis.driver.simulated.SimChassisDriver;
import com.legate.digital_robot_core.chassis.driver.yangchen.SerialChassisDriver;
import com.legate.digital_robot_core.chassis.driver.yangchen.SerialLink;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ChassisConfig {

    @Bean
    @ConditionalOnProperty(prefix="robot.chassis", name="mode", havingValue="sim")
    public ChassisDriver simChassisDriver() {
        return new SimChassisDriver();
    }

    @Bean
    @ConditionalOnProperty(prefix="robot.chassis", name="mode", havingValue="yangcheng", matchIfMissing=true)
    public ChassisDriver serialChassisDriver(
            @Value("${robot.chassis.port:/dev/ttyUSB0}") String portName) {
        // 将 SerialLink 的回调指定为 driver.dispatch（见 SerialChassisDriver）
        SerialChassisDriver driver = new SerialChassisDriver(new SerialLink(portName, null));
        // 如果你的 SerialLink 必须构造时传回调，可改为：
        // var driver = new SerialChassisDriver(null);
        // var link   = new SerialLink(portName, driver::dispatch);
        // driver.setLink(link);
        return driver;
    }
}