package top.thexiaola.dreamhwhub.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import top.thexiaola.dreamhwhub.config.security.AuthInterceptor;
import top.thexiaola.dreamhwhub.config.security.CsrfFilter;
import top.thexiaola.dreamhwhub.config.security.PermissionInterceptor;

/**
 * Web配置类，注册拦截器和过滤器
 */
@Configuration
@Profile("!test")
public class WebConfig implements WebMvcConfigurer {
    
    private final AuthInterceptor authInterceptor;
    private final CsrfFilter csrfFilter;
    private final PermissionInterceptor permissionInterceptor;

    public WebConfig(AuthInterceptor authInterceptor, CsrfFilter csrfFilter,
                     PermissionInterceptor permissionInterceptor) {
        this.authInterceptor = authInterceptor;
        this.csrfFilter = csrfFilter;
        this.permissionInterceptor = permissionInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 拦截所有 API 请求，公开接口由 AuthInterceptor 内部的统一白名单判定
        registry.addInterceptor(authInterceptor)
            .addPathPatterns("/api/**");

        // 权限节点校验：仅作用于管理员后台接口，须在认证拦截器之后执行
        registry.addInterceptor(permissionInterceptor)
            .addPathPatterns("/api/admin/**");
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