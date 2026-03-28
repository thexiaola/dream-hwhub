package top.thexiaola.dreamhwhub.module.login.enums;

/**
 * 业务错误码枚举
 */
public enum BusinessErrorCode {
    // 成功
    SUCCESS(0, "操作成功"),
    
    // 验证码相关错误
    VERIFICATION_CODE_INVALID(1001, "验证码无效"),
    VERIFICATION_CODE_EXPIRED(1002, "验证码已过期"),
    VERIFICATION_CODE_NOT_FOUND(1003, "验证码不存在"),
    
    // 注册相关错误
    USER_NO_EXISTS(2001, "学号已被占用"),
    USERNAME_EXISTS(2002, "用户名已被占用"),
    EMAIL_EXISTS(2003, "邮箱已被占用"),
    REGISTRATION_FAILED(2004, "注册失败"),
    
    // 登录相关错误
    USER_NOT_FOUND(3001, "用户不存在"),
    INVALID_CREDENTIALS(3002, "账号或密码错误"),
    USER_BANNED(3003, "用户已被封禁"),
    USER_NOT_LOGGED_IN(3004, "用户未登录"),
    
    // 邮件相关错误
    EMAIL_SEND_FAILED(4001, "邮件发送失败"),
    INVALID_EMAIL_FORMAT(4002, "邮箱格式不正确"),
    EMAIL_SERVER_NOT_CONFIGURED(4003, "邮件服务器未配置"),
    EMAIL_SENDING_FAILED(4004, "邮件发送失败"),
    
    // 系统错误
    SYSTEM_ERROR(5000, "系统错误"),
    DATABASE_ERROR(5001, "数据库操作失败"),
    
    // 修改用户信息错误
    USER_NO_REQUIRED(6001, "学号不能为空"),
    USERNAME_REQUIRED(6002, "用户名不能为空"),
    
    // 修改邮箱错误
    SAME_EMAIL(6003, "新邮箱不能与原邮箱相同"),
    
    // 修改密码错误
    INVALID_OLD_PASSWORD(6004, "原密码错误"),
    NEW_PASSWORD_SAME_AS_OLD(6005, "新密码不能与原密码相同");

    private final int code;
    private final String message;

    BusinessErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    /**
     * 根据错误码获取枚举
     */
    public static BusinessErrorCode fromCode(int code) {
        for (BusinessErrorCode errorCode : values()) {
            if (errorCode.getCode() == code) {
                return errorCode;
            }
        }
        return SYSTEM_ERROR;
    }

    /**
     * 判断是否为验证码相关错误
     */
    public static boolean isVerificationCodeError(BusinessErrorCode errorCode) {
        return errorCode == VERIFICATION_CODE_INVALID || 
               errorCode == VERIFICATION_CODE_EXPIRED || 
               errorCode == VERIFICATION_CODE_NOT_FOUND;
    }

    /**
     * 判断是否为重复注册错误
     */
    public static boolean isDuplicateRegistrationError(BusinessErrorCode errorCode) {
        return errorCode == USER_NO_EXISTS || 
               errorCode == USERNAME_EXISTS || 
               errorCode == EMAIL_EXISTS;
    }
}