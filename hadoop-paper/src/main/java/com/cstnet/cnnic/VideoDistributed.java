package com.cstnet.cnnic;

import com.cstnet.cnnic.mapred.LinesMapper;
import com.cstnet.cnnic.mapred.LinesReducer;
import com.cstnet.cnnic.util.Assert;
import com.cstnet.cnnic.util.FileUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.NLineInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

import java.io.IOException;

/**
 * Created by biantao on 16/7/13.
 * package command:
 * mvn clean package -Dmaven.test.skip=true
 */
public class VideoDistributed extends Configured implements Tool {
    public static final Log LOG = LogFactory.getLog(VideoDistributed.class);
    // default params
    private static final String NLINES = "mapreduce.input.lineinputformat.linespermap";
    private static final int DEFAULT_NLINES = 2;
    private static final String NREDUCER = "mapred.reduce.tasks";
    private static final int DEFAULT_NREDUCER = 1;
    private static final String SORT = "cnic.sort";
    public static final String SOURCE_PATH = "cnic.source";


    public static void main(String[] args) throws Exception {
        LOG.info(String.format("%s start...", getDescription()));
        int res = ToolRunner.run(new Configuration(), new VideoDistributed(), args);
        LOG.info(String.format("%s finished, exit with stauts %s", getDescription(), res));
    }

    protected static String getDescription() {
        return "VideoDistributed Task";
    }

    private static void printUsage() {
        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append("Usage: VideoDistributed [-options] <command> [args...]\n" +
                "\n" +
                "general options:\n" +
                "-Dmapreduce.input.lineinputformat.linespermap=2 (default:2)\n" +
                "-Dmapred.reduce.tasks=1 (default:1)\n" +
                "-Dcnic.sort=0\t\t\t\t(default:1) sort the records of file by their size, (1 sort 0 not)\n" +
                "-Dcnic.source=hdfs_path\t\t\t\n" +
                "-D etc..." +
                "\n\n" +
                "commands:\n" +
                "batch\t\t\t\tprocess some videos at the same time\n" +
                "single\t\t\t\tprocess a single video at the same time" +
                "\n\n" +
                "args:\n" +
                "inputpath\t\t\tinput files\n" +
                "outputpath\t\t\toutput file location" +
                "\n\n" +
                "example:" +
                "hadoop jar hadoop-paper-1.0-SNAPSHOT.jar com.cstnet.cnnic.VideoDistributed -Dmapreduce.input.lineinputformat.linespermap=2 " +
                "-Dmapred.reduce.tasks=1 " +
                "-Dcnic.source=hdfs_path " +
                "batch " +
                "hdfs:* " +
                "hdfs:*\n");
        System.out.println(stringBuffer.toString());
    }

    /**
     * 真实的提交job
     * @param conf
     * @param args
     * 0:输入文件地址,排序后的文件地址
     * 1:输出文件地址,无用信息,并非处理视频后的地址
     * @return
     * @throws IOException
     * @throws ClassNotFoundException
     * @throws InterruptedException
     */
    public int submitVideoJob(Configuration conf, String... args) throws IOException, ClassNotFoundException, InterruptedException {
        LOG.info("submit video job...");
        Job job = new Job(conf,"VideoDistributed");
        job.setNumReduceTasks(DEFAULT_NREDUCER);
        job.setJarByClass(VideoDistributed.class);
        job.setMapperClass(LinesMapper.class);
        job.setReducerClass(LinesReducer.class);
        job.setOutputKeyClass(LongWritable.class);
        job.setOutputValueClass(Text.class);
        job.setInputFormatClass(NLineInputFormat.class);
        NLineInputFormat.addInputPath(job, new Path(args[0]));
        FileOutputFormat.setOutputPath(job, new Path(args[1]));
        return job.waitForCompletion(true) ? 0 : 1;
    }

    public int run(String[] strings) throws Exception {
        if (Assert.isEmpty(strings) || strings.length != 3) {
            printUsage();
            System.exit(0);
        }

        Configuration conf = getConf();
        if (Assert.isEmpty(conf.get(SOURCE_PATH))) {
            LOG.error("execute source code is not set, program exist");
            System.exit(0);
        } else if(!FileUtil.exists(LOG, conf, conf.get(SOURCE_PATH))) {
            LOG.error("input execute source code is not a exist file, program exist");
            System.exit(0);
        }
        if (Assert.isEmpty(conf.get(NLINES))) {
            conf.setInt(NLINES, DEFAULT_NLINES);
            LOG.info(String.format("%s not set, use default %s", NLINES, DEFAULT_NLINES));
        }
        if (Assert.isEmpty(conf.get(NREDUCER))) {
            conf.setInt(NREDUCER, DEFAULT_NREDUCER);
            LOG.info(String.format("%s not set, use default %s", NREDUCER, DEFAULT_NREDUCER));
        }
        LOG.info("parse command and options");
        String command = strings[0];
        String input = strings[1];
        String output = strings[2];
        if (command.equals("batch")) {
            LOG.info(String.format("command is batch; input file is %s; output is %s", input, output));
            if (conf.getInt(SORT, 1) == 1) {
                try {
                    FileUtil.sortRecordsBySize(LOG, conf, input, conf.getInt(NLINES, DEFAULT_NLINES));
                } catch (Exception e) {
                    LOG.error("error:sort records, program exist");
                    e.printStackTrace();
                    System.exit(0);
                }
            }
        } else {
            LOG.info(String.format("command %s is wrong or not implement this command"));
        }

        // start to submit mr job
        int ret = submitVideoJob(conf, input, output);
        System.exit(ret);
        return 0;
    }
}
