package top.thexiaola.dreamhwhub.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import top.thexiaola.dreamhwhub.config.security.AuthInterceptor;
import top.thexiaola.dreamhwhub.config.security.CsrfFilter;

/**
 * Web配置类，注册拦截器和过滤器
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {
    
    private final AuthInterceptor authInterceptor;
    private final CsrfFilter csrfFilter;

    public WebConfig(AuthInterceptor authInterceptor, CsrfFilter csrfFilter) {
        this.authInterceptor = authInterceptor;
        this.csrfFilter = csrfFilter;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
            .addPathPatterns("/api/**")  // 拦截所有 API 请求
            .excludePathPatterns(        // 排除公开接口
                "/api/users/register",
                "/api/users/login",
                "/api/users/getregcode",
                "/api/users/retrieve/sendcode",
                "/api/users/retrieve/resetpassword"
            );
    }
    
    /**
     * 注册CSRF过滤器
     * 在认证拦截器之前执行
     */
    @Bean
    public FilterRegistrationBean<CsrfFilter> csrfFilterRegistration() {
        FilterRegistrationBean<CsrfFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(csrfFilter);
        registration.addUrlPatterns("/api/*");
        registration.setOrder(1); // 在认证拦截器之前执行
        registration.setName("csrfFilter");
        return registration;
    }
}