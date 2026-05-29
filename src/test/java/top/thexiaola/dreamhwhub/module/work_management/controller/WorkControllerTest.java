package top.thexiaola.dreamhwhub.module.work_management.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import top.thexiaola.dreamhwhub.enums.BusinessErrorCode;
import top.thexiaola.dreamhwhub.exception.BusinessException;
import top.thexiaola.dreamhwhub.module.work_management.dto.CreateWorkRequest;
import top.thexiaola.dreamhwhub.module.work_management.dto.PinWorkRequest;
import top.thexiaola.dreamhwhub.module.work_management.entity.WorkInfo;
import top.thexiaola.dreamhwhub.module.work_management.service.WorkService;
import top.thexiaola.dreamhwhub.module.work_management.vo.WorkResponse;

import java.time.LocalDateTime;
import java.util.Collections;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 作业管理控制器单元测试
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class WorkControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private WorkService workService;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
    }

    private String toJson(Object obj) throws Exception {
        return objectMapper.writeValueAsString(obj);
    }

    // ==================== 正常数据测试 ====================

    /**
     * 测试创建作业 - 成功
     */
    @Test
    @DisplayName("测试创建作业 - 成功")
    void testCreateWork_Success() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "attachments",
                "test.pdf",
                "application/pdf",
                "test content".getBytes()
        );

        WorkInfo workInfo = new WorkInfo();
        workInfo.setId(1);
        workInfo.setTitle("测试作业");

        Mockito.when(workService.createWork(Mockito.any(CreateWorkRequest.class)))
                .thenReturn(workInfo);

        mockMvc.perform(multipart("/api/works/")
                        .file(file)
                        .param("title", "测试作业")
                        .param("description", "测试作业描述")
                        .param("classId", "1")
                        .param("deadline", LocalDateTime.now().plusDays(7).toString())
                        .param("publishTime", LocalDateTime.now().toString())
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    /**
     * 测试查询作业详情 - 成功
     */
    @Test
    @DisplayName("测试查询作业详情 - 成功")
    void testGetWorkDetail_Success() throws Exception {
        WorkInfo workInfo = new WorkInfo();
        workInfo.setId(1);
        workInfo.setTitle("测试作业");

        Mockito.when(workService.getWorkById(Mockito.anyInt()))
                .thenReturn(workInfo);

        mockMvc.perform(get("/api/works/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.title").value("测试作业"));
    }

    /**
     * 测试删除作业 - 成功
     */
    @Test
    @DisplayName("测试删除作业 - 成功")
    void testDeleteWork_Success() throws Exception {
        Mockito.doNothing().when(workService).deleteWork(Mockito.anyInt());

        mockMvc.perform(delete("/api/works/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    /**
     * 测试查询作业列表 - 成功
     */
    @Test
    @DisplayName("测试查询作业列表 - 成功")
    void testGetWorkList_Success() throws Exception {
        Page<WorkResponse> page = new Page<>(1, 10);
        page.setRecords(Collections.emptyList());
        page.setTotal(0);

        Mockito.when(workService.getWorkList(Mockito.any(), Mockito.any(), Mockito.anyInt(), Mockito.anyInt()))
                .thenReturn(page);

        mockMvc.perform(get("/api/works/")
                        .param("pageNum", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    /**
     * 测试置顶作业 - 成功
     */
    @Test
    @DisplayName("测试置顶作业 - 成功")
    void testPinWork_Success() throws Exception {
        PinWorkRequest request = new PinWorkRequest();
        request.setWorkId(1);
        request.setIsPinned(true);

        WorkInfo workInfo = new WorkInfo();
        workInfo.setId(1);
        workInfo.setIsPinned(true);

        Mockito.when(workService.pinWork(Mockito.anyInt(), Mockito.anyBoolean()))
                .thenReturn(workInfo);

        mockMvc.perform(patch("/api/works/1/pin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    /**
     * 测试取消置顶作业 - 成功
     */
    @Test
    @DisplayName("测试取消置顶作业 - 成功")
    void testUnpinWork_Success() throws Exception {
        PinWorkRequest request = new PinWorkRequest();
        request.setWorkId(1);
        request.setIsPinned(false);

        WorkInfo workInfo = new WorkInfo();
        workInfo.setId(1);
        workInfo.setIsPinned(false);

        Mockito.when(workService.pinWork(Mockito.anyInt(), Mockito.anyBoolean()))
                .thenReturn(workInfo);

        mockMvc.perform(patch("/api/works/1/pin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    /**
     * 测试更新作业 - 成功
     */
    @Test
    @DisplayName("测试更新作业 - 成功")
    void testUpdateWork_Success() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "attachments",
                "test.pdf",
                "application/pdf",
                "test content".getBytes()
        );

        WorkInfo workInfo = new WorkInfo();
        workInfo.setId(1);
        workInfo.setTitle("更新后的作业");

        Mockito.when(workService.updateWork(Mockito.any()))
                .thenReturn(workInfo);

        mockMvc.perform(multipart("/api/works/1")
                        .file(file)
                        .param("id", "1")
                        .param("title", "更新后的作业")
                        .param("description", "更新后的作业描述")
                        .param("deadline", LocalDateTime.now().plusDays(7).toString())
                        .param("totalScore", "100")
                        .with(request -> {
                            request.setMethod("PUT");
                            return request;
                        })
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    // ==================== 边界测试 ====================

    /**
     * 边界测试 - 作业标题最大长度128字符
     */
    @Test
    @DisplayName("边界测试 - 作业标题最大长度128字符")
    void testCreateWork_MaxTitleLength() throws Exception {
        String longTitle = "a".repeat(128);
        
        WorkInfo workInfo = new WorkInfo();
        workInfo.setId(1);
        workInfo.setTitle(longTitle);

        Mockito.when(workService.createWork(Mockito.any(CreateWorkRequest.class)))
                .thenReturn(workInfo);

        mockMvc.perform(multipart("/api/works/")
                        .param("title", longTitle)
                        .param("description", "测试")
                        .param("classId", "1")
                        .param("deadline", LocalDateTime.now().plusDays(7).toString())
                        .param("publishTime", LocalDateTime.now().toString())
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    /**
     * 边界测试 - 作业总分最小值1
     */
    @Test
    @DisplayName("边界测试 - 作业总分最小值1")
    void testCreateWork_MinTotalScore() throws Exception {
        WorkInfo workInfo = new WorkInfo();
        workInfo.setId(1);
        workInfo.setTitle("测试作业");

        Mockito.when(workService.createWork(Mockito.any(CreateWorkRequest.class)))
                .thenReturn(workInfo);

        mockMvc.perform(multipart("/api/works/")
                        .param("title", "测试作业")
                        .param("description", "测试")
                        .param("classId", "1")
                        .param("deadline", LocalDateTime.now().plusDays(7).toString())
                        .param("totalScore", "1")
                        .param("publishTime", LocalDateTime.now().toString())
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    /**
     * 边界测试 - 作业总分最大值1000
     */
    @Test
    @DisplayName("边界测试 - 作业总分最大值1000")
    void testCreateWork_MaxTotalScore() throws Exception {
        WorkInfo workInfo = new WorkInfo();
        workInfo.setId(1);
        workInfo.setTitle("测试作业");

        Mockito.when(workService.createWork(Mockito.any(CreateWorkRequest.class)))
                .thenReturn(workInfo);

        mockMvc.perform(multipart("/api/works/")
                        .param("title", "测试作业")
                        .param("description", "测试")
                        .param("classId", "1")
                        .param("deadline", LocalDateTime.now().plusDays(7).toString())
                        .param("totalScore", "1000")
                        .param("publishTime", LocalDateTime.now().toString())
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    // ==================== 越界数据测试 ====================

    /**
     * 越界数据测试 - 作业标题超过最大长度
     */
    @Test
    @DisplayName("越界数据测试 - 作业标题超过最大长度129字符")
    void testCreateWork_TitleTooLong() throws Exception {
        String longTitle = "a".repeat(129);

        mockMvc.perform(multipart("/api/works/")
                        .param("title", longTitle)
                        .param("description", "测试")
                        .param("classId", "1")
                        .param("deadline", LocalDateTime.now().plusDays(7).toString())
                        .param("publishTime", LocalDateTime.now().toString())
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isBadRequest());
    }

    /**
     * 越界数据测试 - 作业总分超过最大值
     */
    @Test
    @DisplayName("越界数据测试 - 作业总分超过最大值1001")
    void testCreateWork_TotalScoreTooHigh() throws Exception {
        mockMvc.perform(multipart("/api/works/")
                        .param("title", "测试作业")
                        .param("description", "测试")
                        .param("classId", "1")
                        .param("deadline", LocalDateTime.now().plusDays(7).toString())
                        .param("totalScore", "1001")
                        .param("publishTime", LocalDateTime.now().toString())
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isBadRequest());
    }

    /**
     * 越界数据测试 - 作业总分小于最小值
     */
    @Test
    @DisplayName("越界数据测试 - 作业总分小于最小值0")
    void testCreateWork_TotalScoreTooLow() throws Exception {
        mockMvc.perform(multipart("/api/works/")
                        .param("title", "测试作业")
                        .param("description", "测试")
                        .param("classId", "1")
                        .param("deadline", LocalDateTime.now().plusDays(7).toString())
                        .param("totalScore", "0")
                        .param("publishTime", LocalDateTime.now().toString())
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isBadRequest());
    }

    /**
     * 越界数据测试 - 分页大小超过最大限制
     */
    @Test
    @DisplayName("越界数据测试 - 分页大小超过最大限制301")
    void testGetWorkList_PageSizeTooLarge() throws Exception {
        mockMvc.perform(get("/api/works/")
                        .param("pageNum", "1")
                        .param("pageSize", "301"))
                .andExpect(status().isBadRequest());
    }

    // ==================== 非法数据测试 ====================

    /**
     * 非法数据测试 - 作业标题为空
     */
    @Test
    @DisplayName("非法数据测试 - 作业标题为空")
    void testCreateWork_EmptyTitle() throws Exception {
        mockMvc.perform(multipart("/api/works/")
                        .param("title", "")
                        .param("description", "测试")
                        .param("classId", "1")
                        .param("deadline", LocalDateTime.now().plusDays(7).toString())
                        .param("publishTime", LocalDateTime.now().toString())
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isBadRequest());
    }

    /**
     * 非法数据测试 - 作业标题包含换行符
     */
    @Test
    @DisplayName("非法数据测试 - 作业标题包含换行符")
    void testCreateWork_TitleWithNewline() throws Exception {
        mockMvc.perform(multipart("/api/works/")
                        .param("title", "测试\n作业")
                        .param("description", "测试")
                        .param("classId", "1")
                        .param("deadline", LocalDateTime.now().plusDays(7).toString())
                        .param("publishTime", LocalDateTime.now().toString())
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isBadRequest());
    }

    /**
     * 非法数据测试 - 作业标题包含制表符
     */
    @Test
    @DisplayName("非法数据测试 - 作业标题包含制表符")
    void testCreateWork_TitleWithTab() throws Exception {
        mockMvc.perform(multipart("/api/works/")
                        .param("title", "测试\t作业")
                        .param("description", "测试")
                        .param("classId", "1")
                        .param("deadline", LocalDateTime.now().plusDays(7).toString())
                        .param("publishTime", LocalDateTime.now().toString())
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isBadRequest());
    }

    /**
     * 非法数据测试 - 班级ID为空
     */
    @Test
    @DisplayName("非法数据测试 - 班级ID为空")
    void testCreateWork_EmptyClassId() throws Exception {
        mockMvc.perform(multipart("/api/works/")
                        .param("title", "测试作业")
                        .param("description", "测试")
                        .param("classId", "")
                        .param("deadline", LocalDateTime.now().plusDays(7).toString())
                        .param("publishTime", LocalDateTime.now().toString())
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isBadRequest());
    }

    /**
     * 非法数据测试 - 班级ID为负数
     */
    @Test
    @DisplayName("非法数据测试 - 班级ID为负数")
    void testCreateWork_NegativeClassId() throws Exception {
        mockMvc.perform(multipart("/api/works/")
                        .param("title", "测试作业")
                        .param("description", "测试")
                        .param("classId", "-1")
                        .param("deadline", LocalDateTime.now().plusDays(7).toString())
                        .param("publishTime", LocalDateTime.now().toString())
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isBadRequest());
    }

    /**
     * 非法数据测试 - 截止时间格式错误
     */
    @Test
    @DisplayName("非法数据测试 - 截止时间格式错误")
    void testCreateWork_InvalidDeadlineFormat() throws Exception {
        mockMvc.perform(multipart("/api/works/")
                        .param("title", "测试作业")
                        .param("description", "测试")
                        .param("classId", "1")
                        .param("deadline", "2026/04/15")
                        .param("publishTime", LocalDateTime.now().toString())
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isBadRequest());
    }

    /**
     * 非法数据测试 - 作业不存在
     */
    @Test
    @DisplayName("非法数据测试 - 查询不存在的作业")
    void testGetWorkDetail_NotFound() throws Exception {
        Mockito.when(workService.getWorkById(Mockito.anyInt()))
                .thenThrow(new BusinessException(BusinessErrorCode.WORK_NOT_FOUND));

        mockMvc.perform(get("/api/works/999"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("作业不存在"));
    }

    /**
     * 非法数据测试 - 发布时间早于当前时间
     */
    @Test
    @DisplayName("非法数据测试 - 发布时间早于当前时间")
    void testCreateWork_PublishTimeInPast() throws Exception {
        mockMvc.perform(multipart("/api/works/")
                        .param("title", "测试作业")
                        .param("description", "测试")
                        .param("classId", "1")
                        .param("deadline", LocalDateTime.now().plusDays(7).toString())
                        .param("publishTime", LocalDateTime.now().minusDays(1).toString())
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isBadRequest());
    }

    /**
     * 非法数据测试 - 截止时间早于发布时间
     */
    @Test
    @DisplayName("非法数据测试 - 截止时间早于发布时间")
    void testCreateWork_DeadlineBeforePublishTime() throws Exception {
        LocalDateTime now = LocalDateTime.now();
        mockMvc.perform(multipart("/api/works/")
                        .param("title", "测试作业")
                        .param("description", "测试")
                        .param("classId", "1")
                        .param("deadline", now.minusDays(1).toString())
                        .param("publishTime", now.plusDays(1).toString())
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isBadRequest());
    }

    /**
     * 非法数据测试 - 作业标题包含XSS攻击代码
     */
    @Test
    @DisplayName("非法数据测试 - 作业标题包含XSS攻击代码")
    void testCreateWork_TitleWithXss() throws Exception {
        mockMvc.perform(multipart("/api/works/")
                        .param("title", "<script>alert('xss')</script>")
                        .param("description", "测试")
                        .param("classId", "1")
                        .param("deadline", LocalDateTime.now().plusDays(7).toString())
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isBadRequest());
    }
}