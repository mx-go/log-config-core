package com.github.max.logconf.base;

import com.google.common.collect.Lists;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;
import java.util.List;

/**
 * 获取本机ip信息
 */
@Slf4j
@UtilityClass
public class IpUtil {
    private static final String SERVER_IP;

    static {
        List<String> ips = getIpV4LocalAddresses();
        SERVER_IP = ips.isEmpty() ? "127.0.0.1" : ips.get(0);
    }

    public String getSiteLocalIp() {
        return SERVER_IP;
    }

    public List<String> getIpV4LocalAddresses() {
        List<String> ips = Lists.newArrayList();
        try {
            Enumeration<NetworkInterface> e = NetworkInterface.getNetworkInterfaces();
            while (e.hasMoreElements()) {
                NetworkInterface ni = e.nextElement();
                boolean skip = ni.isLoopback() || ni.isVirtual() || ni.getName().startsWith("docker");
                if (!skip) {
                    Enumeration<InetAddress> en = ni.getInetAddresses();
                    while (en.hasMoreElements()) {
                        InetAddress net = en.nextElement();
                        String ip = net.getHostAddress();
                        if (ip.indexOf(':') < 0 && net.isSiteLocalAddress()) {
                            ips.add(ip);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("cannot scan ips", e);
        }
        return ips;
    }
}