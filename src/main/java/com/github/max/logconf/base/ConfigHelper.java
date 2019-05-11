package com.github.max.logconf.base;

import com.github.max.logconf.base.entity.ProcessInfo;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URL;
import java.net.URLDecoder;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 基础工具类
 */
@Slf4j
public class ConfigHelper {
    private ConfigHelper() {
    }

    public static Path getConfigPath() {
        return LazyHolder2.CONFIG_PATH;
    }

    public static Config getApplicationConfig() {
        return LazyHolder3.CONFIG;
    }

    public static ProcessInfo getProcessInfo() {
        return LazyHolder4.PROCESS_INFO;
    }

    /**
     * <pre>
     * 1.扫描配置参数 config.path
     * 2.扫描类路径下的 autoconf 目录
     * 3.如果找不到就用java.io.tmpdir
     * </pre>
     *
     * @return factory
     */
    private static Path scanConfigPath() {
        Path basePath = scanProperty();
        if (basePath != null) {
            return basePath;
        }
        //查找若干文件以便找到classes根目录
        String files = "autoconf,log4j.properties,logback.xml,logback-test.xml,application.properties";
        for (String i : Splitter.on(',').split(files)) {
            String s = scanResource(i);
            if (s != null) {
                basePath = new File(s).toPath().getParent().resolve("autoconf");
                File root = basePath.toFile();
                if (root.exists() || root.mkdir()) {
                    return basePath;
                }
            }
        }
        return new File(System.getProperty("java.io.tmpdir")).toPath();
    }

    /**
     * 看是否通过环境变量指明了本地文件cache的路径
     */
    private static Path scanProperty() {
        String localCachePath = System.getProperty("config.path");
        if (!Strings.isNullOrEmpty(localCachePath)) {
            File f = new File(localCachePath);
            f.mkdirs();
            return f.toPath();
        }
        return null;
    }

    /**
     * 在类路径下查找资源
     *
     * @param resource 资源名
     * @return 找到返回路径否则返回null
     */
    private static String scanResource(String resource) {
        try {
            Enumeration<URL> ps = Thread.currentThread().getContextClassLoader().getResources(resource);
            while (ps.hasMoreElements()) {
                URL url = ps.nextElement();
                if ("file".equals(url.getProtocol())) {
                    String path = url.getPath();
                    String os = System.getProperty("os.name", "");
                    if (os.toLowerCase().contains("windows")) {
                        path = path.substring(1);
                    }
                    File f = new File(path);
                    String enc = System.getProperty("file.encoding", "UTF-8");
                    return f.exists() ? path : URLDecoder.decode(path, enc);
                }
            }
        } catch (IOException e) {
            log.error("cannot find {} under classpath", resource, e);
        }
        return null;
    }

    /**
     * 扫描配置根目录或者类路径下的application.properties文件并解析
     *
     * @return 加载的配置信息
     */
    private static Config scanApplicationConfig() {
        // 系统默认配置
        String envAppId = System.getProperty("process.appId", "0");
        String envName = System.getProperty("process.name", "unknown");
        String envProfile = System.getProperty("process.profile");
        String springProfile = System.getProperty("spring.profiles.active", "develop");

        String profile = firstNonNull(envProfile, springProfile);

        Map<String, String> defaults = Maps.newHashMap();
        defaults.put("process.appId", envAppId);
        defaults.put("process.name", envName);
        defaults.put("process.profile", profile);
        // 配置文件配置
        List<String> names = Lists.newArrayList("application-default.properties", "application.properties");
        if (!Strings.isNullOrEmpty(profile)) {
            String name = "application-" + profile + ".properties";
            if (!names.contains(name)) {
                names.add(0, name);
            }
        }
        Config fileConfig = new Config();
        // 扫描类路径下的配置文件
        for (String i : names) {
            String path = scanResource(i);
            if (path != null) {
                try {
                    log.info("load applicationConfig from {}", path);
                    fileConfig.copyOf(Files.readAllBytes(Paths.get(path)));
                    break;
                } catch (IOException e) {
                    log.error("cannot load from {}", path, e);
                }
            }
        }

        // 环境变量配置
        Map<String, String> props = Maps.newHashMap();
        Set<String> keys = System.getProperties().stringPropertyNames();
        for (String key : keys) {
            // 避免windows环境下,路径反斜线导致解析失败
            String val = System.getProperty(key);
            if (val.indexOf('\\') < 0) {
                props.put(key, val);
            }
        }
        Config c = new Config();
        //查找顺序:系统默认 < 文件配置 < 环境变量配置
        c.putAll(defaults).putAll(fileConfig.getAll()).putAll(props);
        return c;
    }

    private static String get(Config config, String key, String defVal) {
        String val = System.getProperty(key);
        if (val != null) {
            return val;
        }
        val = config.get(key);
        if (val != null) {
            return val;
        }
        return defVal;
    }

    private static ProcessInfo scanProcessInfo() {
        Config config = getApplicationConfig();
        ProcessInfo info = new ProcessInfo();
        info.setAppId(config.getLong("process.appId", 0));
        info.setName(config.get("process.name"));
        info.setProfile(config.get("process.profile"));

        // k8s
        if (!Strings.isNullOrEmpty(System.getenv("KUBERNETES_PORT"))) {
            String ip = System.getenv("CLUSTER_IP");
            info.setIp(!Strings.isNullOrEmpty(ip) ? ip : IpUtil.getSiteLocalIp());
            info.setPort(System.getenv("TOMCAT_PORT"));
        } else {
            info.setIp(config.get("process.ip", IpUtil.getSiteLocalIp()));
            String s = get(config, "process.port", null);
            if (Strings.isNullOrEmpty(s)) {
                try {
                    Integer port = WebServer.getHttpPort();
                    if (port != null) {
                        info.setPort(port.toString());
                    }
                } catch (Exception ignored) {
                }
            } else {
                info.setPort(s);
            }
        }
        log.info("process.appId\t=\t{}", info.getAppId());
        log.info("process.name\t=\t{}", info.getName());
        log.info("process.profile\t=\t{}", info.getProfile());
        log.info("process.ip\t=\t{}", info.getIp());
        log.info("process.port\t=\t{}", info.getPort());
        return info;
    }

    @Deprecated
    public static String getServerInnerIP() {
        return IpUtil.getSiteLocalIp();
    }

    public static String getHostName() {
        if (System.getenv("COMPUTERNAME") != null) {
            return System.getenv("COMPUTERNAME");
        } else {
            return getHostNameForLinux();
        }
    }

    private static String getHostNameForLinux() {
        try {
            return (InetAddress.getLocalHost()).getHostName();
        } catch (UnknownHostException uhe) {
            log.error("error", uhe);
            // host = "hostname: hostname"
            String host = uhe.getMessage();
            if (host != null) {
                int colon = host.indexOf(':');
                if (colon > 0) {
                    return host.substring(0, colon);
                }
            }
            return "UnknownHost";
        }
    }

    /**
     * 获取本机内网ip，ip会在第一次访问后缓存起来，并且不会再更新
     * 所以那个模式可能不适合你的机器，本类只是方便大多数人的使用，如果你的
     * 机器不能用该模式获得ip，请使用NetworkInterfaceEx类自行获取
     *
     * @return 返回服务器内部IP
     */
    @Deprecated
    public static String scanServerInnerIP() {
        return IpUtil.getSiteLocalIp();
    }

    /**
     * <pre>
     * 判断一个IP是不是内网IP段的IP
     * 10.0.0.0 – 10.255.255.255
     * 172.16.0.0 – 172.31.255.255
     * 192.168.0.0 – 192.168.255.255
     * </pre>
     *
     * @param ip ip地址
     * @return 如果是内网返回true，否则返回false
     */
    @Deprecated
    public static boolean isInnerIP(String ip) {
        try {
            return InetAddress.getByName(ip).isSiteLocalAddress();
        } catch (UnknownHostException e) {
            log.error("cannot parse: {}", ip, e);
            return false;
        }
    }

    private static class LazyHolder2 {
        private static final Path CONFIG_PATH = scanConfigPath();
    }


    private static class LazyHolder3 {
        private static final Config CONFIG = scanApplicationConfig();
    }


    private static class LazyHolder4 {
        private static final ProcessInfo PROCESS_INFO = scanProcessInfo();
    }

    private static <T> T firstNonNull(T first, T... more) {
        if (first != null) {
            return first;
        }
        for (T i : more) {
            if (i != null) {
                return i;
            }
        }
        return null;
    }
}
