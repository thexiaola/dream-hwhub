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
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class LoginUserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LoginUserService loginUserService;

    @MockitoBean
    private UserMapper userMapper;

    @MockitoBean
    private JwtUtil jwtUtil;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
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
                .andExpect(jsonPath("$.code").value(3002))
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
                .andExpect(jsonPath("$.code").value(3003));
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

    // ==================== 边界测试 ====================

    /**
     * 边界测试 - 使用学号登录
     */
    @Test
    @DisplayName("边界测试 - 使用学号登录")
    void testLogin_WithUserNo() throws Exception {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setAccount("123456"); // 学号
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

        Mockito.when(loginUserService.login(Mockito.any(LoginRequest.class), Mockito.any()))
                .thenReturn(mockUser);
        Mockito.when(userMapper.toUserResponse(Mockito.any(User.class)))
                .thenReturn(userResponse);
        Mockito.when(jwtUtil.generateToken(Mockito.anyInt(), Mockito.anyString()))
                .thenReturn(mockToken);

        mockMvc.perform(post("/api/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.username").value("testuser"));
    }

    /**
     * 边界测试 - 使用邮箱登录
     */
    @Test
    @DisplayName("边界测试 - 使用邮箱登录")
    void testLogin_WithEmail() throws Exception {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setAccount("test@example.com"); // 邮箱
        loginRequest.setPassword("password123");

        User mockUser = new User();
        mockUser.setId(1);
        mockUser.setUsername("testuser");
        mockUser.setEmail("test@example.com");

        UserResponse userResponse = new UserResponse();
        userResponse.setId(1);
        userResponse.setUsername("testuser");
        userResponse.setEmail("test@example.com");

        String mockToken = "mock.jwt.token";

        Mockito.when(loginUserService.login(Mockito.any(LoginRequest.class), Mockito.any()))
                .thenReturn(mockUser);
        Mockito.when(userMapper.toUserResponse(Mockito.any(User.class)))
                .thenReturn(userResponse);
        Mockito.when(jwtUtil.generateToken(Mockito.anyInt(), Mockito.anyString()))
                .thenReturn(mockToken);

        mockMvc.perform(post("/api/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.username").value("testuser"));
    }

    // ==================== 非法数据测试 ====================

    /**
     * 非法数据测试 - 账号包含SQL注入字符
     */
    @Test
    @DisplayName("非法数据测试 - 账号包含SQL注入字符")
    void testLogin_AccountWithSqlInjection() throws Exception {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setAccount("' OR '1'='1"); // SQL注入
        loginRequest.setPassword("password123");

        Mockito.when(loginUserService.login(Mockito.any(LoginRequest.class), Mockito.any()))
                .thenThrow(new BusinessException(BusinessErrorCode.INVALID_CREDENTIALS));

        mockMvc.perform(post("/api/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(loginRequest)))
                .andExpect(status().isUnauthorized());
    }

    /**
     * 非法数据测试 - 账号包含XSS字符
     */
    @Test
    @DisplayName("非法数据测试 - 账号包含XSS字符")
    void testLogin_AccountWithXss() throws Exception {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setAccount("<script>alert(1)</script>"); // XSS
        loginRequest.setPassword("password123");

        Mockito.when(loginUserService.login(Mockito.any(LoginRequest.class), Mockito.any()))
                .thenThrow(new BusinessException(BusinessErrorCode.INVALID_CREDENTIALS));

        mockMvc.perform(post("/api/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(loginRequest)))
                .andExpect(status().isUnauthorized());
    }

    /**
     * 非法数据测试 - 特殊Unicode账号
     */
    @Test
    @DisplayName("非法数据测试 - 特殊Unicode账号")
    void testLogin_AccountWithSpecialUnicode() throws Exception {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setAccount("test\u0000user"); // 包含null字符
        loginRequest.setPassword("password123");

        // Mock service返回null，导致Controller中出现NPE
        Mockito.when(loginUserService.login(Mockito.any(LoginRequest.class), Mockito.any()))
                .thenReturn(null);

        mockMvc.perform(post("/api/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(loginRequest)))
                .andExpect(status().isInternalServerError());
    }

    // ==================== 极限数据测试 ====================

    /**
     * 极限数据测试 - 超长账号
     */
    @Test
    @DisplayName("极限数据测试 - 超长账号")
    void testLogin_VeryLongAccount() throws Exception {
        String longAccount = "a".repeat(1000);
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setAccount(longAccount);
        loginRequest.setPassword("password123");

        mockMvc.perform(post("/api/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(loginRequest)))
                .andExpect(status().isBadRequest());
    }

    /**
     * 极限数据测试 - 超大JSON请求
     */
    @Test
    @DisplayName("极限数据测试 - 超大JSON请求")
    void testLogin_LargeRequestBody() throws Exception {
        String largeJson = "{\"account\":\"test\",\"password\":\"" + "a".repeat(10000) + "\"}";

        mockMvc.perform(post("/api/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(largeJson))
                .andExpect(status().isBadRequest());
    }

    // ==================== 异常场景测试 ====================

    /**
     * 测试登出 - 未登录状态
     */
    @Test
    @DisplayName("测试登出 - 未登录状态")
    void testLogout_NotLoggedIn() throws Exception {
        Mockito.when(loginUserService.getCurrentUser(Mockito.any()))
                .thenThrow(new BusinessException(BusinessErrorCode.USER_NOT_FOUND));

        mockMvc.perform(post("/api/users/logout"))
                .andExpect(status().isBadRequest());
    }

    /**
     * 测试登出 - 服务器内部错误
     */
    @Test
    @DisplayName("测试登出 - 服务器内部错误")
    void testLogout_ServerError() throws Exception {
        User mockUser = new User();
        mockUser.setId(1);
        mockUser.setUsername("testuser");

        Mockito.when(loginUserService.getCurrentUser(Mockito.any()))
                .thenReturn(mockUser);
        Mockito.doThrow(new RuntimeException("Database error"))
                .when(loginUserService).logout(Mockito.anyInt(), Mockito.any());

        mockMvc.perform(post("/api/users/logout"))
                .andExpect(status().isInternalServerError());
    }
}