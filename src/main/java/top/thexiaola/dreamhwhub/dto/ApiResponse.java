package top.thexiaola.dreamhwhub.dto;

/**
 * 统一API响应包装类
 * @param <T> 响应数据类型
 */
public class ApiResponse<T> {
    private final int code;
    private final String message;
    private final T data;

    private ApiResponse(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(200, "操作成功", data);
    }

    public static <T> ApiResponse<T> error(int code, String message) {
        return new ApiResponse<>(code, message, null);
    }

    // --- getter ---
    public int getCode() { return code; }
    public String getMessage() { return message; }
    public T getData() { return data; }
}
