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
@ActiveProfiles("test")
class RetrieveUserControllerTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @MockitoBean
    private ModifyUserService modifyUserService;

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
        request.setCode("wrongcode");
        request.setNewPassword("newpass123");

        Mockito.when(modifyUserService.retrievePassword(Mockito.any(RetrievePasswordModifyRequest.class)))
                .thenThrow(new BusinessException(BusinessErrorCode.VERIFICATION_CODE_INVALID));

        mockMvc.perform(put("/api/users/retrieve/resetpassword")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("验证码错误"));
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
}