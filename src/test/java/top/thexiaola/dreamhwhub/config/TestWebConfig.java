package top.thexiaola.dreamhwhub.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import top.thexiaola.dreamhwhub.module.login.entity.User;

/**
 * 测试环境下的Web配置，设置默认测试用户
 */
@Configuration
@Profile("test")
public class TestWebConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new TestUserInterceptor());
    }

    /**
     * 测试用户拦截器 - 设置默认测试用户
     */
    public static class TestUserInterceptor implements HandlerInterceptor {
        @Override
        public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
            User testUser = new User();
            testUser.setId(1);
            testUser.setUsername("testuser");
            testUser.setUserNo("123456");
            testUser.setPermission((short) 1);
            request.setAttribute("currentUser", testUser);
            return true;
        }
    }
}