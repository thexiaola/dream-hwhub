package top.thexiaola.dreamhwhub.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import top.thexiaola.dreamhwhub.interceptor.AuthInterceptor;

/**
 * Web配置类，注册拦截器
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {
    
    private final AuthInterceptor authInterceptor;

    public WebConfig(AuthInterceptor authInterceptor) {
        this.authInterceptor = authInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
            .addPathPatterns("/api/**")  // 拦截所有 API 请求
            .excludePathPatterns(        // 排除公开接口
                "/api/users/register",
                "/api/users/login",
                "/api/users/getregcode"
            );
    }
}