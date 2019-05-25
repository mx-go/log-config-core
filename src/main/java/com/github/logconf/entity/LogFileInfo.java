package com.github.logconf.entity;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Data;

@Data
public class LogFileInfo {

    private String name;
    private String parent;
    private long size;
    private String lastModified;
    @JSONField(serialize = false)
    private String absolutePath;
}
