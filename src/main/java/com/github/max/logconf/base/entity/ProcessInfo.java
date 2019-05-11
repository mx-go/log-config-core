package com.github.max.logconf.base.entity;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import lombok.Data;

import java.util.List;

@Data
public class ProcessInfo {
    private String path;
    private String name;
    private String profile;
    private String ip;
    private String port;
    private Long appId;

    /**
     * profile 优先级
     */
    public List<String> profilePriority() {
        List<String> paths = Lists.newArrayList();
        if (!Strings.isNullOrEmpty(ip)) {
            if (!Strings.isNullOrEmpty(port)) {
                paths.add(ip + ':' + port);
            }
            paths.add(ip);
        }
        if (!Strings.isNullOrEmpty(profile)) {
            paths.add(profile);
        }
        if (!Strings.isNullOrEmpty(name)) {
            paths.add(name);
        }
        return paths;
    }
}
