package top.thexiaola.dreamhwhub.exception;

import lombok.Getter;
import top.thexiaola.dreamhwhub.enums.BusinessErrorCode;

/**
 * 业务异常类
 */
@Getter
public class BusinessException extends RuntimeException {
    
    private final BusinessErrorCode errorCode;

    private final Object extraData;

    public BusinessException(BusinessErrorCode errorCode, String message, Object extraData) {
        super(message);
        this.errorCode = errorCode;
        this.extraData = extraData;
    }

    public BusinessException(BusinessErrorCode errorCode, String message) {
        this(errorCode, message, null);
    }

    public BusinessException(BusinessErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.extraData = null;
    }

    public int getErrorCodeValue() {
        return errorCode.getCode();
    }
}