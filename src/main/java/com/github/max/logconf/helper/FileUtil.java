package com.github.max.logconf.helper;

import com.google.common.collect.Lists;
import com.google.common.collect.Queues;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.List;
import java.util.Queue;

/**
 * File操作工具类
 *
 * @author : admin
 */
public class FileUtil {

    private static final Logger LOG = LoggerFactory.getLogger(FileUtil.class);
    /**
     * 1M
     */
    private static final int ONE_MB = 1024 * 1024;

    private FileUtil() {
    }

    /**
     * 获取文件最后N行数据
     *
     * @param path 文件路径
     * @param num  tail行数
     * @return 文件N行列表
     */
    public static List<String> tailFile(String path, int num, String charset) {
        List<String> lines = Lists.newArrayList();
        if (StringUtils.isEmpty(path)) {
            return lines;
        }
        File file = new File(path);
        if (!file.exists() || file.isDirectory()) {
            return lines;
        }

        Queue<String> lineQueue = Queues.newArrayBlockingQueue(num);

        try (RandomAccessFile rdmFile = new RandomAccessFile(file, "r")) {
            long fileSize = file.length();
            long seekSize = Math.max(0, fileSize - ONE_MB);
            rdmFile.seek(seekSize);

            String line;
            while ((line = rdmFile.readLine()) != null) {
                if (lineQueue.size() == num) {
                    lineQueue.poll();
                }
                String lineC = new String(line.getBytes("8859_1"), charset);
                lineQueue.offer(lineC);
            }
        } catch (IOException e) {
            LOG.error("Cannot read from file {}", path, e);
        }
        return Lists.newArrayList(lineQueue);
    }

}
