package top.thexiaola.dreamhwhub.support.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import top.thexiaola.dreamhwhub.exception.BusinessException;
import top.thexiaola.dreamhwhub.module.login.entity.User;

/**
 * 敏感操作身份二次验证拦截器
 * <p>
 * 读取 {@link RequireSensitiveVerification} 注解，要求调用方通过请求头提供操作者本人的
 * 身份凭据（登录密码或邮箱验证码）。校验失败返回 400，避免与「登录失效(401)」混淆而误触登出。
 * 依赖认证拦截器已将当前用户写入 request 属性，故须在其之后注册。
 */
@Component
@RequiredArgsConstructor
public class SensitiveVerificationInterceptor implements HandlerInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(SensitiveVerificationInterceptor.class);

    /** 验证方式请求头 */
    public static final String HEADER_METHOD = "X-Verify-Method";
    /** 登录密码请求头 */
    public static final String HEADER_PASSWORD = "X-Verify-Password";
    /** 邮箱验证码请求头 */
    public static final String HEADER_CODE = "X-Verify-Code";

    private final SensitiveOperationVerifier verifier;
    private final SensitiveOperationSettingsService settingsService;

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request,
                             @NonNull HttpServletResponse response,
                             @NonNull Object handler) throws Exception {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        RequireSensitiveVerification annotation = resolveAnnotation(handlerMethod);
        if (annotation == null) {
            return true;
        }

        User currentUser = (User) request.getAttribute("currentUser");
        if (currentUser == null) {
            return writeError(response, 401, "用户未登录");
        }

        // 解析稳定操作标识：优先 key()，未显式提供时回退 value()
        String operationKey = resolveKey(annotation);

        // 用户已为该操作关闭二次验证：直接放行（默认所有操作都需要验证）
        if (operationKey != null && SensitiveOperations.exists(operationKey)
                && !settingsService.isVerificationRequired(currentUser.getId(), operationKey)) {
            logger.info("Sensitive operation {} allowed for user {} (verification disabled for this operation)",
                    operationKey, currentUser.getId());
            return true;
        }

        String method = request.getHeader(HEADER_METHOD);
        String password = request.getHeader(HEADER_PASSWORD);
        String code = request.getHeader(HEADER_CODE);

        try {
            // 请求头是 ASCII 传输，密码可能含非 ASCII 时由前端做编码；此处直接按 UTF-8 使用
            verifier.verify(currentUser, method, password, code);
            return true;
        } catch (BusinessException e) {
            String opName = annotation.value();
            String message = e.getMessage();
            logger.warn("Sensitive verification failed for user {} on {} ({}): {}",
                    currentUser.getId(), request.getRequestURI(), opName, message);
            return writeError(response, e.getErrorCodeValue(), message);
        }
    }

    /** 解析操作标识：key() 优先，空白时回退 value()（可能为空） */
    private String resolveKey(RequireSensitiveVerification annotation) {
        String key = annotation.key();
        if (key != null && !key.isBlank()) {
            return key.trim();
        }
        String value = annotation.value();
        return (value != null && !value.isBlank()) ? value.trim() : null;
    }

    private RequireSensitiveVerification resolveAnnotation(HandlerMethod handlerMethod) {
        RequireSensitiveVerification annotation =
                handlerMethod.getMethodAnnotation(RequireSensitiveVerification.class);
        if (annotation != null) {
            return annotation;
        }
        return handlerMethod.getBeanType().getAnnotation(RequireSensitiveVerification.class);
    }

    private boolean writeError(HttpServletResponse response, int code, String message) throws Exception {
        // 二次验证失败属于用户输入错误：以 400 返回，避免前端当作登录过期而登出
        response.setStatus(HttpStatus.BAD_REQUEST.value());
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(String.format(
                "{\"code\":%d,\"message\":%s,\"data\":null}", code, jsonString(message)));
        return false;
    }

    /** 最小化 JSON 字符串转义，避免引入额外依赖 */
    private String jsonString(String raw) {
        if (raw == null) {
            return "\"\"";
        }
        StringBuilder sb = new StringBuilder(raw.length() + 2);
        sb.append('"');
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        sb.append('"');
        return sb.toString();
    }
}
