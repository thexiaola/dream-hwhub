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
import top.thexiaola.dreamhwhub.module.login.dto.EmailCodeRequest;
import top.thexiaola.dreamhwhub.module.login.dto.RegisterRequest;
import top.thexiaola.dreamhwhub.module.login.dto.UserResponse;
import top.thexiaola.dreamhwhub.module.login.entity.User;
import top.thexiaola.dreamhwhub.module.login.service.RegisterUserService;
import top.thexiaola.dreamhwhub.support.mapper.UserMapper;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 用户注册控制器单元测试
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class RegisterControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RegisterUserService registerUserService;

    @MockitoBean
    private UserMapper userMapper;

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
     * 测试正常注册场景
     */
    @Test
    @DisplayName("测试正常注册 - 成功")
    void testRegister_Success() throws Exception {
        // 准备测试数据
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setUserNo("123456");
        registerRequest.setUsername("张三");
        registerRequest.setEmail("zhangsan@example.com");
        registerRequest.setEmailCode("123456");
        registerRequest.setPassword("password123");

        User mockUser = new User();
        mockUser.setId(1);
        mockUser.setUserNo("123456");
        mockUser.setUsername("张三");
        mockUser.setEmail("zhangsan@example.com");

        UserResponse userResponse = new UserResponse();
        userResponse.setId(1);
        userResponse.setUserNo("123456");
        userResponse.setUsername("张三");
        userResponse.setEmail("zhangsan@example.com");

        // Mock 行为
        Mockito.when(registerUserService.register(Mockito.any(RegisterRequest.class)))
                .thenReturn(mockUser);
        Mockito.when(userMapper.toUserResponse(Mockito.any(User.class)))
                .thenReturn(userResponse);

        // 执行测试并验证
        mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(registerRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("注册成功"))
                .andExpect(jsonPath("$.data.username").value("张三"));
    }

    /**
     * 测试注册失败 - 学号已存在
     */
    @Test
    @DisplayName("测试注册失败 - 学号已存在")
    void testRegister_UserNoExists() throws Exception {
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setUserNo("123456");
        registerRequest.setUsername("张三");
        registerRequest.setEmail("zhangsan@example.com");
        registerRequest.setEmailCode("123456");
        registerRequest.setPassword("password123");

        // Mock 抛出业务异常
        Mockito.when(registerUserService.register(Mockito.any(RegisterRequest.class)))
                .thenThrow(new BusinessException(BusinessErrorCode.USER_NO_EXISTS));

        // 执行测试并验证
        mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(registerRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("学号已被占用"));
    }

    /**
     * 测试注册 - 学号为空
     */
    @Test
    @DisplayName("测试注册 - 学号为空")
    void testRegister_UserNoEmpty() throws Exception {
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setUserNo("");
        registerRequest.setUsername("张三");
        registerRequest.setEmail("zhangsan@example.com");
        registerRequest.setEmailCode("123456");
        registerRequest.setPassword("password123");

        // 执行测试并验证 - 应该被参数校验拦截
        mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(registerRequest)))
                .andExpect(status().isBadRequest());
    }

    /**
     * 测试注册 - 邮箱格式错误
     */
    @Test
    @DisplayName("测试注册 - 邮箱格式错误")
    void testRegister_InvalidEmail() throws Exception {
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setUserNo("123456");
        registerRequest.setUsername("张三");
        registerRequest.setEmail("invalid-email");
        registerRequest.setEmailCode("123456");
        registerRequest.setPassword("password123");

        // 执行测试并验证
        mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(registerRequest)))
                .andExpect(status().isBadRequest());
    }

    /**
     * 测试注册 - 密码过短
     */
    @Test
    @DisplayName("测试注册 - 密码长度不足")
    void testRegister_PasswordTooShort() throws Exception {
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setUserNo("123456");
        registerRequest.setUsername("张三");
        registerRequest.setEmail("zhangsan@example.com");
        registerRequest.setEmailCode("123456");
        registerRequest.setPassword("12345"); // 少于 6 位

        // 执行测试并验证
        mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(registerRequest)))
                .andExpect(status().isBadRequest());
    }

    /**
     * 测试发送注册验证码 - 成功
     */
    @Test
    @DisplayName("测试发送注册验证码 - 成功")
    void testSendRegisterCode_Success() throws Exception {
        EmailCodeRequest emailCodeRequest = new EmailCodeRequest();
        emailCodeRequest.setEmail("zhangsan@example.com");
        emailCodeRequest.setUserNo("123456");
        emailCodeRequest.setUsername("张三");

        Mockito.doNothing().when(registerUserService).sendEmailCode(
                Mockito.anyString(), Mockito.anyString(), Mockito.anyString());

        mockMvc.perform(post("/api/users/getregcode")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(emailCodeRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("验证码发送成功"));
    }

    /**
     * 测试发送注册验证码 - 邮箱为空
     */
    @Test
    @DisplayName("测试发送注册验证码 - 邮箱为空")
    void testSendRegisterCode_EmailEmpty() throws Exception {
        EmailCodeRequest emailCodeRequest = new EmailCodeRequest();
        emailCodeRequest.setEmail("");
        emailCodeRequest.setUserNo("123456");
        emailCodeRequest.setUsername("张三");

        mockMvc.perform(post("/api/users/getregcode")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(emailCodeRequest)))
                .andExpect(status().isBadRequest());
    }

    // ==================== 边界测试 ====================

    /**
     * 边界测试 - 学号达到最大长度
     */
    @Test
    @DisplayName("边界测试 - 学号达到最大长度24位")
    void testRegister_UserNoMaxLength() throws Exception {
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setUserNo("123456789012345678901234"); // 24位
        registerRequest.setUsername("张三");
        registerRequest.setEmail("zhangsan@example.com");
        registerRequest.setEmailCode("123456");
        registerRequest.setPassword("password123");

        User mockUser = new User();
        mockUser.setId(1);
        mockUser.setUserNo("123456789012345678901234");
        mockUser.setUsername("张三");
        mockUser.setEmail("zhangsan@example.com");

        UserResponse userResponse = new UserResponse();
        userResponse.setId(1);
        userResponse.setUserNo("123456789012345678901234");
        userResponse.setUsername("张三");
        userResponse.setEmail("zhangsan@example.com");

        Mockito.when(registerUserService.register(Mockito.any(RegisterRequest.class)))
                .thenReturn(mockUser);
        Mockito.when(userMapper.toUserResponse(Mockito.any(User.class)))
                .thenReturn(userResponse);

        mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(registerRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    /**
     * 边界测试 - 学号超过最大长度
     */
    @Test
    @DisplayName("边界测试 - 学号超过最大长度25位")
    void testRegister_UserNoTooLong() throws Exception {
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setUserNo("1234567890123456789012345"); // 25位
        registerRequest.setUsername("张三");
        registerRequest.setEmail("zhangsan@example.com");
        registerRequest.setEmailCode("123456");
        registerRequest.setPassword("password123");

        mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(registerRequest)))
                .andExpect(status().isBadRequest());
    }

    /**
     * 边界测试 - 用户名达到最大长度64位
     */
    @Test
    @DisplayName("边界测试 - 用户名达到最大长度64位")
    void testRegister_UsernameMaxLength() throws Exception {
        String longUsername = "张".repeat(64);
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setUserNo("123456");
        registerRequest.setUsername(longUsername);
        registerRequest.setEmail("zhangsan@example.com");
        registerRequest.setEmailCode("123456");
        registerRequest.setPassword("password123");

        User mockUser = new User();
        mockUser.setId(1);
        mockUser.setUserNo("123456");
        mockUser.setUsername(longUsername);
        mockUser.setEmail("zhangsan@example.com");

        UserResponse userResponse = new UserResponse();
        userResponse.setId(1);
        userResponse.setUserNo("123456");
        userResponse.setUsername(longUsername);
        userResponse.setEmail("zhangsan@example.com");

        Mockito.when(registerUserService.register(Mockito.any(RegisterRequest.class)))
                .thenReturn(mockUser);
        Mockito.when(userMapper.toUserResponse(Mockito.any(User.class)))
                .thenReturn(userResponse);

        mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(registerRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    /**
     * 边界测试 - 密码最小长度6位
     */
    @Test
    @DisplayName("边界测试 - 密码最小长度6位")
    void testRegister_PasswordMinLength() throws Exception {
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setUserNo("123456");
        registerRequest.setUsername("张三");
        registerRequest.setEmail("zhangsan@example.com");
        registerRequest.setEmailCode("123456");
        registerRequest.setPassword("abc123"); // 6位

        User mockUser = new User();
        mockUser.setId(1);
        mockUser.setUserNo("123456");
        mockUser.setUsername("张三");
        mockUser.setEmail("zhangsan@example.com");

        UserResponse userResponse = new UserResponse();
        userResponse.setId(1);
        userResponse.setUserNo("123456");
        userResponse.setUsername("张三");
        userResponse.setEmail("zhangsan@example.com");

        Mockito.when(registerUserService.register(Mockito.any(RegisterRequest.class)))
                .thenReturn(mockUser);
        Mockito.when(userMapper.toUserResponse(Mockito.any(User.class)))
                .thenReturn(userResponse);

        mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(registerRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    /**
     * 边界测试 - 密码最大长度48位
     */
    @Test
    @DisplayName("边界测试 - 密码最大长度48位")
    void testRegister_PasswordMaxLength() throws Exception {
        String longPassword = "a".repeat(48);
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setUserNo("123456");
        registerRequest.setUsername("张三");
        registerRequest.setEmail("zhangsan@example.com");
        registerRequest.setEmailCode("123456");
        registerRequest.setPassword(longPassword);

        User mockUser = new User();
        mockUser.setId(1);
        mockUser.setUserNo("123456");
        mockUser.setUsername("张三");
        mockUser.setEmail("zhangsan@example.com");

        UserResponse userResponse = new UserResponse();
        userResponse.setId(1);
        userResponse.setUserNo("123456");
        userResponse.setUsername("张三");
        userResponse.setEmail("zhangsan@example.com");

        Mockito.when(registerUserService.register(Mockito.any(RegisterRequest.class)))
                .thenReturn(mockUser);
        Mockito.when(userMapper.toUserResponse(Mockito.any(User.class)))
                .thenReturn(userResponse);

        mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(registerRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    // ==================== 非法数据测试 ====================

    /**
     * 非法数据测试 - 学号包含字母
     */
    @Test
    @DisplayName("非法数据测试 - 学号包含字母")
    void testRegister_UserNoWithLetters() throws Exception {
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setUserNo("abc123"); // 包含字母
        registerRequest.setUsername("张三");
        registerRequest.setEmail("zhangsan@example.com");
        registerRequest.setEmailCode("123456");
        registerRequest.setPassword("password123");

        mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(registerRequest)))
                .andExpect(status().isBadRequest());
    }

    /**
     * 非法数据测试 - 学号包含特殊字符
     */
    @Test
    @DisplayName("非法数据测试 - 学号包含特殊字符")
    void testRegister_UserNoWithSpecialChars() throws Exception {
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setUserNo("123#456"); // 包含特殊字符
        registerRequest.setUsername("张三");
        registerRequest.setEmail("zhangsan@example.com");
        registerRequest.setEmailCode("123456");
        registerRequest.setPassword("password123");

        mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(registerRequest)))
                .andExpect(status().isBadRequest());
    }

    /**
     * 非法数据测试 - 用户名包含换行符
     */
    @Test
    @DisplayName("非法数据测试 - 用户名包含换行符")
    void testRegister_UsernameWithNewline() throws Exception {
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setUserNo("123456");
        registerRequest.setUsername("张\n三"); // 包含换行符
        registerRequest.setEmail("zhangsan@example.com");
        registerRequest.setEmailCode("123456");
        registerRequest.setPassword("password123");

        mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(registerRequest)))
                .andExpect(status().isBadRequest());
    }

    /**
     * 非法数据测试 - 用户名包含制表符
     */
    @Test
    @DisplayName("非法数据测试 - 用户名包含制表符")
    void testRegister_UsernameWithTab() throws Exception {
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setUserNo("123456");
        registerRequest.setUsername("张\t三"); // 包含制表符
        registerRequest.setEmail("zhangsan@example.com");
        registerRequest.setEmailCode("123456");
        registerRequest.setPassword("password123");

        mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(registerRequest)))
                .andExpect(status().isBadRequest());
    }

    /**
     * 非法数据测试 - 密码包含禁用字符
     */
    @Test
    @DisplayName("非法数据测试 - 密码包含中文字符")
    void testRegister_PasswordWithChinese() throws Exception {
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setUserNo("123456");
        registerRequest.setUsername("张三");
        registerRequest.setEmail("zhangsan@example.com");
        registerRequest.setEmailCode("123456");
        registerRequest.setPassword("密码123456"); // 包含中文

        mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(registerRequest)))
                .andExpect(status().isBadRequest());
    }

    /**
     * 非法数据测试 - 邮箱缺少@符号
     */
    @Test
    @DisplayName("非法数据测试 - 邮箱缺少@符号")
    void testRegister_EmailMissingAt() throws Exception {
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setUserNo("123456");
        registerRequest.setUsername("张三");
        registerRequest.setEmail("zhangsanexample.com"); // 缺少@
        registerRequest.setEmailCode("123456");
        registerRequest.setPassword("password123");

        mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(registerRequest)))
                .andExpect(status().isBadRequest());
    }

    /**
     * 非法数据测试 - 邮箱缺少域名
     */
    @Test
    @DisplayName("非法数据测试 - 邮箱缺少域名")
    void testRegister_EmailMissingDomain() throws Exception {
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setUserNo("123456");
        registerRequest.setUsername("张三");
        registerRequest.setEmail("zhangsan@"); // 缺少域名
        registerRequest.setEmailCode("123456");
        registerRequest.setPassword("password123");

        mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(registerRequest)))
                .andExpect(status().isBadRequest());
    }

    /**
     * 非法数据测试 - 验证码长度不足
     */
    @Test
    @DisplayName("非法数据测试 - 验证码长度不足")
    void testRegister_CodeTooShort() throws Exception {
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setUserNo("123456");
        registerRequest.setUsername("张三");
        registerRequest.setEmail("zhangsan@example.com");
        registerRequest.setEmailCode("12345"); // 5位
        registerRequest.setPassword("password123");

        mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(registerRequest)))
                .andExpect(status().isBadRequest());
    }

    /**
     * 非法数据测试 - 验证码长度过长
     */
    @Test
    @DisplayName("非法数据测试 - 验证码长度过长")
    void testRegister_CodeTooLong() throws Exception {
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setUserNo("123456");
        registerRequest.setUsername("张三");
        registerRequest.setEmail("zhangsan@example.com");
        registerRequest.setEmailCode("1234567"); // 7位
        registerRequest.setPassword("password123");

        mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(registerRequest)))
                .andExpect(status().isBadRequest());
    }

    /**
     * 非法数据测试 - 验证码包含字母（当前验证规则只限制长度，不限制字符类型）
     * 注意：实际应用中应该添加 @Pattern 验证来限制验证码为纯数字
     */
    @Test
    @DisplayName("非法数据测试 - 验证码包含字母")
    void testRegister_CodeWithLetters() throws Exception {
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setUserNo("123456");
        registerRequest.setUsername("张三");
        registerRequest.setEmail("zhangsan@example.com");
        registerRequest.setEmailCode("a12345"); // 包含字母
        registerRequest.setPassword("password123");

        User mockUser = new User();
        mockUser.setId(1);
        mockUser.setUserNo("123456");
        mockUser.setUsername("张三");
        mockUser.setEmail("zhangsan@example.com");

        UserResponse userResponse = new UserResponse();
        userResponse.setId(1);
        userResponse.setUserNo("123456");
        userResponse.setUsername("张三");
        userResponse.setEmail("zhangsan@example.com");

        Mockito.when(registerUserService.register(Mockito.any(RegisterRequest.class)))
                .thenReturn(mockUser);
        Mockito.when(userMapper.toUserResponse(Mockito.any(User.class)))
                .thenReturn(userResponse);

        // 当前 RegisterRequest 的验证规则只限制验证码长度，不限制字符类型
        // 所以该请求会通过验证
        mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(registerRequest)))
                .andExpect(status().isOk());
    }

    // ==================== 极限数据测试 ====================

    /**
     * 极限数据测试 - 超大JSON请求体
     */
    @Test
    @DisplayName("极限数据测试 - 超大JSON请求体")
    void testRegister_LargeRequestBody() throws Exception {
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setUserNo("123456");
        String longUsername = "张".repeat(5000); // 超长用户名（应该被验证拦截）
        registerRequest.setUsername(longUsername);
        registerRequest.setEmail("zhangsan@example.com");
        registerRequest.setEmailCode("123456");
        registerRequest.setPassword("password123");

        mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(registerRequest)))
                .andExpect(status().isBadRequest());
    }

    /**
     * 极限数据测试 - 特殊Unicode字符
     */
    @Test
    @DisplayName("极限数据测试 - 特殊Unicode字符")
    void testRegister_SpecialUnicodeChars() throws Exception {
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setUserNo("123456");
        registerRequest.setUsername("张三😀🎉🌍"); // 包含emoji
        registerRequest.setEmail("zhangsan@example.com");
        registerRequest.setEmailCode("123456");
        registerRequest.setPassword("password123");

        // emoji应该被允许（只要不包含控制字符）
        User mockUser = new User();
        mockUser.setId(1);
        mockUser.setUserNo("123456");
        mockUser.setUsername("张三😀🎉🌍");
        mockUser.setEmail("zhangsan@example.com");

        UserResponse userResponse = new UserResponse();
        userResponse.setId(1);
        userResponse.setUserNo("123456");
        userResponse.setUsername("张三😀🎉🌍");
        userResponse.setEmail("zhangsan@example.com");

        Mockito.when(registerUserService.register(Mockito.any(RegisterRequest.class)))
                .thenReturn(mockUser);
        Mockito.when(userMapper.toUserResponse(Mockito.any(User.class)))
                .thenReturn(userResponse);

        mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(registerRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }
}