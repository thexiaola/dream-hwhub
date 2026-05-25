package top.thexiaola.dreamhwhub.module.login.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import com.fasterxml.jackson.databind.ObjectMapper;
import top.thexiaola.dreamhwhub.enums.BusinessErrorCode;
import top.thexiaola.dreamhwhub.exception.BusinessException;
import top.thexiaola.dreamhwhub.module.login.dto.LoginRequest;
import top.thexiaola.dreamhwhub.module.login.dto.UserResponse;
import top.thexiaola.dreamhwhub.module.login.entity.User;
import top.thexiaola.dreamhwhub.module.login.service.LoginUserService;
import top.thexiaola.dreamhwhub.support.jwt.JwtUtil;
import top.thexiaola.dreamhwhub.support.mapper.UserMapper;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 用户登录控制器单元测试
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class LoginUserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private LoginUserService loginUserService;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private JwtUtil jwtUtil;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
    }

    @TestConfiguration
    static class TestConfig {
        @Bean
        public LoginUserService loginUserService() {
            return Mockito.mock(LoginUserService.class);
        }

        @Bean
        public UserMapper userMapper() {
            return Mockito.mock(UserMapper.class);
        }

        @Bean
        public JwtUtil jwtUtil() {
            return Mockito.mock(JwtUtil.class);
        }
    }

    /**
     * 将对象转换为 JSON 字符串
     */
    private String toJson(Object obj) throws Exception {
        return objectMapper.writeValueAsString(obj);
    }

    /**
     * 测试正常登录场景
     */
    @Test
    @DisplayName("测试正常登录 - 成功")
    void testLogin_Success() throws Exception {
        // 准备测试数据
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setAccount("testuser");
        loginRequest.setPassword("password123");

        User mockUser = new User();
        mockUser.setId(1);
        mockUser.setUsername("testuser");
        mockUser.setUserNo("123456");

        UserResponse userResponse = new UserResponse();
        userResponse.setId(1);
        userResponse.setUsername("testuser");
        userResponse.setUserNo("123456");

        String mockToken = "mock.jwt.token";

        // Mock 行为
        Mockito.when(loginUserService.login(Mockito.any(LoginRequest.class), Mockito.any()))
                .thenReturn(mockUser);
        Mockito.when(userMapper.toUserResponse(Mockito.any(User.class)))
                .thenReturn(userResponse);
        Mockito.when(jwtUtil.generateToken(Mockito.anyInt(), Mockito.anyString()))
                .thenReturn(mockToken);

        // 执行测试并验证
        mockMvc.perform(post("/api/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("登录成功"))
                .andExpect(jsonPath("$.data.username").value("testuser"))
                .andExpect(jsonPath("$.data.token").value("mock.jwt.token"));
    }

    /**
     * 测试登录失败 - 密码错误
     */
    @Test
    @DisplayName("测试登录失败 - 密码错误")
    void testLogin_InvalidCredentials() throws Exception {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setAccount("testuser");
        loginRequest.setPassword("wrongpassword");

        // Mock 抛出业务异常
        Mockito.when(loginUserService.login(Mockito.any(LoginRequest.class), Mockito.any()))
                .thenThrow(new BusinessException(BusinessErrorCode.INVALID_CREDENTIALS));

        // 执行测试并验证
        mockMvc.perform(post("/api/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(loginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.message").value("账号或密码错误"));
    }

    /**
     * 测试登录失败 - 账号被封禁
     */
    @Test
    @DisplayName("测试登录失败 - 账号被封禁")
    void testLogin_UserBanned() throws Exception {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setAccount("banneduser");
        loginRequest.setPassword("password123");

        // Mock 抛出账号封禁异常
        Mockito.when(loginUserService.login(Mockito.any(LoginRequest.class), Mockito.any()))
                .thenThrow(new BusinessException(BusinessErrorCode.USER_BANNED));

        // 执行测试并验证
        mockMvc.perform(post("/api/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(loginRequest)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));
    }

    /**
     * 测试登录 - 账号为空
     */
    @Test
    @DisplayName("测试登录 - 账号为空")
    void testLogin_AccountEmpty() throws Exception {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setAccount("");
        loginRequest.setPassword("password123");

        // 执行测试并验证 - 应该被参数校验拦截
        mockMvc.perform(post("/api/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(loginRequest)))
                .andExpect(status().isBadRequest());
    }

    /**
     * 测试登录 - 密码为空
     */
    @Test
    @DisplayName("测试登录 - 密码为空")
    void testLogin_PasswordEmpty() throws Exception {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setAccount("testuser");
        loginRequest.setPassword("");

        // 执行测试并验证 - 应该被参数校验拦截
        mockMvc.perform(post("/api/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(loginRequest)))
                .andExpect(status().isBadRequest());
    }

    /**
     * 测试登录 - 密码过长
     */
    @Test
    @DisplayName("测试登录 - 密码超过最大长度")
    void testLogin_PasswordTooLong() throws Exception {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setAccount("testuser");
        loginRequest.setPassword("a".repeat(51)); // 超过 50 位限制

        // 执行测试并验证
        mockMvc.perform(post("/api/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(loginRequest)))
                .andExpect(status().isBadRequest());
    }

    /**
     * 测试登出 - 成功
     */
    @Test
    @DisplayName("测试登出 - 成功")
    void testLogout_Success() throws Exception {
        User mockUser = new User();
        mockUser.setId(1);
        mockUser.setUsername("testuser");

        Mockito.when(loginUserService.getCurrentUser(Mockito.any()))
                .thenReturn(mockUser);
        Mockito.doNothing().when(loginUserService).logout(Mockito.anyInt(), Mockito.any());

        mockMvc.perform(post("/api/users/logout"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("登出成功"));
    }
}