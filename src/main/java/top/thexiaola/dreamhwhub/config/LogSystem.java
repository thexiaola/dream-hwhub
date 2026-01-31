package top.thexiaola.dreamhwhub.config;

import ch.qos.logback.core.rolling.RollingPolicyBase;
import ch.qos.logback.core.rolling.RolloverFailure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 日志系统
 * 格式: log_{日期}_{启动次数}_{文件序号}.log
 */
@Component
public class LogSystem extends RollingPolicyBase {

    private static final Logger logger = LoggerFactory.getLogger(LogSystem.class);

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
        currentDate = new SimpleDateFormat(DATE_FORMAT).format(new Date());
        startupCount = calculateStartupCount();
        super.start();
        logger.info("统一日志系统初始化完成");
    }

    @Override
    public void stop() {
        currentDate = null;
        currentFileIndex = 1;
        startupCount = 1;
        super.stop();
        logger.info("日志滚动策略已停止，资源释放完成");
    }

    // 确保日志目录存在
    private void ensureLogsDirectoryExists() {
        File logsDir = new File(LOGS_DIR);
        if (!logsDir.exists()) {
            boolean created = logsDir.mkdirs();
            if (created) {
                logger.info("创建日志目录: {}", LOGS_DIR);
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
            logger.debug("文件 {} 超过大小限制，准备滚动到下一个文件", file.getName());
        }
    }

    /**
     * 检查并处理日期变化
     */
    private void checkAndHandleDateChange() {
        String today = new SimpleDateFormat(DATE_FORMAT).format(new Date());
        if (!today.equals(currentDate)) {
            logger.info("检测到日期变化: {} -> {}，自动切换日志文件", currentDate, today);
            currentDate = today;
            startupCount = calculateStartupCount();
            currentFileIndex = 1;
            logger.info("日期切换完成，新启动次数: {}, 文件序号重置为: {}", startupCount, currentFileIndex);
        }
    }

    /**
     * 格式化日志消息
     */
    private String formatLogMessage(String message, String level) {
        return String.format("%s [%s] %s - %s",
                new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date()),
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
                    "%s [FALLBACK] %s - 原始错误: %s, 消息: %s%s",
                    new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date()),
                    level.toUpperCase(),
                    originalException.getMessage(),
                    message,
                    System.lineSeparator()
            );
            writer.write(fallbackMessage);
            writer.flush();
        } catch (Exception fallbackException) {
            System.err.println("致命错误：无法写入任何日志文件 - " + fallbackException.getMessage());
            System.err.println("原始消息: " + message);
        }
    }

    @Override
    public String getActiveFileName() {
        ensureLogsDirectoryExists();
        checkAndHandleDateChange();

        String expectedFileName = String.format("%s/log_%s_%d_%d.log", LOGS_DIR, currentDate, startupCount, currentFileIndex);
        File expectedFile = new File(expectedFileName);

        if (!expectedFile.exists()) {
            logger.warn("检测到日志文件 {} 不存在，重新计算启动参数", expectedFileName);
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

        String today = new SimpleDateFormat(DATE_FORMAT).format(new Date());
        if (!today.equals(currentDate)) {
            logger.info("日期变更 {} -> {}，重置日志文件序号", currentDate, today);
            currentDate = today;
            startupCount = calculateStartupCount();
            currentFileIndex = 1;
        } else {
            currentFileIndex++;
            currentFileIndex = findNextAvailableFileIndex();
            logger.info("同日内滚动，新文件序号: {}", currentFileIndex);
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
            logger.debug("日志目录不存在或不是目录，返回启动次数1");
            return 1;
        }

        String datePrefix = "log_" + currentDate + "_";
        Pattern pattern = Pattern.compile("^log_" + currentDate + "_(\\d+)_(\\d+)\\.log$");

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

        int nextStartupCount = maxStartupCount + 1;
        while (true) {
            String baseFileName = String.format("%s/log_%s_%d_1.log", LOGS_DIR, currentDate, nextStartupCount);
            File testFile = new File(baseFileName);
            if (!testFile.exists()) {
                break;
            }
            nextStartupCount++;
        }

        logger.debug("计算得到当日启动次数: {}", nextStartupCount);
        return nextStartupCount;
    }

    /**
     * 获取当前日志系统状态信息
     *
     * @return 状态信息字符串
     */
    public String getStatusInfo() {
        return String.format(
                "统一日志系统状态 - 当前日期: %s, 启动次数: %d, 文件序号: %d, 当前文件: %s",
                currentDate, startupCount, currentFileIndex,
                String.format("%s/log_%s_%d_%d.log", LOGS_DIR, currentDate, startupCount, currentFileIndex)
        );
    }
}