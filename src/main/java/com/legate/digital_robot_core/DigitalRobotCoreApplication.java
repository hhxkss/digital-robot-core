package com.legate.digital_robot_core;

import com.legate.digital_robot_core.config.AvatarProperties;
import com.legate.digital_robot_core.config.ChassisApiProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({AvatarProperties.class, ChassisApiProperties.class})
public class DigitalRobotCoreApplication {

        public static void main(String[] args) {
                SpringApplication.run(DigitalRobotCoreApplication.class, args);
        }

}
