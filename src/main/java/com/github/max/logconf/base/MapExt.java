package com.github.max.logconf.base;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

import java.util.Map;

/**
 * 扩展map功能，可以获取不同类型的属性值
 */
public class MapExt {
    private Map<String, String> m = ImmutableMap.of();

    public void copyOf(Map<String, String> items) {
        this.m = ImmutableMap.copyOf(items);
    }

    public void copyOf(java.util.Properties props) {
        this.m = Maps.fromProperties(props);
    }

    /**
     * 将items的内容合并到当前配置中
     *
     * @param items 配置map
     * @return 当前对象
     */
    public MapExt putAll(Map<String, String> items) {
        Map<String, String> all = Maps.newHashMap(this.m);
        all.putAll(items);
        this.m = ImmutableMap.copyOf(all);
        return this;
    }

    /**
     * 将properties合并到当前配置中
     *
     * @param props 配置
     * @return 当前对象
     */
    public MapExt putAll(java.util.Properties props) {
        Map<String, String> all = Maps.newHashMap(this.m);
        for (String key : props.stringPropertyNames()) {
            Object obj = props.get(key);
            if (obj != null) {
                all.put(key, String.valueOf(obj));
            }
        }
        this.m = ImmutableMap.copyOf(all);
        return this;
    }

    public int getInt(String key) {
        return getInt(key, 0);
    }

    public int getInt(String key, int defaultVal) {
        String val = get(key);
        if (!Strings.isNullOrEmpty(val)) {
            try {
                return Integer.parseInt(val);
            } catch (NumberFormatException ignored) {
            }
        }
        return defaultVal;
    }

    public long getLong(String key) {
        return getLong(key, 0L);
    }

    public long getLong(String key, long defaultVal) {
        String val = get(key);
        if (!Strings.isNullOrEmpty(val)) {
            try {
                return Long.parseLong(val);
            } catch (NumberFormatException ignored) {
            }
        }
        return defaultVal;
    }

    public boolean getBool(String key) {
        return getBool(key, false);
    }

    public boolean getBool(String key, boolean defaultVal) {
        String val = get(key);
        if (!Strings.isNullOrEmpty(val)) {
            return Boolean.parseBoolean(val);
        }
        return defaultVal;
    }

    public double getDouble(String key) {
        return getDouble(key, 0.0);
    }

    public double getDouble(String key, double defaultVal) {
        String val = get(key);
        if (!Strings.isNullOrEmpty(val)) {
            try {
                return Double.parseDouble(val);
            } catch (NumberFormatException ignored) {
            }
        }
        return defaultVal;
    }

    public String get(String key, String defaultVal) {
        String val = get(key);
        return val == null ? defaultVal : val;
    }

    public boolean has(String key) {
        return get(key) != null;
    }

    /**
     * 只有真正按照kv格式查找的时候，才进行解析对应kv内容。避免解析非KV格式的配置
     *
     * @param key 查找的key
     * @return 获取对应的value
     */
    public String get(String key) {
        return m.get(key);
    }

    public Map<String, String> getAll() {
        return m;
    }
}
