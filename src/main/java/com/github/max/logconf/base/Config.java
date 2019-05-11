package com.github.max.logconf.base;

import com.google.common.base.Charsets;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.hash.Hashing;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;

/**
 * 根据缓存获取内容，支持纯文本或者KV格式的解析。并且使用懒加载模式，只在需要的时候做解析。
 */
@Slf4j
public class Config extends MapExt {
    private boolean parsed = false;
    private byte[] content;
    private String md5;

    public byte[] getContent() {
        return content;
    }

    public String getMd5() {
        return md5;
    }

    public void copyOf(String s) {
        this.content = s.getBytes(Charsets.UTF_8);
        this.md5 = Hashing.md5().hashBytes(this.content).toString();
        parsed = false;
    }

    public void copyOf(byte[] content) {
        this.content = content;
        this.md5 = Hashing.md5().hashBytes(this.content).toString();
        parsed = false;
    }

    @Override
    public void copyOf(Map<String, String> m) {
        super.copyOf(m);
        resetContent();
    }

    @Override
    public void copyOf(java.util.Properties props) {
        super.copyOf(props);
        resetContent();
    }

    @Override
    public MapExt putAll(Map<String, String> items) {
        super.putAll(items);
        resetContent();
        return this;
    }

    @Override
    public MapExt putAll(java.util.Properties props) {
        super.putAll(props);
        resetContent();
        return this;
    }

    private void resetContent() {
        parsed = true;
        Map<String, String> m = getAll();
        if (m.isEmpty()) {
            this.content = new byte[0];
        } else {
            StringBuilder sbd = new StringBuilder();
            for (Map.Entry<String, String> i : m.entrySet()) {
                sbd.append(i.getKey()).append('=').append(i.getValue()).append('\n');
            }
            this.content = sbd.toString().getBytes(Charsets.UTF_8);
        }
        this.md5 = Hashing.md5().hashBytes(this.content).toString();
    }

    private synchronized void parse() {
        if (!parsed) {
            Map<String, String> m = Maps.newLinkedHashMap();
            final byte[] bytes = content;
            if (bytes != null) {
                String txt = new String(bytes, Charsets.UTF_8);
                for (String i : lines(txt, true)) {
                    int pos = i.indexOf('=');
                    if (pos != -1) {
                        String k = i.substring(0, pos).trim();
                        int next = pos + 1;
                        if (next < i.length()) {
                            try {
                                m.put(k, unEscapeJava(i.substring(next).trim()));
                            } catch (Exception e) {
                                log.error("cannot escape:{}, content={}", i, content);
                            }
                        } else {
                            m.put(k, "");
                        }
                    }
                }
                super.copyOf(m);
            }
            parsed = true;
        }
    }

    /* copyFrom StringEscapeUtils.unescapeJava */
    private String unEscapeJava(String value) {
        if (value == null || value.length() == 0) {
            return value;
        }
        StringBuilder buf = null;
        int len = value.length();
        int len1 = len - 1;
        for (int i = 0; i < len; i++) {
            char ch = value.charAt(i);
            if (ch == '\\' && i < len1) {
                int j = i;
                i++;
                ch = value.charAt(i);
                switch (ch) {
                    case '\\':
                        ch = '\\';
                        break;
                    case '\"':
                        ch = '\"';
                        break;
                    case '\'':
                        ch = '\'';
                        break;
                    case 't':
                        ch = '\t';
                        break;
                    case 'n':
                        ch = '\n';
                        break;
                    case 'r':
                        ch = '\r';
                        break;
                    case 'b':
                        ch = '\b';
                        break;
                    case 'f':
                        ch = '\f';
                        break;
                    case 'u':
                    case 'U':
                        ch = (char) Integer.parseInt(value.substring(i + 1, i + 5), 16);
                        i = i + 4;
                        break;
                    default:
                        j--;
                }
                if (buf == null) {
                    buf = new StringBuilder(len);
                    if (j > 0) {
                        buf.append(value.substring(0, j));
                    }
                }
                buf.append(ch);
            } else if (buf != null) {
                buf.append(ch);
            }
        }
        if (buf != null) {
            return buf.toString();
        }
        return value;
    }

    @Override
    public String get(String key) {
        if (!parsed) {
            parse();
        }
        return super.get(key);
    }

    @Override
    public Map<String, String> getAll() {
        if (!parsed) {
            parse();
        }
        return super.getAll();
    }

    public String getString() {
        return new String(getContent(), Charsets.UTF_8);
    }

    public String getString(Charset charset) {
        return new String(getContent(), charset);
    }

    public List<String> getLines() {
        return getLines(Charsets.UTF_8, true);
    }

    public List<String> getLines(Charset charset) {
        return lines(new String(getContent(), charset), true);
    }

    public List<String> getLines(Charset charset, boolean removeComment) {
        return lines(new String(getContent(), charset), removeComment);
    }

    private List<String> lines(String s, boolean removeComment) {
        List<String> lines = Lists.newArrayList();
        if (!removeComment) {
            Splitter.on('\n').trimResults().omitEmptyStrings().split(s).forEach(lines::add);
        } else {
            Splitter.on('\n').trimResults().omitEmptyStrings().split(s).forEach(i -> {
                boolean skip = i.charAt(0) == '#' || i.startsWith("//");
                if (!skip) {
                    lines.add(i);
                }
            });
        }
        return lines;
    }
}
