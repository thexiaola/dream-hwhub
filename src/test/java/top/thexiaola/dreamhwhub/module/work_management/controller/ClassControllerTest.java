package top.thexiaola.dreamhwhub.module.work_management.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.ObjectMapper;
import top.thexiaola.dreamhwhub.module.work_management.dto.CreateClassRequest;
import top.thexiaola.dreamhwhub.module.work_management.entity.ClassInfo;
import top.thexiaola.dreamhwhub.module.work_management.service.ClassService;
import top.thexiaola.dreamhwhub.module.work_management.vo.ClassDetailResponse;

import java.util.Collections;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 班级管理控制器单元测试
 */
@SpringBootTest
@ActiveProfiles("test")
class ClassControllerTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @MockitoBean
    private ClassService classService;

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
     * 测试创建班级申请 - 成功
     */
    @Test
    @DisplayName("测试创建班级申请 - 成功")
    void testApplyCreateClass_Success() throws Exception {
        CreateClassRequest request = new CreateClassRequest();
        request.setClassName("测试班级");
        request.setDescription("这是一个测试班级");

        top.thexiaola.dreamhwhub.module.work_management.vo.CreateClassApplicationResponse response = 
            new top.thexiaola.dreamhwhub.module.work_management.vo.CreateClassApplicationResponse();
        response.setId(1);

        Mockito.when(classService.submitCreateClassRequest(Mockito.anyString(), Mockito.anyString()))
                .thenReturn(response);

        mockMvc.perform(post("/api/class/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("创建班级的申请已提交，待审核"));
    }

    /**
     * 测试获取班级详情 - 成功
     */
    @Test
    @DisplayName("测试获取班级详情 - 成功")
    void testGetClassDetail_Success() throws Exception {
        ClassDetailResponse response = new ClassDetailResponse();
        response.setId(1);
        response.setClassName("测试班级");

        Mockito.when(classService.getClassDetail(Mockito.anyInt()))
                .thenReturn(response);

        mockMvc.perform(get("/api/class/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.className").value("测试班级"));
    }

    /**
     * 测试退出班级 - 成功
     */
    @Test
    @DisplayName("测试退出班级 - 成功")
    void testLeaveClass_Success() throws Exception {
        Mockito.when(classService.leaveClass(Mockito.anyInt()))
                .thenReturn("测试班级");

        mockMvc.perform(delete("/api/class/1/members/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("已成功退出\"测试班级\"班级"));
    }

    /**
     * 测试解散班级 - 成功
     */
    @Test
    @DisplayName("测试解散班级 - 成功")
    void testDissolveClass_Success() throws Exception {
        Mockito.doNothing().when(classService).dissolveClass(Mockito.anyInt());

        mockMvc.perform(delete("/api/class/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    /**
     * 测试更新班级信息 - 成功
     */
    @Test
    @DisplayName("测试更新班级信息 - 成功")
    void testUpdateClassInfo_Success() throws Exception {
        top.thexiaola.dreamhwhub.module.work_management.dto.UpdateClassRequest request = 
            new top.thexiaola.dreamhwhub.module.work_management.dto.UpdateClassRequest();
        request.setClassId(1);
        request.setClassName("新班级名");
        request.setDescription("新描述");

        ClassInfo updatedClass = new ClassInfo();
        updatedClass.setId(1);
        updatedClass.setClassName("新班级名");

        Mockito.when(classService.updateClassInfo(Mockito.anyInt(), Mockito.anyString(), Mockito.anyString()))
                .thenReturn(updatedClass);

        mockMvc.perform(put("/api/class/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("班级信息更新成功"));
    }

    /**
     * 测试获取我的班级列表 - 成功
     */
    @Test
    @DisplayName("测试获取我的班级列表 - 成功")
    void testGetMyClasses_Success() throws Exception {
        Page<ClassDetailResponse> page = new Page<>(1, 10);
        page.setRecords(Collections.emptyList());
        page.setTotal(0);

        Mockito.when(classService.getMyClasses(Mockito.anyInt(), Mockito.anyInt(), Mockito.anyInt()))
                .thenReturn(page);

        mockMvc.perform(get("/api/class/mine")
                        .param("pageNum", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    /**
     * 测试生成邀请码 - 成功
     */
    @Test
    @DisplayName("测试生成邀请码 - 成功")
    void testGenerateInviteCode_Success() throws Exception {
        Mockito.when(classService.generateOrRefreshInviteCode(Mockito.anyInt()))
                .thenReturn("ABC123");

        mockMvc.perform(post("/api/class/1/invite-code"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("邀请码生成成功"))
                .andExpect(jsonPath("$.data").value("ABC123"));
    }

    /**
     * 测试通过邀请码加入班级 - 成功
     */
    @Test
    @DisplayName("测试通过邀请码加入班级 - 成功")
    void testJoinByInviteCode_Success() throws Exception {
        top.thexiaola.dreamhwhub.module.work_management.entity.ClassJoinApplication application = 
            new top.thexiaola.dreamhwhub.module.work_management.entity.ClassJoinApplication();
        application.setId(1);

        Mockito.when(classService.joinClassByInviteCode(Mockito.anyString()))
                .thenReturn(application);

        mockMvc.perform(post("/api/class/join-by-code")
                        .param("inviteCode", "ABC123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("加入申请已提交，待审核"));
    }

    /**
     * 测试转让班级所有权 - 成功
     */
    @Test
    @DisplayName("测试转让班级所有权 - 成功")
    void testTransferOwnership_Success() throws Exception {
        Mockito.doNothing().when(classService).transferClassOwnership(Mockito.anyInt(), Mockito.anyInt());

        mockMvc.perform(put("/api/class/1/owner")
                        .param("newOwnerId", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("班级所有权转让成功"));
    }

    /**
     * 测试创建班级 - 班级名为空
     */
    @Test
    @DisplayName("测试创建班级 - 班级名为空")
    void testApplyCreateClass_EmptyName() throws Exception {
        CreateClassRequest request = new CreateClassRequest();
        request.setClassName("");
        request.setDescription("描述");

        mockMvc.perform(post("/api/class/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request)))
                .andExpect(status().isBadRequest());
    }
}