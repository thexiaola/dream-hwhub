package top.thexiaola.dreamhwhub.config.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import top.thexiaola.dreamhwhub.module.login.entity.User;
import top.thexiaola.dreamhwhub.module.login.mapper.UserMapper;
import top.thexiaola.dreamhwhub.support.jwt.JwtUtil;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 认证拦截器单元测试：覆盖公开路径放行、Token 缺失/无效，以及账号被删除、封禁等越权场景
 */
class AuthInterceptorTest {

    private JwtUtil jwtUtil;
    private UserMapper userMapper;
    private AuthInterceptor authInterceptor;

    private HttpServletRequest request;
    private HttpServletResponse response;

    @BeforeEach
    void setUp() throws Exception {
        jwtUtil = mock(JwtUtil.class);
        userMapper = mock(UserMapper.class);
        authInterceptor = new AuthInterceptor(jwtUtil, userMapper);

        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        when(response.getWriter()).thenReturn(new PrintWriter(new StringWriter()));
    }

    @Test
    @DisplayName("公开接口无需认证直接放行")
    void publicPathPassesWithoutToken() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/users/login");

        assertTrue(authInterceptor.preHandle(request, response, new Object()));
        verifyNoInteractions(jwtUtil);
    }

    @Test
    @DisplayName("未携带Authorization头时返回401")
    void missingTokenRejected() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/users/info");
        when(request.getHeader("Authorization")).thenReturn(null);

        assertFalse(authInterceptor.preHandle(request, response, new Object()));
        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verifyNoInteractions(userMapper);
    }

    @Test
    @DisplayName("Token无效或已过期时返回401")
    void invalidTokenRejected() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/users/info");
        when(request.getHeader("Authorization")).thenReturn("Bearer invalid-token");
        when(jwtUtil.validateToken("invalid-token")).thenReturn(false);

        assertFalse(authInterceptor.preHandle(request, response, new Object()));
        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verifyNoInteractions(userMapper);
    }

    @Test
    @DisplayName("Token指向已删除账号时返回401")
    void deletedUserRejected() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/users/info");
        when(request.getHeader("Authorization")).thenReturn("Bearer valid-token");
        when(jwtUtil.validateToken("valid-token")).thenReturn(true);

        User tokenUser = new User();
        tokenUser.setId(99);
        when(jwtUtil.getUserFromToken("valid-token")).thenReturn(tokenUser);
        when(userMapper.selectById(99)).thenReturn(null);

        assertFalse(authInterceptor.preHandle(request, response, new Object()));
        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    }

    @Test
    @DisplayName("账号已被封禁时返回401")
    void bannedUserRejected() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/users/info");
        when(request.getHeader("Authorization")).thenReturn("Bearer valid-token");
        when(jwtUtil.validateToken("valid-token")).thenReturn(true);

        User tokenUser = new User();
        tokenUser.setId(1);
        when(jwtUtil.getUserFromToken("valid-token")).thenReturn(tokenUser);

        User bannedUser = new User();
        bannedUser.setId(1);
        bannedUser.setUsername("banned");
        bannedUser.setIsBanned(true);
        when(userMapper.selectById(1)).thenReturn(bannedUser);

        assertFalse(authInterceptor.preHandle(request, response, new Object()));
        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    }

    @Test
    @DisplayName("Token有效且账号正常时放行并写入当前用户")
    void validTokenPassesAndSetsCurrentUser() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/users/info");
        when(request.getHeader("Authorization")).thenReturn("Bearer valid-token");
        when(jwtUtil.validateToken("valid-token")).thenReturn(true);

        User tokenUser = new User();
        tokenUser.setId(1);
        when(jwtUtil.getUserFromToken("valid-token")).thenReturn(tokenUser);

        User currentUser = new User();
        currentUser.setId(1);
        currentUser.setUsername("TheXiaoLa");
        currentUser.setIsBanned(false);
        when(userMapper.selectById(1)).thenReturn(currentUser);

        assertTrue(authInterceptor.preHandle(request, response, new Object()));
        verify(request).setAttribute("currentUser", currentUser);
    }
}
