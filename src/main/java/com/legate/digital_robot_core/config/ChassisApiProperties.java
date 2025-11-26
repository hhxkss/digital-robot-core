package com.legate.digital_robot_core.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "robot.chassis.api")
public class ChassisApiProperties {
    /** Enable or disable the REST entrypoints that talk to the chassis. */
    private boolean enabled = true;
    /** Base path for the chassis REST controller. */
    private String basePath = "/api/v1/chassis";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getBasePath() {
        return basePath;
    }

    public void setBasePath(String basePath) {
        this.basePath = basePath;
    }
}
