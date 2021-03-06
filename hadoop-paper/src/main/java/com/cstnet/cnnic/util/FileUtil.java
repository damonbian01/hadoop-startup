package com.cstnet.cnnic.util;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.*;
import org.apache.hadoop.io.IOUtils;

import java.io.IOException;
import java.util.*;

/**
 * Created by biantao on 16/7/13.
 */
public class FileUtil {
    public static final Log LOG = LogFactory.getLog(FileUtil.class);
    public static final String SEP = "\n";

    /**
     * 根据输入文件(文件每行代表一个文件的hdfs地址)
     * 根据大小排序纪录
     * @param file
     * @param split 计划的map数量
     * Point1:
     * 尽量每个map中size加起来的和相差最小
     * @return 返回重排序后文件对地址
     */
    public static String sortRecordsBySize(Configuration conf, String file, int split) throws IOException {
        LOG.info("sort records by their size");
        Set<String> files = readFsFile(conf, file);
        Map<String, Long> map = new HashMap<String, Long>();
        for (String f : files) {
            map.putAll(processFile(conf,f));
        }
        List<String> sortFiles = lenSort(conf, map, split);
        return writeFsFile(conf, file, sortFiles);
    }

    /**
     * 判断文件是否存在
     * @param conf
     * @param file
     * @return
     * @throws IOException
     */
    public static boolean exists(Configuration conf, String file) throws IOException {
        Path path = new Path(file);
        FileSystem fs = FileSystem.get(conf);
        if (!fs.isFile(path)) {
            LOG.error(String.format("[%s file not exist]", file));
            return false;
        }
        else return true;
    }

    /**
     * 读取文件内容,封装到Set中返回
     * @param conf
     * @param file
     * @return
     * @throws IOException
     */
    public static Set<String> readFsFile(Configuration conf, String file) throws IOException {
        Set<String> files = new HashSet<String>();
        Path path = new Path(file);
        FileSystem fs = FileSystem.get(conf);
        if (!fs.isFile(path)) {
            LOG.error(String.format("[%s] is not a file", file));
            return null;
        }
        FSDataInputStream fsin = fs.open(path);
        // read records per line
        String line;
        while (!Assert.isEmpty(line = fsin.readLine())) {
            files.add(line);
        }
        LOG.info(String.format("%s records have been read", files.size()));
        IOUtils.closeStream(fsin);
        return Assert.isEmpty(files) ? null : files;
    }

    /**
     * 将排序后的文件重写到hfds,文件名为sort_file(即加上前缀sort_)
     * @param conf
     * @param file
     * @return 返回新保存的文件名
     */
    public static String writeFsFile(Configuration conf, String file, List<String> content) throws IOException {
        Path prePath = new Path(file);
        Path path = new Path(prePath.getParent(), "sort_" + prePath.getName());
        FileSystem fs = FileSystem.get(conf);
        if (fs.exists(path)) {
            LOG.info(String.format("%s has been existed, delete first", path.getName()));
            fs.delete(path, true);
        }
        StringBuffer stringBuffer = new StringBuffer();
        for (String string : content) {
            stringBuffer.append(string);
            stringBuffer.append(SEP);
            LOG.info(String.format("[record %s has been written to %s]", string, path.getName()));
        }
        FSDataOutputStream fout = fs.create(path);
        fout.write(stringBuffer.toString().getBytes());
        IOUtils.closeStream(fout);
        LOG.info(String.format("sorted records have been witten to %s", path.getParent() + "/" + path.getName()));
        return path.getParent() + "/" + path.getName();
    }

    /**
     * 处理单个文件,返回<文件名, 文件大小>
     * Point2:
     * 如果能根据文件所在的机器就更好了
     * @param conf
     * @param file
     * @return
     */
    public static Map<String, Long> processFile(Configuration conf, String file) throws IOException {
        Map<String, Long> map = new HashMap<String, Long>();
        Path path = new Path(file);
        FileSystem fs = FileSystem.get(conf);
        if (!fs.isFile(path)) {
            LOG.info(String.format("[%s] is not a file", file));
            return null;
        }
        FileStatus fileStatus = fs.getFileStatus(path);
        long blockSize = fileStatus.getBlockSize();
        long len = fileStatus.getLen();
        LOG.info(String.format("[file:%s blocksize:%s len:%s]", file, blockSize, len));
        map.put(file, len);
        return Assert.isEmpty(map) ? null : map;
    }

    /**
     * TODO
     * 自定义排序算法,根据split和文件大小衡量
     * @param conf
     * @param files
     * @param split
     */
    public static List<String> lenSort(Configuration conf, Map<String, Long> files, int split) {
        LOG.info("lenSort a key point in the algorithm");
        List<String> list = new ArrayList<String>();
        list.addAll(files.keySet());
        return list;
    }

}
