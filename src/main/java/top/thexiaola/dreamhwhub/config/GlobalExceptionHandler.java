package top.thexiaola.dreamhwhub.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.resource.NoResourceFoundException;
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
        log.info("Business exception occurred: code={}, message={}", code, message);
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
        log.info("Validation exception: {}", message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(400, message));
    }

    /**
     * 处理 JSON 解析异常
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleHttpMessageNotReadableException(HttpMessageNotReadableException e) {
        log.info("Invalid JSON format: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(400, "请求数据格式错误，请检查 JSON 格式"));
    }

    /**
     * 处理请求方法不支持的异常
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleHttpRequestMethodNotSupportedException(HttpRequestMethodNotSupportedException e) {
        log.info("Request method not supported: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(ApiResponse.error(405, "请求方法不支持，请使用 " + e.getSupportedHttpMethods()));
    }

    /**
     * 处理资源未找到异常 (404)
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNoResourceFoundException(NoResourceFoundException e) {
        log.debug("Resource not found: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(404, "请求的资源不存在"));
    }

    /**
     * 处理请求参数缺失异常
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingServletRequestParameterException(MissingServletRequestParameterException e) {
        log.info("Missing request parameter: {}", e.getParameterName());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(400, "缺少必需的请求参数"));
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
