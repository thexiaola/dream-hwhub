package top.thexiaola.dreamhwhub.module.login.dto;

import top.thexiaola.dreamhwhub.module.login.enums.BusinessErrorCode;

/**
 * 服务层返回结果包装类
 * 用于统一返回业务结果和错误码
 */
public class ServiceResult<T> {
    
    private final boolean success;
    private final T data;
    private final BusinessErrorCode errorCode;
    private final String message;
    private final Object extraData; // 额外数据（如剩余时间）
    
    private ServiceResult(boolean success, T data, BusinessErrorCode errorCode, String message, Object extraData) {
        this.success = success;
        this.data = data;
        this.errorCode = errorCode;
        this.message = message;
        this.extraData = extraData;
    }
    
    /**
     * 创建成功的返回结果
     */
    public static <T> ServiceResult<T> success(T data) {
        return new ServiceResult<>(true, data, null, null, null);
    }
    
    /**
     * 创建失败的返回结果
     */
    public static <T> ServiceResult<T> failure(BusinessErrorCode errorCode) {
        return new ServiceResult<>(false, null, errorCode, errorCode.getMessage(), null);
    }
    
    /**
     * 创建失败的返回结果（带自定义消息）
     */
    public static <T> ServiceResult<T> failure(BusinessErrorCode errorCode, String message) {
        return new ServiceResult<>(false, null, errorCode, message, null);
    }
    
    /**
     * 创建失败的返回结果（带自定义消息和额外数据）
     */
    public static <T> ServiceResult<T> failure(BusinessErrorCode errorCode, String message, Object extraData) {
        return new ServiceResult<>(false, null, errorCode, message, extraData);
    }
    
    public boolean isSuccess() {
        return success;
    }
    
    public T getData() {
        return data;
    }
    
    public BusinessErrorCode getErrorCode() {
        return errorCode;
    }
    
    public String getMessage() {
        return message;
    }
    
    public Object getExtraData() {
        return extraData;
    }
    
    /**
     * 获取错误码值
     */
    public int getErrorCodeValue() {
        return errorCode != null ? errorCode.getCode() : 0;
    }
}