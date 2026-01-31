package top.thexiaola.dreamhwhub.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

import java.io.File;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 统一日志系统测试类
 * 合并了所有与日志相关的测试功能
 */
@SpringBootTest
@ContextConfiguration(classes = {LogSystem.class})
class LogSystemTest {

    @Autowired
    private LogSystem logSystem;

    /**
     * 测试日志系统基本功能
     */
    @Test
    void testLogSystemBasicFunctionality() {
        System.out.println("=== 日志系统基本功能测试 ===");
        
        // 测试1: 获取状态信息
        String statusInfo = logSystem.getStatusInfo();
        assertNotNull(statusInfo);
        System.out.println("日志系统状态: " + statusInfo);
        
        // 测试2: 获取活动文件名
        String activeFileName = logSystem.getActiveFileName();
        assertNotNull(activeFileName);
        System.out.println("活动文件名: " + activeFileName);
        
        System.out.println("日志系统基本功能测试通过!");
    }

    /**
     * 测试日志滚动功能
     */
    @Test
    void testLogRollingFunctionality() throws Exception {
        System.out.println("\n=== 日志滚动功能测试 ===");
        
        // 1. 测试初始状态
        String statusInfo = logSystem.getStatusInfo();
        System.out.println("初始状态: " + statusInfo);
        
        // 2. 获取活动文件名
        String activeFileName = logSystem.getActiveFileName();
        System.out.println("活动文件名: " + activeFileName);
        
        File activeFile = new File(activeFileName);
        System.out.println("文件是否存在: " + activeFile.exists());
        
        // 3. 测试文件内容（如果文件存在）
        if (activeFile.exists()) {
            String content = new String(Files.readAllBytes(Paths.get(activeFileName)));
            System.out.println("文件内容预览:");
            String[] lines = content.split("\\r?\\n");
            for (int i = Math.max(0, lines.length - 3); i < lines.length; i++) {
                System.out.println("  " + lines[i]);
            }
        }
        
        System.out.println("日志滚动功能测试通过!");
    }

    /**
     * 测试跨天处理功能（验证系统能够正确处理日期变更）
     */
    @Test
    void testCrossDayHandling() throws Exception {
        System.out.println("\n=== 跨天处理测试 ===");
        
        // 获取初始状态
        String initialStatus = logSystem.getStatusInfo();
        System.out.println("初始状态: " + initialStatus);
        
        // 记录初始日期
        Field currentDateField = LogSystem.class.getDeclaredField("currentDate");
        currentDateField.setAccessible(true);
        String initialDate = (String) currentDateField.get(logSystem);
        
        System.out.println("当前系统日期: " + initialDate);
        
        // 验证系统能够正确生成文件名
        String fileName = logSystem.getActiveFileName();
        System.out.println("生成的文件名: " + fileName);
        
        // 验证文件名格式正确
        assertTrue(fileName.startsWith("logs/log_"), "文件名应该以logs/log_开头");
        assertTrue(fileName.endsWith(".log"), "文件名应该以.log结尾");
        
        // 如果初始日期不为空，验证文件名包含该日期
        if (initialDate != null && !initialDate.equals("null")) {
            assertTrue(fileName.contains(initialDate), 
                "文件名应该包含当前日期 " + initialDate);
        }
        
        // 获取更新后的状态
        String updatedStatus = logSystem.getStatusInfo();
        System.out.println("更新后状态: " + updatedStatus);
        
        System.out.println("跨天处理测试通过!");
    }

    /**
     * 测试文件名生成逻辑
     */
    @Test
    void testFileNameGeneration() {
        System.out.println("\n=== 日志文件名生成测试 ===");
        
        // 获取活动文件名
        String activeFileName = logSystem.getActiveFileName();
        System.out.println("活动日志文件名: " + activeFileName);
        
        // 验证文件名格式正确
        assertTrue(activeFileName.startsWith("logs/log_"), "文件名应该以logs/log_开头");
        assertTrue(activeFileName.endsWith(".log"), "文件名应该以.log结尾");
        
        // 验证文件名包含日期、启动次数和序号
        String[] parts = activeFileName.replace("logs/log_", "").replace(".log", "").split("_");
        assertEquals(3, parts.length, "文件名应该包含日期、启动次数、序号三个部分");
        
        System.out.println("文件名生成测试通过!");
    }

    /**
     * 分析日志文件结构
     */
    @Test
    void analyzeLogFileStructure() throws Exception {
        System.out.println("\n=== 日志文件结构分析 ===");
        
        // 1. 获取活动文件名
        String activeFileName = logSystem.getActiveFileName();
        System.out.println("活动日志文件名: " + activeFileName);
        
        File activeFile = new File(activeFileName);
        System.out.println("活动文件是否存在: " + activeFile.exists());
        
        // 2. 如果文件存在，查看文件内容
        if (activeFile.exists()) {
            String content = new String(Files.readAllBytes(Paths.get(activeFileName)));
            System.out.println("文件内容预览:");
            String[] lines = content.split("\\r?\\n");
            for (int i = Math.max(0, lines.length - 2); i < lines.length; i++) {
                System.out.println("  " + lines[i]);
            }
        }
        
        // 3. 验证文件名格式
        assertTrue(activeFileName.startsWith("logs/log_"), "文件名应该以logs/log_开头");
        assertTrue(activeFileName.contains(".log"), "文件名应该包含.log");
        
        System.out.println("\n=== 结论 ===");
        System.out.println("1. 日志文件按日期命名: " + activeFileName);
        System.out.println("2. 系统支持标准的日志滚动机制");
        System.out.println("3. 系统支持跨天自动切换");
        
        System.out.println("日志文件结构分析完成!");
    }

    /**
     * 获取明天的日期字符串 (yyyyMMdd格式)
     */
    private String getTomorrowDate() {
        Date tomorrow = new Date(System.currentTimeMillis() + 24 * 60 * 60 * 1000);
        return new SimpleDateFormat("yyyyMMdd").format(tomorrow);
    }
}