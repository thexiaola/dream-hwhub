package top.thexiaola.dreamhwhub.module.work_management.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.ObjectMapper;
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
@ActiveProfiles("test")
class WorkControllerTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @MockitoBean
    private WorkService workService;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        objectMapper = new ObjectMapper();
    }

    private String toJson(Object obj) throws Exception {
        return objectMapper.writeValueAsString(obj);
    }

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
                        .param("classId", "1")
                        .param("deadline", LocalDateTime.now().plusDays(7).toString())
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
                        .param("title", "更新后的作业")
                        .with(request -> {
                            request.setMethod("PUT");
                            return request;
                        })
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    /**
     * 测试查询作业详情 - 作业不存在
     */
    @Test
    @DisplayName("测试查询作业详情 - 作业不存在")
    void testGetWorkDetail_NotFound() throws Exception {
        Mockito.when(workService.getWorkById(Mockito.anyInt()))
                .thenThrow(new BusinessException(BusinessErrorCode.WORK_NOT_FOUND));

        mockMvc.perform(get("/api/works/999"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("作业不存在"));
    }
}