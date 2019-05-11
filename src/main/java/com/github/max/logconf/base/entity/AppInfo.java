package com.github.max.logconf.base.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AppInfo implements Serializable {

    private String ip;
    private String port;
    private String app;
    private String processName;
}