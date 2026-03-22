package top.thexiaola.dreamhwhub.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import top.thexiaola.dreamhwhub.dto.ApiResponse;
import top.thexiaola.dreamhwhub.exception.BusinessException;

import java.util.Objects;

/**
 * 全局异常处理器
 * 统一处理控制器层抛出的异常，并返回标准化响应
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 处理业务逻辑异常 (ServiceResult.failure)
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException e) {
        int code = e.getErrorCodeValue();
        String message = e.getMessage();
        log.warn("Business exception occurred: code={}, message={}", code, message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(code, message));
    }

    /**
     * 处理参数校验异常 (JSR-303)
     */
    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public ResponseEntity<ApiResponse<Void>> handleValidationException(Exception e) {
        String message = "请求参数校验失败";
        if (e instanceof MethodArgumentNotValidException validException) {
            message = Objects.requireNonNull(validException.getFieldError()).getDefaultMessage();
        } else if (e instanceof BindException bindException) {
            message = Objects.requireNonNull(bindException.getFieldError()).getDefaultMessage();
        }
        log.warn("Validation exception: {}", message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(400, message));
    }

    /**
     * 处理所有未捕获的异常
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpectedException(Exception e) {
        // TODO: 解决无效路径刷报错的问题
        log.error("Unexpected error", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(500, "服务器内部错误，请联系管理员"));
    }
}
