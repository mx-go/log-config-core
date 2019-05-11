package com.github.max.logconf.servlets;

import ch.qos.logback.classic.Logger;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.github.max.logconf.base.ConfigHelper;
import com.github.max.logconf.base.SimpleHttpClient;
import com.github.max.logconf.base.entity.AppInfo;
import com.github.max.logconf.entity.LogFileInfo;
import com.github.max.logconf.entity.LoggerConfig;
import com.github.max.logconf.base.entity.ProcessInfo;
import com.github.max.logconf.helper.*;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Logback servlet, 提供logback修改配置接口
 */
@Slf4j
@SuppressWarnings("serial")
public class LogbackServlet extends HttpServlet {

    private String logConfWeb;

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String uri = req.getPathInfo();
        log.info("logback servlet request uri: {}", uri);
        if (uri != null && !"/".equals(uri)) {
            switch (uri) {
                case Constant.GET_ALL_URI:
                    this.getAllLoggers(req, resp);
                    break;
                case Constant.GET_LOGGER_URI:
                    this.getLogger(req, resp);
                    break;
                case Constant.SET_LEVEL_URI:
                    this.setLoggerLevel(req, resp);
                    break;
                case Constant.GET_LOG_FILES_URI:
                    this.getLogFiles(resp);
                    break;
                case Constant.PEEK_FILE_URI:
                    this.peekFile(req, resp);
                    break;
                default:
                    resp.sendError(404);
                    break;
            }
        } else {
            super.service(req, resp);
        }
    }

    private void getAllLoggers(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        // 是否返回所有的logger,包括未在xml中配置的
        String showAllStr = getParam(req, "showAll", "false");
        boolean showAll = Boolean.parseBoolean(showAllStr);

        List<LoggerConfig> loggerInfoList = Lists.newArrayList();
        List<Logger> loggers = LoggingUtil.getLoggers(showAll);
        if (loggers != null) {
            loggerInfoList.addAll(loggers.stream().map(LoggerConfig::new).collect(Collectors.toList()));
        }
        String retJson = toJson(Constant.OK, JSON.toJSON(loggerInfoList));
        log.debug("get all loggers: {}", retJson);
        resp.setContentType(Constant.CONTENT_TYPE);
        resp.getWriter().write(retJson);
    }

    private void getLogger(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType(Constant.CONTENT_TYPE);
        String loggerName = this.getParam(req, "logger", "");
        if (StringUtils.isEmpty(loggerName)) {
            String msg = "logger不能为空";
            resp.getWriter().write(toJson(Constant.ERROR, msg));
            return;
        }
        Logger logger = LoggingUtil.getLogger(loggerName);
        LoggerConfig config = new LoggerConfig(logger);
        resp.getWriter().write(toJson(Constant.OK, JSON.toJSON(config)));
    }

    private void setLoggerLevel(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType(Constant.CONTENT_TYPE);
        String loggerName = getParam(req, "logger", "");
        String level = getParam(req, "level", "");
        String newLevel = getParam(req, "newLevel", "");
        log.warn("change logger level start! logger: {}, oldLevel: {}, newLevel: {}", loggerName, level, newLevel);

        if (StringUtils.isEmpty(loggerName)) {
            String msg = "logger不能为空";
            resp.getWriter().write(toJson(Constant.ERROR, msg));
            return;
        }
        if (StringUtils.isEmpty(newLevel) || !LoggingUtil.isValid(newLevel)) {
            String msg = "新level不能为空, 并且应该在level列表中";
            resp.getWriter().write(toJson(Constant.ERROR, msg));
            return;
        }
        LoggerConfig config = new LoggerConfig(loggerName, newLevel);
        boolean success = LoggingUtil.setLogger(config);
        if (success) {
            String msg = "修改日志level成功";
            resp.getWriter().write(toJson(Constant.OK, msg));
        } else {
            String msg = "修改日志level失败";
            resp.getWriter().write(toJson(Constant.ERROR, msg));
        }
    }

    private void getLogFiles(HttpServletResponse resp) throws IOException {
        resp.setContentType(Constant.CONTENT_TYPE);
        List<LogFileInfo> logs = LoggingUtil.getLogFileInfos();
        resp.getWriter().write(toJson(Constant.OK, JSON.toJSON(logs)));
    }

    private void peekFile(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType(Constant.CONTENT_TYPE);
        String file = getParam(req, "file", "");
        String num = getParam(req, "num", "1000");

        if (StringUtils.isEmpty(file)) {
            String msg = "文件名称不能为空";
            resp.getWriter().write(toJson(Constant.ERROR, msg));
            return;
        }

        int tailNum;
        try {
            tailNum = Integer.parseInt(num);
        } catch (Exception e) {
            tailNum = 1000;
            log.error("tail num {} is not valid", num, e);
        }
        String filePath = LoggingUtil.getFileAbsolutePath(file);
        List<String> lines = FileUtil.tailFile(filePath, tailNum, "UTF-8");
        if (filePath == null || lines == null) {
            String msg = "日志文件tail失败";
            resp.getWriter().write(toJson(Constant.ERROR, msg));
        } else {
            resp.getWriter().write(toJson(Constant.OK, JSON.toJSON(lines)));
        }
    }

    private String getParam(HttpServletRequest request, String key, String defaultValue) {
        Preconditions.checkNotNull(key, "param key should not be empty");
        String value = request.getParameter(key);
        if (StringUtils.isEmpty(value)) {
            return defaultValue;
        } else {
            return value;
        }
    }

    private String toJson(int status, Object data) {
        JSONObject json = new JSONObject();
        json.put("status", status);
        json.put("data", data);
        return json.toJSONString();
    }

    @Override
    public void destroy() {
        try {
            AppInfo ai = getAppInfo();
            String params = String.format("?ip=%s&port=%s&app=%s&processName=%s", ai.getIp(), ai.getPort(), ai.getApp(), ai.getProcessName());
            String url = logConfWeb + Constant.UNREGISTER_URI + params;
            log.info("unregister logController, url={}", url);
            SimpleHttpClient.get(url);
        } catch (Exception e) {
            log.error("Cannot unregister from logconf. ", e);
        }
    }

    @Override
    public void init() throws ServletException {
        // FIXME 可注册到服务中心，统一展示所有服务并可进行修改
        if (!Strings.isNullOrEmpty(ConfigHelper.getProcessInfo().getPort())) {
            String port = System.getenv("xxx_PORT");
            if (!Strings.isNullOrEmpty(port)) {
                // 运行在k8s中，直接使用service名字
                logConfWeb = "http://xxx/logconf";
            } else {
                String profile = ConfigHelper.getProcessInfo().getProfile();
                if (StringUtils.equals(profile, "xxx")) {
                    logConfWeb = "http://xxx/logconf";
                } else {
                    logConfWeb = "http://xxx/logconf";
                }
            }

            lazyRegister();
        } else {
            log.info("Cannot detect container port, will not register to logconf.");
        }
        log.info("{} init", this);
    }

    private void lazyRegister() {
        new Thread(() -> {
            try {
                AppInfo ai = getAppInfo();
                String params = String.format("?ip=%s&port=%s&app=%s&processName=%s", ai.getIp(), ai.getPort(), ai.getApp(), ai.getProcessName());
                String url = logConfWeb + Constant.REGISTER_URI + params;
                log.info("Register to logController url={}", url);
                SimpleHttpClient.get(url);
            } catch (Exception e) {
                log.error("Cannot register to logController. ", e);
            }
        }).start();
    }

    private AppInfo getAppInfo() {
        ProcessInfo info = ConfigHelper.getProcessInfo();
        String ip = info.getIp();
        String port = info.getPort();
        String appName = StringUtils.substringAfter(this.getServletContext().getContextPath(), "/");
        String processName = info.getName();
        return new AppInfo(ip, port, appName, processName);
    }
}
