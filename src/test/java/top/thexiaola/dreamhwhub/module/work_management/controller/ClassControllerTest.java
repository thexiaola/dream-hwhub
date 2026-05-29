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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import top.thexiaola.dreamhwhub.enums.BusinessErrorCode;
import top.thexiaola.dreamhwhub.exception.BusinessException;
import top.thexiaola.dreamhwhub.module.work_management.dto.CreateClassRequest;
import top.thexiaola.dreamhwhub.module.work_management.dto.JoinClassRequest;
import top.thexiaola.dreamhwhub.module.work_management.dto.UpdateClassRequest;
import top.thexiaola.dreamhwhub.module.work_management.entity.ClassInfo;
import top.thexiaola.dreamhwhub.module.work_management.service.ClassService;
import top.thexiaola.dreamhwhub.module.work_management.vo.ClassDetailResponse;
import top.thexiaola.dreamhwhub.module.work_management.vo.CreateClassApplicationResponse;

import java.util.Collections;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 班级管理控制器单元测试
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class ClassControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ClassService classService;

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
     * 测试创建班级申请 - 成功
     */
    @Test
    @DisplayName("测试创建班级申请 - 成功")
    void testApplyCreateClass_Success() throws Exception {
        CreateClassRequest request = new CreateClassRequest();
        request.setClassName("测试班级");
        request.setDescription("这是一个测试班级");

        CreateClassApplicationResponse response = new CreateClassApplicationResponse();
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
        UpdateClassRequest request = new UpdateClassRequest();
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
     * 测试提交加入班级申请 - 成功
     */
    @Test
    @DisplayName("测试提交加入班级申请 - 成功")
    void testApplyJoinClass_Success() throws Exception {
        JoinClassRequest request = new JoinClassRequest();
        request.setClassId(1);

        top.thexiaola.dreamhwhub.module.work_management.vo.JoinClassApplicationResponse response =
                new top.thexiaola.dreamhwhub.module.work_management.vo.JoinClassApplicationResponse();
        response.setId(1);
        response.setClassName("测试班级");

        Mockito.when(classService.submitJoinClassRequest(Mockito.anyInt()))
                .thenReturn(response);

        mockMvc.perform(post("/api/class/1/applications/join")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("加入班级的申请已提交，待审核"));
    }

    /**
     * 测试获取班级成员列表 - 成功
     */
    @Test
    @DisplayName("测试获取班级成员列表 - 成功")
    void testGetClassMembers_Success() throws Exception {
        Page<top.thexiaola.dreamhwhub.module.work_management.vo.ClassMemberResponse> page = new Page<>(1, 10);
        page.setRecords(Collections.emptyList());
        page.setTotal(0);

        Mockito.when(classService.getClassMembers(Mockito.anyInt(), Mockito.anyInt(), Mockito.anyInt()))
                .thenReturn(page);

        mockMvc.perform(get("/api/class/1/members")
                        .param("pageNum", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }



    // ==================== 边界测试 ====================

    /**
     * 边界测试 - 班级名称最大长度64字符
     */
    @Test
    @DisplayName("边界测试 - 班级名称最大长度64字符")
    void testApplyCreateClass_MaxClassNameLength() throws Exception {
        String longName = "班".repeat(64);
        CreateClassRequest request = new CreateClassRequest();
        request.setClassName(longName);
        request.setDescription("测试");

        CreateClassApplicationResponse response = new CreateClassApplicationResponse();
        response.setId(1);

        Mockito.when(classService.submitCreateClassRequest(Mockito.anyString(), Mockito.anyString()))
                .thenReturn(response);

        mockMvc.perform(post("/api/class/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    /**
     * 边界测试 - 班级描述最大长度512字符
     */
    @Test
    @DisplayName("边界测试 - 班级描述最大长度512字符")
    void testApplyCreateClass_MaxDescriptionLength() throws Exception {
        String longDesc = "描".repeat(512);
        CreateClassRequest request = new CreateClassRequest();
        request.setClassName("测试班级");
        request.setDescription(longDesc);

        CreateClassApplicationResponse response = new CreateClassApplicationResponse();
        response.setId(1);

        Mockito.when(classService.submitCreateClassRequest(Mockito.anyString(), Mockito.anyString()))
                .thenReturn(response);

        mockMvc.perform(post("/api/class/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    /**
     * 边界测试 - 分页大小最大值300
     */
    @Test
    @DisplayName("边界测试 - 分页大小最大值300")
    void testGetMyClasses_MaxPageSize() throws Exception {
        Page<ClassDetailResponse> page = new Page<>(1, 300);
        page.setRecords(Collections.emptyList());
        page.setTotal(0);

        Mockito.when(classService.getMyClasses(Mockito.anyInt(), Mockito.anyInt(), Mockito.anyInt()))
                .thenReturn(page);

        mockMvc.perform(get("/api/class/mine")
                        .param("pageNum", "1")
                        .param("pageSize", "300"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    // ==================== 越界数据测试 ====================

    /**
     * 越界数据测试 - 班级名称超过最大长度
     */
    @Test
    @DisplayName("越界数据测试 - 班级名称超过最大长度65字符")
    void testApplyCreateClass_ClassNameTooLong() throws Exception {
        String longName = "班".repeat(65);
        CreateClassRequest request = new CreateClassRequest();
        request.setClassName(longName);
        request.setDescription("测试");

        mockMvc.perform(post("/api/class/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request)))
                .andExpect(status().isBadRequest());
    }

    /**
     * 越界数据测试 - 班级描述超过最大长度
     */
    @Test
    @DisplayName("越界数据测试 - 班级描述超过最大长度513字符")
    void testApplyCreateClass_DescriptionTooLong() throws Exception {
        String longDesc = "描".repeat(513);
        CreateClassRequest request = new CreateClassRequest();
        request.setClassName("测试班级");
        request.setDescription(longDesc);

        mockMvc.perform(post("/api/class/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request)))
                .andExpect(status().isBadRequest());
    }

    /**
     * 越界数据测试 - 分页大小超过最大限制
     */
    @Test
    @DisplayName("越界数据测试 - 分页大小超过最大限制301")
    void testGetMyClasses_PageSizeTooLarge() throws Exception {
        mockMvc.perform(get("/api/class/mine")
                        .param("pageNum", "1")
                        .param("pageSize", "301"))
                .andExpect(status().isBadRequest());
    }

    /**
     * 越界数据测试 - 页码小于1
     */
    @Test
    @DisplayName("越界数据测试 - 页码小于1")
    void testGetMyClasses_PageNumLessThanOne() throws Exception {
        mockMvc.perform(get("/api/class/mine")
                        .param("pageNum", "0")
                        .param("pageSize", "10"))
                .andExpect(status().isBadRequest());
    }

    // ==================== 非法数据测试 ====================

    /**
     * 非法数据测试 - 班级名为空
     */
    @Test
    @DisplayName("非法数据测试 - 班级名为空")
    void testApplyCreateClass_EmptyName() throws Exception {
        CreateClassRequest request = new CreateClassRequest();
        request.setClassName("");
        request.setDescription("描述");

        mockMvc.perform(post("/api/class/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request)))
                .andExpect(status().isBadRequest());
    }

    /**
     * 非法数据测试 - 班级名包含换行符
     */
    @Test
    @DisplayName("非法数据测试 - 班级名包含换行符")
    void testApplyCreateClass_NameWithNewline() throws Exception {
        CreateClassRequest request = new CreateClassRequest();
        request.setClassName("测试\n班级");
        request.setDescription("描述");

        mockMvc.perform(post("/api/class/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request)))
                .andExpect(status().isBadRequest());
    }

    /**
     * 非法数据测试 - 班级名包含制表符
     */
    @Test
    @DisplayName("非法数据测试 - 班级名包含制表符")
    void testApplyCreateClass_NameWithTab() throws Exception {
        CreateClassRequest request = new CreateClassRequest();
        request.setClassName("测试\t班级");
        request.setDescription("描述");

        mockMvc.perform(post("/api/class/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request)))
                .andExpect(status().isBadRequest());
    }

    /**
     * 非法数据测试 - 班级描述包含制表符
     */
    @Test
    @DisplayName("非法数据测试 - 班级描述包含制表符")
    void testApplyCreateClass_DescriptionWithTab() throws Exception {
        CreateClassRequest request = new CreateClassRequest();
        request.setClassName("测试班级");
        request.setDescription("这是\t描述");

        mockMvc.perform(post("/api/class/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request)))
                .andExpect(status().isBadRequest());
    }

    /**
     * 非法数据测试 - 班级ID为负数
     */
    @Test
    @DisplayName("非法数据测试 - 班级ID为负数")
    void testApplyJoinClass_NegativeClassId() throws Exception {
        mockMvc.perform(post("/api/class/-1/applications/join"))
                .andExpect(status().isBadRequest());
    }

    /**
     * 非法数据测试 - 班级不存在
     */
    @Test
    @DisplayName("非法数据测试 - 获取不存在的班级")
    void testGetClassDetail_NotFound() throws Exception {
        Mockito.when(classService.getClassDetail(Mockito.anyInt()))
                .thenThrow(new BusinessException(BusinessErrorCode.CLASS_NOT_FOUND));

        mockMvc.perform(get("/api/class/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value("班级不存在"));
    }

    /**
     * 非法数据测试 - 班级名称包含XSS攻击代码
     */
    @Test
    @DisplayName("非法数据测试 - 班级名称包含XSS攻击代码")
    void testApplyCreateClass_NameWithXss() throws Exception {
        CreateClassRequest request = new CreateClassRequest();
        request.setClassName("<script>alert('xss')</script>");
        request.setDescription("描述");

        mockMvc.perform(post("/api/class/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request)))
                .andExpect(status().isBadRequest());
    }

    /**
     * 非法数据测试 - 不是班级成员无法获取详情
     */
    @Test
    @DisplayName("非法数据测试 - 不是班级成员无法获取详情")
    void testGetClassDetail_NotMember() throws Exception {
        Mockito.when(classService.getClassDetail(Mockito.anyInt()))
                .thenThrow(new BusinessException(BusinessErrorCode.NOT_IN_CLASS));

        mockMvc.perform(get("/api/class/999"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403))
                .andExpect(jsonPath("$.message").value("你不是该班级的成员"));
    }
}