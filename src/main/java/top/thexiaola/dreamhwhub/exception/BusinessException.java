package top.thexiaola.dreamhwhub.exception;

import top.thexiaola.dreamhwhub.module.login.enums.BusinessErrorCode;

/**
 * 业务异常类
 */
public class BusinessException extends RuntimeException {
    
    private final BusinessErrorCode errorCode;
    
    public BusinessException(BusinessErrorCode errorCode, String message, Object extraData) {
        super(message);
        this.errorCode = errorCode;
    }

    public BusinessException(BusinessErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
    
    public BusinessException(BusinessErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
    
    public BusinessErrorCode getErrorCode() {
        return errorCode;
    }
    
    public int getErrorCodeValue() {
        return errorCode.getCode();
    }
}