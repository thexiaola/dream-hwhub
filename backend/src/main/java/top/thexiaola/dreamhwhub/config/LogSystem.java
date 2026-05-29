package top.thexiaola.dreamhwhub.config;

import ch.qos.logback.core.rolling.RollingPolicyBase;
import ch.qos.logback.core.rolling.RolloverFailure;
import cn.hutool.core.date.DateUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileWriter;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 日志系统
 * 格式: log_{日期}_{启动次数}_{文件序号}.log
 */
@Slf4j
@Component
public class LogSystem extends RollingPolicyBase {

    // 滚动策略相关常量
    private static final String DATE_FORMAT = "yyyyMMdd";
    private static final long MAX_FILE_SIZE = 50 * 1024 * 1024;
    private static final String LOGS_DIR = "logs";

    // 滚动策略状态变量
    private String currentDate;
    private int currentFileIndex = 1;
    private int startupCount = 1;

    @Override
    public void start() {
        currentDate = DateUtil.format(new Date(), DATE_FORMAT);
        startupCount = calculateStartupCount();
        super.start();
        log.info("Unified logging system initialization completed");
    }

    @Override
    public void stop() {
        currentDate = null;
        currentFileIndex = 1;
        startupCount = 1;
        super.stop();
        log.info("Log rolling policy stopped, resources released");
    }

    // 确保日志目录存在
    private void ensureLogsDirectoryExists() {
        File logsDir = new File(LOGS_DIR);
        if (!logsDir.exists()) {
            boolean created = logsDir.mkdirs();
            if (created) {
                log.info("Creating logs directory: {}", LOGS_DIR);
            }
        }
    }

    /**
     * 检查文件大小并在必要时触发滚动
     *
     * @param file 要检查的文件
     */
    private void checkFileSizeAndRollover(File file) {
        if (file.exists() && file.length() >= MAX_FILE_SIZE) {
            currentFileIndex++;
            log.debug("File {} exceeds size limit, preparing to roll to next file", file.getName());
        }
    }

    /**
     * 检查并处理日期变化
     */
    private void checkAndHandleDateChange() {
        String today = DateUtil.format(new Date(), DATE_FORMAT);
        if (!today.equals(currentDate)) {
            log.info("Detected date change: {} -> {}, automatically switching log file", currentDate, today);
            currentDate = today;
            startupCount = calculateStartupCount();
            currentFileIndex = 1;
            log.info("Date switch completed, new startup count: {}, file index reset to: {}", startupCount, currentFileIndex);
        }
    }

    /**
     * 格式化日志消息
     */
    private String formatLogMessage(String message, String level) {
        return String.format("%s [%s] %s - %s",
                DateUtil.format(new Date(), "yyyy-MM-dd HH:mm:ss.SSS"),
                Thread.currentThread().getName(),
                level.toUpperCase(),
                message);
    }

    /**
     * 写入备用日志（当主日志文件写入失败时）
     */
    private void writeToFallbackLog(String message, String level, Exception originalException) {
        String fallbackFileName = String.format("%s/fallback_%s.log", LOGS_DIR, currentDate);
        try (FileWriter writer = new FileWriter(fallbackFileName, true)) {
            String fallbackMessage = String.format(
                    "%s [FALLBACK] %s - Original error: %s, Message: %s%s",
                    DateUtil.format(new Date(), "yyyy-MM-dd HH:mm:ss.SSS"),
                    level.toUpperCase(),
                    originalException.getMessage(),
                    message,
                    System.lineSeparator()
            );
            writer.write(fallbackMessage);
            writer.flush();
        } catch (Exception fallbackException) {
            System.err.println("Fatal error: Unable to write to any log file - " + fallbackException.getMessage());
            System.err.println("Original message: " + message);
        }
    }

    @Override
    public String getActiveFileName() {
        ensureLogsDirectoryExists();
        checkAndHandleDateChange();

        String expectedFileName = String.format("%s/log_%s_%d_%d.log", LOGS_DIR, currentDate, startupCount, currentFileIndex);
        File expectedFile = new File(expectedFileName);

        if (!expectedFile.exists()) {
            log.warn("Detected log file {} does not exist, recalculating startup parameters", expectedFileName);
            startupCount = calculateStartupCount();
            currentFileIndex = 1;
            expectedFileName = String.format("%s/log_%s_%d_%d.log", LOGS_DIR, currentDate, startupCount, currentFileIndex);
        } else {
            checkFileSizeAndRollover(expectedFile);
        }

        return expectedFileName;
    }

    @Override
    public void rollover() throws RolloverFailure {
        ensureLogsDirectoryExists();

        String today = DateUtil.format(new Date(), DATE_FORMAT);
        if (!today.equals(currentDate)) {
            log.info("Date change {} -> {}, resetting log file index", currentDate, today);
            currentDate = today;
            startupCount = calculateStartupCount();
            currentFileIndex = 1;
        } else {
            currentFileIndex++;
            currentFileIndex = findNextAvailableFileIndex();
            log.info("Same-day rolling, new file index: {}", currentFileIndex);
        }
    }

    /**
     * 查找下一个可用的文件索引
     *
     * @return 下一个可用的文件索引
     */
    private int findNextAvailableFileIndex() {
        int newIndex = currentFileIndex;
        String baseFileName = String.format("%s/log_%s_%d_", LOGS_DIR, currentDate, startupCount);

        while (true) {
            String fileName = baseFileName + newIndex + ".log";
            File file = new File(fileName);
            if (!file.exists()) {
                break;
            }
            newIndex++;
        }

        return newIndex;
    }

    /**
     * 扫描logs目录计算当日启动次数
     *
     * @return 当日启动次数
     */
    private int calculateStartupCount() {
        File logsDir = new File(LOGS_DIR);
        if (!logsDir.exists() || !logsDir.isDirectory()) {
            log.debug("Logs directory does not exist or is not a directory, returning startup count 1");
            return 1;
        }
    
        String datePrefix = "log_" + currentDate + "_";
        Pattern pattern = Pattern.compile("^log_" + currentDate + "_(\\d+)_(\\d+)\\.log$");
    
        int maxStartupCount = findMaxStartupCount(logsDir, datePrefix, pattern);
        int nextStartupCount = calculateNextStartupCount(maxStartupCount);
    
        log.debug("Calculated today's startup count: {}", nextStartupCount);
        return nextStartupCount;
    }
    
    /**
     * 查找目录中最大的启动次数
     *
     * @param logsDir 日志目录
     * @param datePrefix 日期前缀
     * @param pattern 文件名匹配模式
     * @return 最大启动次数
     */
    private int findMaxStartupCount(File logsDir, String datePrefix, Pattern pattern) {
        int maxStartupCount = 0;
        File[] files = logsDir.listFiles();
            
        if (files != null) {
            for (File file : files) {
                if (file.isFile() && file.getName().startsWith(datePrefix)) {
                    Matcher matcher = pattern.matcher(file.getName());
                    if (matcher.matches()) {
                        int fileStartupCount = Integer.parseInt(matcher.group(1));
                        maxStartupCount = Math.max(maxStartupCount, fileStartupCount);
                    }
                }
            }
        }
            
        return maxStartupCount;
    }
    
    /**
     * 计算下一个启动次数
     *
     * @param maxStartupCount 当前最大启动次数
     * @return 下一个启动次数
     */
    private int calculateNextStartupCount(int maxStartupCount) {
        int nextStartupCount = maxStartupCount + 1;
            
        while (true) {
            String baseFileName = String.format("%s/log_%s_%d_1.log", LOGS_DIR, currentDate, nextStartupCount);
            File testFile = new File(baseFileName);
            if (!testFile.exists()) {
                break;
            }
            nextStartupCount++;
        }
            
        return nextStartupCount;
    }

    /**
     * 获取当前日志系统状态信息
     *
     * @return 状态信息字符串
     */
    public String getStatusInfo() {
        return String.format(
                "Unified log system status - Current date: %s, Startup count: %d, File index: %d, Current file: %s",
                currentDate, startupCount, currentFileIndex,
                String.format("%s/log_%s_%d_%d.log", LOGS_DIR, currentDate, startupCount, currentFileIndex)
        );
    }
}