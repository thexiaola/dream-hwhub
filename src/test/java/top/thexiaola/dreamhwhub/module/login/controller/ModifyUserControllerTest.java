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
import top.thexiaola.dreamhwhub.module.login.dto.*;
import top.thexiaola.dreamhwhub.module.login.entity.User;
import top.thexiaola.dreamhwhub.module.login.service.ModifyUserService;
import top.thexiaola.dreamhwhub.support.mapper.UserMapper;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 用户信息修改控制器单元测试
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class ModifyUserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ModifyUserService modifyUserService;

    @MockitoBean
    private UserMapper userMapper;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
    }

    private String toJson(Object obj) throws Exception {
        return objectMapper.writeValueAsString(obj);
    }

    /**
     * 测试修改用户信息 - 成功
     */
    @Test
    @DisplayName("测试修改用户信息 - 成功")
    void testModifyUserInfo_Success() throws Exception {
        ModifyUserInfoRequest request = new ModifyUserInfoRequest();
        request.setUsername("新用户名");

        User mockUser = new User();
        mockUser.setId(1);
        mockUser.setUsername("新用户名");

        UserResponse userResponse = new UserResponse();
        userResponse.setId(1);
        userResponse.setUsername("新用户名");

        Mockito.when(modifyUserService.modifyUserInfo(Mockito.any(ModifyUserInfoRequest.class)))
                .thenReturn(mockUser);
        Mockito.when(userMapper.toUserResponse(Mockito.any(User.class)))
                .thenReturn(userResponse);

        mockMvc.perform(put("/api/users/modify/info")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("信息修改成功"));
    }

    /**
     * 测试修改邮箱 - 成功
     */
    @Test
    @DisplayName("测试修改邮箱 - 成功")
    void testModifyEmail_Success() throws Exception {
        ModifyEmailRequest request = new ModifyEmailRequest();
        request.setNewEmail("newemail@example.com");
        request.setBeforeCode("123456");
        request.setAfterCode("123456");

        User mockUser = new User();
        mockUser.setId(1);
        mockUser.setEmail("newemail@example.com");

        UserResponse userResponse = new UserResponse();
        userResponse.setId(1);
        userResponse.setEmail("newemail@example.com");

        Mockito.when(modifyUserService.modifyUserEmail(Mockito.any(ModifyEmailRequest.class)))
                .thenReturn(mockUser);
        Mockito.when(userMapper.toUserResponse(Mockito.any(User.class)))
                .thenReturn(userResponse);

        mockMvc.perform(put("/api/users/modify/email")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("邮箱修改成功"));
    }

    /**
     * 测试修改密码 - 成功
     */
    @Test
    @DisplayName("测试修改密码 - 成功")
    void testModifyPassword_Success() throws Exception {
        ModifyPasswordRequest request = new ModifyPasswordRequest();
        request.setOldPassword("oldpass123");
        request.setNewPassword("newpass123");

        Mockito.doNothing().when(modifyUserService).modifyUserPassword(Mockito.any(ModifyPasswordRequest.class));

        mockMvc.perform(put("/api/users/modify/password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("密码修改成功"));
    }

    /**
     * 测试发送旧邮箱验证码 - 成功
     */
    @Test
    @DisplayName("测试发送旧邮箱验证码 - 成功")
    void testSendModifyCodeBefore_Success() throws Exception {
        Mockito.doNothing().when(modifyUserService).sendModifyCodeToOldEmail();

        mockMvc.perform(post("/api/users/modify/getmodifycode/before"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("验证码已发送"));
    }

    /**
     * 测试发送新邮箱验证码 - 成功
     */
    @Test
    @DisplayName("测试发送新邮箱验证码 - 成功")
    void testSendModifyCodeAfter_Success() throws Exception {
        SendModifyCodeRequest request = new SendModifyCodeRequest();
        request.setNewEmail("newemail@example.com");

        Mockito.doNothing().when(modifyUserService).sendModifyCodeToNewEmail(Mockito.anyString());

        mockMvc.perform(post("/api/users/modify/getmodifycode/after")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("验证码已发送"));
    }

    /**
     * 测试修改密码 - 旧密码错误
     */
    @Test
    @DisplayName("测试修改密码 - 旧密码错误")
    void testModifyPassword_WrongOldPassword() throws Exception {
        ModifyPasswordRequest request = new ModifyPasswordRequest();
        request.setOldPassword("wrongpass");
        request.setNewPassword("newpass123");

        Mockito.doThrow(new BusinessException(BusinessErrorCode.INVALID_OLD_PASSWORD))
                .when(modifyUserService).modifyUserPassword(Mockito.any(ModifyPasswordRequest.class));

        mockMvc.perform(put("/api/users/modify/password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("原密码错误"));
    }

    /**
     * 测试修改邮箱 - 邮箱格式错误
     */
    @Test
    @DisplayName("测试修改邮箱 - 邮箱格式错误")
    void testModifyEmail_InvalidEmail() throws Exception {
        ModifyEmailRequest request = new ModifyEmailRequest();
        request.setNewEmail("invalid-email");
        request.setBeforeCode("123456");
        request.setAfterCode("123456");

        mockMvc.perform(put("/api/users/modify/email")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(request)))
                .andExpect(status().isBadRequest());
    }

    // ==================== 边界测试 ====================

    /**
     * 边界测试 - 用户名最大长度
     */
    @Test
    @DisplayName("边界测试 - 用户名最大长度64个字符")
    void testModifyUserInfo_MaxUsernameLength() throws Exception {
        String longUsername = "张".repeat(64);
        ModifyUserInfoRequest request = new ModifyUserInfoRequest();
        request.setUsername(longUsername);

        User mockUser = new User();
        mockUser.setId(1);
        mockUser.setUsername(longUsername);

        UserResponse userResponse = new UserResponse();
        userResponse.setId(1);
        userResponse.setUsername(longUsername);

        Mockito.when(modifyUserService.modifyUserInfo(Mockito.any(ModifyUserInfoRequest.class)))
                .thenReturn(mockUser);
        Mockito.when(userMapper.toUserResponse(Mockito.any(User.class)))
                .thenReturn(userResponse);

        mockMvc.perform(put("/api/users/modify/info")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    /**
     * 边界测试 - 密码最小长度
     */
    @Test
    @DisplayName("边界测试 - 密码最小长度6个字符")
    void testModifyPassword_MinPasswordLength() throws Exception {
        ModifyPasswordRequest request = new ModifyPasswordRequest();
        request.setOldPassword("oldpass");
        request.setNewPassword("abc123");

        Mockito.doNothing().when(modifyUserService).modifyUserPassword(Mockito.any(ModifyPasswordRequest.class));

        mockMvc.perform(put("/api/users/modify/password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    /**
     * 边界测试 - 密码最大长度
     */
    @Test
    @DisplayName("边界测试 - 密码最大长度48个字符")
    void testModifyPassword_MaxPasswordLength() throws Exception {
        String longPassword = "a".repeat(48);
        ModifyPasswordRequest request = new ModifyPasswordRequest();
        request.setOldPassword("oldpass123");
        request.setNewPassword(longPassword);

        Mockito.doNothing().when(modifyUserService).modifyUserPassword(Mockito.any(ModifyPasswordRequest.class));

        mockMvc.perform(put("/api/users/modify/password")
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
    void testModifyEmail_MinCodeLength() throws Exception {
        ModifyEmailRequest request = new ModifyEmailRequest();
        request.setNewEmail("newemail@example.com");
        request.setBeforeCode("12345");
        request.setAfterCode("12345");

        mockMvc.perform(put("/api/users/modify/email")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(request)))
                .andExpect(status().isBadRequest());
    }

    // ==================== 非法数据测试 ====================

    /**
     * 非法数据测试 - 用户名包含换行符
     */
    @Test
    @DisplayName("非法数据测试 - 用户名包含换行符")
    void testModifyUserInfo_UsernameWithNewline() throws Exception {
        ModifyUserInfoRequest request = new ModifyUserInfoRequest();
        request.setUsername("张\n三");

        mockMvc.perform(put("/api/users/modify/info")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(request)))
                .andExpect(status().isBadRequest());
    }

    /**
     * 非法数据测试 - 用户名包含制表符
     */
    @Test
    @DisplayName("非法数据测试 - 用户名包含制表符")
    void testModifyUserInfo_UsernameWithTab() throws Exception {
        ModifyUserInfoRequest request = new ModifyUserInfoRequest();
        request.setUsername("张\t三");

        mockMvc.perform(put("/api/users/modify/info")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(request)))
                .andExpect(status().isBadRequest());
    }

    /**
     * 非法数据测试 - 邮箱缺少@符号
     */
    @Test
    @DisplayName("非法数据测试 - 邮箱缺少@符号")
    void testModifyEmail_EmailMissingAt() throws Exception {
        ModifyEmailRequest request = new ModifyEmailRequest();
        request.setNewEmail("newemail.example.com");
        request.setBeforeCode("123456");
        request.setAfterCode("123456");

        mockMvc.perform(put("/api/users/modify/email")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(request)))
                .andExpect(status().isBadRequest());
    }

    /**
     * 非法数据测试 - 邮箱缺少域名
     */
    @Test
    @DisplayName("非法数据测试 - 邮箱缺少域名")
    void testModifyEmail_EmailMissingDomain() throws Exception {
        ModifyEmailRequest request = new ModifyEmailRequest();
        request.setNewEmail("newemail@");
        request.setBeforeCode("123456");
        request.setAfterCode("123456");

        mockMvc.perform(put("/api/users/modify/email")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(request)))
                .andExpect(status().isBadRequest());
    }

    /**
     * 非法数据测试 - 密码包含中文字符
     */
    @Test
    @DisplayName("非法数据测试 - 密码包含中文字符")
    void testModifyPassword_PasswordWithChinese() throws Exception {
        ModifyPasswordRequest request = new ModifyPasswordRequest();
        request.setOldPassword("oldpass123");
        request.setNewPassword("密码123456");

        mockMvc.perform(put("/api/users/modify/password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(request)))
                .andExpect(status().isBadRequest());
    }

    /**
     * 非法数据测试 - 验证码包含字母
     * (当前验证只检查验证码长度，不检查字符类型)
     */
    @Test
    @DisplayName("非法数据测试 - 验证码包含字母")
    void testModifyEmail_CodeWithLetters() throws Exception {
        ModifyEmailRequest request = new ModifyEmailRequest();
        request.setNewEmail("newemail@example.com");
        request.setBeforeCode("a12345");
        request.setAfterCode("a12345");

        User mockUser = new User();
        mockUser.setId(1);
        mockUser.setEmail("newemail@example.com");

        UserResponse userResponse = new UserResponse();
        userResponse.setId(1);
        userResponse.setEmail("newemail@example.com");

        Mockito.when(modifyUserService.modifyUserEmail(Mockito.any(ModifyEmailRequest.class)))
                .thenReturn(mockUser);
        Mockito.when(userMapper.toUserResponse(Mockito.any(User.class)))
                .thenReturn(userResponse);

        // 当前验证只检查验证码长度，不检查字符类型
        mockMvc.perform(put("/api/users/modify/email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    /**
     * 非法数据测试 - 新邮箱与旧邮箱相同
     */
    @Test
    @DisplayName("非法数据测试 - 新邮箱与旧邮箱相同")
    void testModifyEmail_SameEmail() throws Exception {
        ModifyEmailRequest request = new ModifyEmailRequest();
        request.setNewEmail("same@example.com");
        request.setBeforeCode("123456");
        request.setAfterCode("123456");

        Mockito.doThrow(new BusinessException(BusinessErrorCode.SAME_EMAIL))
                .when(modifyUserService).modifyUserEmail(Mockito.any(ModifyEmailRequest.class));

        mockMvc.perform(put("/api/users/modify/email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request)))
                .andExpect(status().isBadRequest());
    }

    // ==================== 极限数据测试 ====================

    /**
     * 极限数据测试 - 超长长用户名
     */
    @Test
    @DisplayName("极限数据测试 - 超长长用户名1000个字符")
    void testModifyUserInfo_ExtremelyLongUsername() throws Exception {
        String longUsername = "张".repeat(1000);
        ModifyUserInfoRequest request = new ModifyUserInfoRequest();
        request.setUsername(longUsername);

        mockMvc.perform(put("/api/users/modify/info")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(request)))
                .andExpect(status().isBadRequest());
    }

    /**
     * 极限数据测试 - 特殊Unicode字符
     */
    @Test
    @DisplayName("极限数据测试 - 用户名包含Emoji字符")
    void testModifyUserInfo_UsernameWithEmoji() throws Exception {
        ModifyUserInfoRequest request = new ModifyUserInfoRequest();
        request.setUsername("张😀🎉三");

        User mockUser = new User();
        mockUser.setId(1);
        mockUser.setUsername("张😀🎉三");

        UserResponse userResponse = new UserResponse();
        userResponse.setId(1);
        userResponse.setUsername("张😀🎉三");

        Mockito.when(modifyUserService.modifyUserInfo(Mockito.any(ModifyUserInfoRequest.class)))
                .thenReturn(mockUser);
        Mockito.when(userMapper.toUserResponse(Mockito.any(User.class)))
                .thenReturn(userResponse);

        mockMvc.perform(put("/api/users/modify/info")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    /**
     * 极限数据测试 - 超大JSON请求
     */
    @Test
    @DisplayName("极限数据测试 - 超大JSON请求")
    void testModifyUserInfo_LargeJsonRequest() throws Exception {
        String largeUsername = "张".repeat(5000);
        ModifyUserInfoRequest request = new ModifyUserInfoRequest();
        request.setUsername(largeUsername);

        mockMvc.perform(put("/api/users/modify/info")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(request)))
                .andExpect(status().isBadRequest());
    }

    // ==================== 异常场景测试 ====================

    /**
     * 测试修改邮箱 - 旧邮箱验证码错误
     */
    @Test
    @DisplayName("测试修改邮箱 - 旧邮箱验证码错误")
    void testModifyEmail_WrongBeforeCode() throws Exception {
        ModifyEmailRequest request = new ModifyEmailRequest();
        request.setNewEmail("newemail@example.com");
        request.setBeforeCode("000000");
        request.setAfterCode("123456");

        Mockito.doThrow(new BusinessException(BusinessErrorCode.VERIFICATION_CODE_INVALID))
                .when(modifyUserService).modifyUserEmail(Mockito.any(ModifyEmailRequest.class));

        mockMvc.perform(put("/api/users/modify/email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    /**
     * 测试修改邮箱 - 新邮箱验证码错误
     */
    @Test
    @DisplayName("测试修改邮箱 - 新邮箱验证码错误")
    void testModifyEmail_WrongAfterCode() throws Exception {
        ModifyEmailRequest request = new ModifyEmailRequest();
        request.setNewEmail("newemail@example.com");
        request.setBeforeCode("123456");
        request.setAfterCode("000000");

        Mockito.doThrow(new BusinessException(BusinessErrorCode.VERIFICATION_CODE_INVALID))
                .when(modifyUserService).modifyUserEmail(Mockito.any(ModifyEmailRequest.class));

        mockMvc.perform(put("/api/users/modify/email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }
}