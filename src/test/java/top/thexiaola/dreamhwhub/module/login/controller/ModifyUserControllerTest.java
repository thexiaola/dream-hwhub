package top.thexiaola.dreamhwhub.module.login.controller;

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
@ActiveProfiles("test")
class ModifyUserControllerTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @MockitoBean
    private ModifyUserService modifyUserService;

    @MockitoBean
    private UserMapper userMapper;

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
                .andExpect(jsonPath("$.message").value("旧密码错误"));
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
}