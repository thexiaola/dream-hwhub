package top.thexiaola.dreamhwhub.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import top.thexiaola.dreamhwhub.common.api.ApiResponse;
import top.thexiaola.dreamhwhub.exception.BusinessException;

import java.util.Objects;

/**
 * 全局异常处理器
 * 统一处理控制器层抛出的异常，并返回标准化响应
 */
@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理业务逻辑异常 (BusinessException)
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Object>> handleBusinessException(BusinessException e) {
        log.info("Business exception occurred: code={}, message={}", e.getErrorCodeValue(), e.getMessage());
        return buildBusinessErrorResponse(e);
    }

    /**
     * 按错误码映射业务异常的 HTTP 状态码并构造标准响应（供控制器 catch 块复用，保证状态码语义一致）
     * 
     * @param e   业务异常
     * @param <T> 响应数据类型
     * @return 带正确 HTTP 状态码的标准化响应
     */
    @SuppressWarnings("unchecked")
    public static <T> ResponseEntity<ApiResponse<T>> buildBusinessErrorResponse(BusinessException e) {
        int code = e.getErrorCodeValue();
        String message = e.getMessage();

        // 根据错误码设置合适的 HTTP 状态码和返回码
        HttpStatus httpStatus;
        int returnCode;

        if (code == 9001 || code == 8504) {
            // 权限不足或不是班级成员返回 403
            httpStatus = HttpStatus.FORBIDDEN;
            returnCode = 403;
        } else if (code == 401 || code == 3001 || code == 3002 || code == 3003 || code == 3004) {
            // 未登录或认证失败返回 401
            httpStatus = HttpStatus.UNAUTHORIZED;
            returnCode = 401;
        } else if (code == 404 || code == 8501 || code == 7001 || code == 7004) {
            // 资源不存在返回 404（班级、作业、提交记录）
            httpStatus = HttpStatus.NOT_FOUND;
            returnCode = 404;
        } else {
            // 其他业务错误返回 400
            httpStatus = HttpStatus.BAD_REQUEST;
            returnCode = code;
        }

        return ResponseEntity.status(httpStatus).body(ApiResponse.error(returnCode, message, (T) e.getExtraData()));
    }

    /**
     * 处理参数校验异常 (JSR-303)
     */
    @ExceptionHandler({ MethodArgumentNotValidException.class, BindException.class })
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
    public ResponseEntity<ApiResponse<Void>> handleHttpRequestMethodNotSupportedException(
            HttpRequestMethodNotSupportedException e) {
        log.info("Request method not supported: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(ApiResponse.error(405, "请求方法不支持，请使用 " + e.getSupportedHttpMethods()));
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
    public ResponseEntity<ApiResponse<Void>> handleMissingServletRequestParameterException(
            MissingServletRequestParameterException e) {
        log.info("Missing request parameter: {}", e.getParameterName());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(400, "缺少必需的请求参数"));
    }

    /**
     * 处理参数类型转换异常（如日期格式不正确）
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodArgumentTypeMismatchException(
            MethodArgumentTypeMismatchException e) {
        String message = String.format("参数 '%s' 格式错误", e.getName());
        log.warn("Type mismatch for parameter: {}, value: {}, error: {}", e.getName(), e.getValue(), e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(400, message));
    }

    /**
     * 处理文件上传大小超限异常
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException e) {
        log.warn("File upload size exceeded: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(ApiResponse.error(413, "文件大小超过限制（最大50MB）"));
    }

    /**
     * 处理所有未捕获的异常
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpectedException(Exception e) {
        log.error("Unexpected error", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(500, "服务器内部错误，请联系管理员"));
    }
}
