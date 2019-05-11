package com.github.max.logconf.helper;

import ch.qos.logback.classic.AsyncAppender;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.FileAppender;
import com.github.max.logconf.entity.LogFileInfo;
import com.github.max.logconf.entity.LoggerConfig;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.ILoggerFactory;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * 对logback操作类
 *
 * @author max
 */
public class LoggingUtil {

    private static final Set<String> ALL_LOG_LEVEL = Sets.newHashSet("OFF", "TRACE", "DEBUG", "INFO", "WARN", "ERROR");
    private static List<LogFileInfo> logFileInfoCache = Lists.newArrayList();

    private LoggingUtil() {
    }

    /**
     * Get a single logger.
     *
     * @return Logger
     */
    public static Logger getLogger(final String loggerName) {
        ILoggerFactory loggerFactory = LoggerFactory.getILoggerFactory();
        if (loggerFactory instanceof LoggerContext) {
            LoggerContext context = (LoggerContext) loggerFactory;
            return context.getLogger(loggerName);
        }
        return null;
    }

    public static boolean setLogger(LoggerConfig config) {
        ILoggerFactory loggerFactory = LoggerFactory.getILoggerFactory();
        if (loggerFactory instanceof LoggerContext) {
            LoggerContext context = (LoggerContext) loggerFactory;
            context.getLogger(config.getLogger()).setLevel(Level.valueOf(config.getLevel()));
            return true;
        }
        return false;
    }

    /**
     * 得到日志文件信息,只遍历ROOT和AsyncAppender下的appender
     *
     * @return LogFileInfo列表
     */
    public static List<LogFileInfo> getLogFileInfos() {
        final SimpleDateFormat dataFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        final List<LogFileInfo> logFileInfos = Lists.newArrayList();
        Set<Appender<ILoggingEvent>> appenders = getAppenders();
        appenders.forEach(ref -> {
            if (ref instanceof FileAppender) {
                final FileAppender<ILoggingEvent> fileAppender = (FileAppender<ILoggingEvent>) ref;
                final File logFile = new File(fileAppender.getFile());
                final LogFileInfo logFileInfo = new LogFileInfo();
                logFileInfo.setName(logFile.getName());
                logFileInfo.setParent(logFile.getParent());
                logFileInfo.setAbsolutePath(logFile.getAbsolutePath());
                long lastModified = logFile.lastModified();
                String lastModifiedFormatted = dataFormat.format(new Date(lastModified));
                logFileInfo.setLastModified(lastModifiedFormatted);
                logFileInfo.setSize(logFile.length());
                logFileInfos.add(logFileInfo);
            }
        });
        logFileInfoCache = logFileInfos;
        return logFileInfos;
    }

    /**
     * 只遍历ROOT和AsyncAppender下的appender
     *
     * @return appenders
     */
    private static Set<Appender<ILoggingEvent>> getAppenders() {
        Set<Appender<ILoggingEvent>> appenders = Sets.newHashSet();

        List<Logger> loggers = getLoggers(false);
        loggers.forEach(logger -> {
            final Iterator<Appender<ILoggingEvent>> appenderRefs = logger.iteratorForAppenders();
            while (appenderRefs.hasNext()) {
                final Appender<ILoggingEvent> appenderRef = appenderRefs.next();
                if (appenderRef instanceof AsyncAppender) {
                    final Iterator<Appender<ILoggingEvent>> asyncAppenderRefs = ((AsyncAppender) appenderRef).iteratorForAppenders();
                    while (asyncAppenderRefs.hasNext()) {
                        final Appender<ILoggingEvent> asyncRef = asyncAppenderRefs.next();
                        appenders.add(asyncRef);
                    }
                } else {
                    appenders.add(appenderRef);
                }
            }
        });

        return appenders;
    }

    /**
     * 检索所有配置的logger
     *
     * @param showAll 是否返回所有的logger,包括未在xml中配置的
     * @return logger列表
     */
    public static List<Logger> getLoggers(final boolean showAll) {
        ILoggerFactory loggerFactory = LoggerFactory.getILoggerFactory();
        final List<Logger> loggers = Lists.newArrayList();
        if (!(loggerFactory instanceof LoggerContext)) {
            return loggers;
        }
        LoggerContext context = (LoggerContext) loggerFactory;
        for (Logger log : context.getLoggerList()) {
            if (!showAll) {
                if (log.getLevel() != null || LoggingUtil.hasAppenders(log)) {
                    loggers.add(log);
                }
            } else {
                loggers.add(log);
            }
        }
        return loggers;
    }

    /**
     * judge whether the provided logger has appenders.
     *
     * @param logger The logger to test
     * @return true if the logger has appenders.
     */
    private static boolean hasAppenders(Logger logger) {
        Iterator<Appender<ILoggingEvent>> it = logger.iteratorForAppenders();
        return it.hasNext();
    }

    public static String getFileAbsolutePath(String file) {
        if (StringUtils.isEmpty(file)) {
            return "";
        }
        for (LogFileInfo info : logFileInfoCache) {
            if (StringUtils.equals(file, info.getName())) {
                return info.getAbsolutePath();
            }
        }
        return "";
    }

    public static boolean isValid(final String level) {
        String levelUpperCase = StringUtils.upperCase(level);
        return ALL_LOG_LEVEL.contains(levelUpperCase);
    }

}
