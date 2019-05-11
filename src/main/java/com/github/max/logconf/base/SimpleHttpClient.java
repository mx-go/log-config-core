package com.github.max.logconf.base;

import com.google.common.base.Charsets;
import lombok.experimental.UtilityClass;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.zip.GZIPInputStream;

/**
 * HTTP请求
 */
@UtilityClass
public class SimpleHttpClient {

    static {
        System.setProperty("sun.net.client.defaultConnectTimeout", "30000");
        System.setProperty("sun.net.client.defaultReadTimeout", "30000");
    }

    public String get(String url) throws IOException {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(10000);
            connection.setRequestProperty("Connection", "Close");
            connection.setRequestProperty("Charset", "UTF-8");
            connection.setRequestProperty("Accept-Encoding", "gzip, deflate");
            connection.connect();
            return readResponse(connection);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    public String post(String url, Object body) throws IOException {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(10000);
            connection.setDoOutput(true);
            connection.setDoInput(true);
            connection.setUseCaches(false);
            connection.setRequestProperty("Connection", "Close");
            connection.setRequestProperty("Charset", "UTF-8");
            connection.setRequestProperty("Accept-Encoding", "gzip, deflate");
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            connection.connect();

            try (DataOutputStream dos = new DataOutputStream(connection.getOutputStream())) {
                Charset utf8 = Charsets.UTF_8;
                if (body instanceof String) {
                    dos.write(body.toString().getBytes(utf8));
                } else if (body instanceof Map) {
                    for (Object obj : ((Map) body).entrySet()) {
                        Map.Entry kv = (Map.Entry) obj;
                        dos.write(encode(kv.getKey().toString()).getBytes(utf8));
                        dos.write('=');
                        dos.write(encode(kv.getValue().toString()).getBytes(utf8));
                        dos.write('&');
                    }
                }
                dos.flush();
            }

            return readResponse(connection);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    public String postJson(String url, String json) throws IOException {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(10000);
            connection.setDoOutput(true);
            connection.setDoInput(true);
            connection.setUseCaches(false);
            connection.setRequestProperty("Connection", "Close");
            connection.setRequestProperty("Charset", "UTF-8");
            connection.setRequestProperty("Accept-Encoding", "gzip, deflate");
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            connection.connect();

            try (DataOutputStream dos = new DataOutputStream(connection.getOutputStream())) {
                dos.write(json.getBytes(Charsets.UTF_8));
                dos.flush();
            }

            return readResponse(connection);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private String readResponse(HttpURLConnection connection) throws IOException {
        int statusCode = connection.getResponseCode();
        if (statusCode < 400) {
            String enc = connection.getContentEncoding();
            if (enc != null && enc.toLowerCase().contains("gzip")) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(new GZIPInputStream(connection.getInputStream()), Charsets.UTF_8))) {
                    return read(reader);
                }
            } else {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), Charsets.UTF_8))) {
                    return read(reader);
                }
            }
        }
        return null;
    }

    private String read(BufferedReader reader) throws IOException {
        StringBuilder sbd = new StringBuilder(2048);
        String line;
        while ((line = reader.readLine()) != null) {
            sbd.append(line).append('\n');
        }
        return sbd.toString();
    }

    private String encode(String s) {
        try {
            return URLEncoder.encode(s, "UTF-8");
        } catch (UnsupportedEncodingException ignored) {
            return s;
        }
    }
}
