package com.github.max.logconf.entity;

import ch.qos.logback.classic.Logger;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;

/**
 * Logger information.
 */
@Data
@AllArgsConstructor
public class LoggerConfig implements Serializable {

    private String logger;
    private String level;

    public LoggerConfig(Logger logger) {
        this.logger = logger.getName();
        this.level = logger.getEffectiveLevel().toString();
    }
}
