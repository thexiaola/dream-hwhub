package top.thexiaola.dreamhwhub.module.login.controller;

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
import top.thexiaola.dreamhwhub.module.login.dto.RetrievePasswordCodeRequest;
import top.thexiaola.dreamhwhub.module.login.dto.RetrievePasswordModifyRequest;
import top.thexiaola.dreamhwhub.module.login.entity.User;
import top.thexiaola.dreamhwhub.module.login.service.ModifyUserService;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 密码找回控制器单元测试
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class RetrieveUserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ModifyUserService modifyUserService;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
    }

    private String toJson(Object obj) throws Exception {
        return objectMapper.writeValueAsString(obj);
    }

    /**
     * 测试发送找回密码验证码 - 成功
     */
    @Test
    @DisplayName("测试发送找回密码验证码 - 成功")
    void testSendRetrievePasswordCode_Success() throws Exception {
        RetrievePasswordCodeRequest request = new RetrievePasswordCodeRequest();
        request.setAccount("testuser");

        User mockUser = new User();
        mockUser.setId(1);
        mockUser.setUsername("testuser");

        Mockito.when(modifyUserService.sendRetrievePasswordCode(Mockito.anyString()))
                .thenReturn(mockUser);

        mockMvc.perform(post("/api/users/retrieve/sendcode")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("验证码已发送"));
    }

    /**
     * 测试找回密码 - 成功
     */
    @Test
    @DisplayName("测试找回密码 - 成功")
    void testRetrievePassword_Success() throws Exception {
        RetrievePasswordModifyRequest request = new RetrievePasswordModifyRequest();
        request.setAccount("testuser");
        request.setCode("123456");
        request.setNewPassword("newpass123");

        User mockUser = new User();
        mockUser.setId(1);
        mockUser.setUsername("testuser");

        Mockito.when(modifyUserService.retrievePassword(Mockito.any(RetrievePasswordModifyRequest.class)))
                .thenReturn(mockUser);

        mockMvc.perform(put("/api/users/retrieve/resetpassword")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("密码重置成功"));
    }

    /**
     * 测试发送验证码 - 账号不存在
     */
    @Test
    @DisplayName("测试发送验证码 - 账号不存在")
    void testSendCode_UserNotFound() throws Exception {
        RetrievePasswordCodeRequest request = new RetrievePasswordCodeRequest();
        request.setAccount("nonexistent");

        Mockito.when(modifyUserService.sendRetrievePasswordCode(Mockito.anyString()))
                .thenThrow(new BusinessException(BusinessErrorCode.USER_NOT_FOUND));

        mockMvc.perform(post("/api/users/retrieve/sendcode")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("用户不存在"));
    }

    /**
     * 测试找回密码 - 验证码错误
     */
    @Test
    @DisplayName("测试找回密码 - 验证码错误")
    void testRetrievePassword_InvalidCode() throws Exception {
        RetrievePasswordModifyRequest request = new RetrievePasswordModifyRequest();
        request.setAccount("testuser");
        request.setCode("123456");
        request.setNewPassword("newpass123");

        Mockito.when(modifyUserService.retrievePassword(Mockito.any(RetrievePasswordModifyRequest.class)))
                .thenThrow(new BusinessException(BusinessErrorCode.VERIFICATION_CODE_INVALID));

        mockMvc.perform(put("/api/users/retrieve/resetpassword")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("验证码无效"));
    }

    /**
     * 测试发送验证码 - 账号为空
     */
    @Test
    @DisplayName("测试发送验证码 - 账号为空")
    void testSendCode_AccountEmpty() throws Exception {
        RetrievePasswordCodeRequest request = new RetrievePasswordCodeRequest();
        request.setAccount("");

        mockMvc.perform(post("/api/users/retrieve/sendcode")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request)))
                .andExpect(status().isBadRequest());
    }

    /**
     * 测试找回密码 - 新密码过短
     */
    @Test
    @DisplayName("测试找回密码 - 新密码长度不足")
    void testRetrievePassword_PasswordTooShort() throws Exception {
        RetrievePasswordModifyRequest request = new RetrievePasswordModifyRequest();
        request.setAccount("testuser");
        request.setCode("123456");
        request.setNewPassword("12345"); // 少于 6 位

        mockMvc.perform(put("/api/users/retrieve/resetpassword")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request)))
                .andExpect(status().isBadRequest());
    }

    // ==================== 边界测试 ====================

    /**
     * 边界测试 - 使用学号发送验证码
     */
    @Test
    @DisplayName("边界测试 - 使用学号发送验证码")
    void testSendCode_WithUserNo() throws Exception {
        RetrievePasswordCodeRequest request = new RetrievePasswordCodeRequest();
        request.setAccount("2024001"); // 学号

        User mockUser = new User();
        mockUser.setId(1);
        mockUser.setUserNo("2024001");

        Mockito.when(modifyUserService.sendRetrievePasswordCode(Mockito.anyString()))
                .thenReturn(mockUser);

        mockMvc.perform(post("/api/users/retrieve/sendcode")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    /**
     * 边界测试 - 使用邮箱发送验证码
     */
    @Test
    @DisplayName("边界测试 - 使用邮箱发送验证码")
    void testSendCode_WithEmail() throws Exception {
        RetrievePasswordCodeRequest request = new RetrievePasswordCodeRequest();
        request.setAccount("test@example.com"); // 邮箱

        User mockUser = new User();
        mockUser.setId(1);
        mockUser.setEmail("test@example.com");

        Mockito.when(modifyUserService.sendRetrievePasswordCode(Mockito.anyString()))
                .thenReturn(mockUser);

        mockMvc.perform(post("/api/users/retrieve/sendcode")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    /**
     * 边界测试 - 新密码最小长度
     */
    @Test
    @DisplayName("边界测试 - 新密码最小长度6个字符")
    void testRetrievePassword_MinPasswordLength() throws Exception {
        RetrievePasswordModifyRequest request = new RetrievePasswordModifyRequest();
        request.setAccount("testuser");
        request.setCode("123456");
        request.setNewPassword("abc123");

        User mockUser = new User();
        mockUser.setId(1);
        mockUser.setUsername("testuser");

        Mockito.when(modifyUserService.retrievePassword(Mockito.any(RetrievePasswordModifyRequest.class)))
                .thenReturn(mockUser);

        mockMvc.perform(put("/api/users/retrieve/resetpassword")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    /**
     * 边界测试 - 新密码最大长度
     */
    @Test
    @DisplayName("边界测试 - 新密码最大长度48个字符")
    void testRetrievePassword_MaxPasswordLength() throws Exception {
        String longPassword = "a".repeat(48);
        RetrievePasswordModifyRequest request = new RetrievePasswordModifyRequest();
        request.setAccount("testuser");
        request.setCode("123456");
        request.setNewPassword(longPassword);

        User mockUser = new User();
        mockUser.setId(1);
        mockUser.setUsername("testuser");

        Mockito.when(modifyUserService.retrievePassword(Mockito.any(RetrievePasswordModifyRequest.class)))
                .thenReturn(mockUser);

        mockMvc.perform(put("/api/users/retrieve/resetpassword")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    /**
     * 边界测试 - 验证码最小长度
     */
    @Test
    @DisplayName("边界测试 - 验证码最小长度6位")
    void testRetrievePassword_MinCodeLength() throws Exception {
        RetrievePasswordModifyRequest request = new RetrievePasswordModifyRequest();
        request.setAccount("testuser");
        request.setCode("12345"); // 5位，不符合要求
        request.setNewPassword("newpass123");

        mockMvc.perform(put("/api/users/retrieve/resetpassword")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request)))
                .andExpect(status().isBadRequest());
    }

    // ==================== 非法数据测试 ====================

    /**
     * 非法数据测试 - 账号包含SQL注入
     */
    @Test
    @DisplayName("非法数据测试 - 账号包含SQL注入")
    void testSendCode_SqlInjection() throws Exception {
        RetrievePasswordCodeRequest request = new RetrievePasswordCodeRequest();
        request.setAccount("' OR '1'='1");

        Mockito.when(modifyUserService.sendRetrievePasswordCode(Mockito.anyString()))
                .thenThrow(new BusinessException(BusinessErrorCode.USER_NOT_FOUND));

        mockMvc.perform(post("/api/users/retrieve/sendcode")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request)))
                .andExpect(status().isBadRequest());
    }

    /**
     * 非法数据测试 - 账号包含XSS
     * (当前验证不阻止HTML标签，会正常处理)
     */
    @Test
    @DisplayName("非法数据测试 - 账号包含XSS")
    void testSendCode_XssInjection() throws Exception {
        RetrievePasswordCodeRequest request = new RetrievePasswordCodeRequest();
        request.setAccount("<script>alert(1)</script>");

        User mockUser = new User();
        mockUser.setId(1);
        mockUser.setUsername("<script>alert(1)</script>");

        Mockito.when(modifyUserService.sendRetrievePasswordCode(Mockito.anyString()))
                .thenReturn(mockUser);

        mockMvc.perform(post("/api/users/retrieve/sendcode")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    /**
     * 非法数据测试 - 账号包含特殊Unicode字符
     * (当前验证不阻止特殊字符，会正常处理)
     */
    @Test
    @DisplayName("非法数据测试 - 账号包含null字符")
    void testSendCode_NullCharacter() throws Exception {
        RetrievePasswordCodeRequest request = new RetrievePasswordCodeRequest();
        request.setAccount("test\u0000user");

        User mockUser = new User();
        mockUser.setId(1);
        mockUser.setUsername("test\u0000user");

        Mockito.when(modifyUserService.sendRetrievePasswordCode(Mockito.anyString()))
                .thenReturn(mockUser);

        mockMvc.perform(post("/api/users/retrieve/sendcode")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    /**
     * 非法数据测试 - 验证码包含字母
     * (当前验证只检查验证码长度，不检查字符类型)
     */
    @Test
    @DisplayName("非法数据测试 - 验证码包含字母")
    void testRetrievePassword_CodeWithLetters() throws Exception {
        RetrievePasswordModifyRequest request = new RetrievePasswordModifyRequest();
        request.setAccount("testuser");
        request.setCode("a12345"); // 包含字母
        request.setNewPassword("newpass123");

        User mockUser = new User();
        mockUser.setId(1);
        mockUser.setUsername("testuser");

        Mockito.when(modifyUserService.retrievePassword(Mockito.any(RetrievePasswordModifyRequest.class)))
                .thenReturn(mockUser);

        // 当前验证只检查验证码长度，不检查字符类型
        mockMvc.perform(put("/api/users/retrieve/resetpassword")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    /**
     * 非法数据测试 - 新密码包含中文
     */
    @Test
    @DisplayName("非法数据测试 - 新密码包含中文")
    void testRetrievePassword_PasswordWithChinese() throws Exception {
        RetrievePasswordModifyRequest request = new RetrievePasswordModifyRequest();
        request.setAccount("testuser");
        request.setCode("123456");
        request.setNewPassword("密码123456"); // 包含中文

        mockMvc.perform(put("/api/users/retrieve/resetpassword")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request)))
                .andExpect(status().isBadRequest());
    }

    /**
     * 非法数据测试 - 非法邮箱格式
     * (因为账号可以是学号或邮箱，所以不会严格验证邮箱格式)
     */
    @Test
    @DisplayName("非法数据测试 - 非法邮箱格式")
    void testSendCode_InvalidEmail() throws Exception {
        RetrievePasswordCodeRequest request = new RetrievePasswordCodeRequest();
        request.setAccount("invalid-email");

        User mockUser = new User();
        mockUser.setId(1);
        mockUser.setUserNo("invalid-email");

        Mockito.when(modifyUserService.sendRetrievePasswordCode(Mockito.anyString()))
                .thenReturn(mockUser);

        // 账号可以是学号或邮箱，所以不会严格验证邮箱格式
        mockMvc.perform(post("/api/users/retrieve/sendcode")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    // ==================== 极限数据测试 ====================

    /**
     * 极限数据测试 - 超长账号
     */
    @Test
    @DisplayName("极限数据测试 - 超长账号1000个字符")
    void testSendCode_ExtremelyLongAccount() throws Exception {
        String longAccount = "a".repeat(1000);
        RetrievePasswordCodeRequest request = new RetrievePasswordCodeRequest();
        request.setAccount(longAccount);

        mockMvc.perform(post("/api/users/retrieve/sendcode")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request)))
                .andExpect(status().isBadRequest());
    }

    /**
     * 极限数据测试 - 超大JSON请求
     */
    @Test
    @DisplayName("极限数据测试 - 超大JSON请求")
    void testRetrievePassword_LargeJsonRequest() throws Exception {
        String longAccount = "a".repeat(5000);
        RetrievePasswordModifyRequest request = new RetrievePasswordModifyRequest();
        request.setAccount(longAccount);
        request.setCode("123456");
        request.setNewPassword("newpass123");

        mockMvc.perform(put("/api/users/retrieve/resetpassword")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request)))
                .andExpect(status().isBadRequest());
    }

    // ==================== 异常场景测试 ====================

    /**
     * 测试找回密码 - 账号为空
     */
    @Test
    @DisplayName("测试找回密码 - 账号为空")
    void testRetrievePassword_EmptyAccount() throws Exception {
        RetrievePasswordModifyRequest request = new RetrievePasswordModifyRequest();
        request.setAccount("");
        request.setCode("123456");
        request.setNewPassword("newpass123");

        mockMvc.perform(put("/api/users/retrieve/resetpassword")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request)))
                .andExpect(status().isBadRequest());
    }

    /**
     * 测试找回密码 - 验证码为空
     */
    @Test
    @DisplayName("测试找回密码 - 验证码为空")
    void testRetrievePassword_EmptyCode() throws Exception {
        RetrievePasswordModifyRequest request = new RetrievePasswordModifyRequest();
        request.setAccount("testuser");
        request.setCode("");
        request.setNewPassword("newpass123");

        mockMvc.perform(put("/api/users/retrieve/resetpassword")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request)))
                .andExpect(status().isBadRequest());
    }

    /**
     * 测试找回密码 - 新密码为空
     */
    @Test
    @DisplayName("测试找回密码 - 新密码为空")
    void testRetrievePassword_EmptyNewPassword() throws Exception {
        RetrievePasswordModifyRequest request = new RetrievePasswordModifyRequest();
        request.setAccount("testuser");
        request.setCode("123456");
        request.setNewPassword("");

        mockMvc.perform(put("/api/users/retrieve/resetpassword")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request)))
                .andExpect(status().isBadRequest());
    }
}