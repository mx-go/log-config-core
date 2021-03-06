package com.github.logconf.helper;

/**
 * 常量
 **/
public interface Constant {

    int OK = 0;
    int ERROR = 1;

    String CONTENT_TYPE = "text/json;charset=UTF-8";


    /**
     * 获取所有logger level信息
     */
    String GET_ALL_URI = "/all";
    /**
     * 获取单个logger level信息
     */
    String GET_LOGGER_URI = "/getLogger";
    /**
     * 设置logger level
     */
    String SET_LEVEL_URI = "/setLevel";
    /**
     * 获取日志文件信息
     */
    String GET_LOG_FILES_URI = "/getLogFiles";
    /**
     * 查询最近N行日志
     */
    String PEEK_FILE_URI = "/peekFile";
}
