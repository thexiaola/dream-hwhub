package top.thexiaola.dreamhwhub.enums;

import lombok.Getter;

/**
 * 业务错误码枚举
 */
@Getter
public enum BusinessErrorCode {
    // 成功
    SUCCESS(0, "操作成功"),
    
    // 验证码相关错误
    VERIFICATION_CODE_INVALID(1001, "验证码无效"),
    VERIFICATION_CODE_EXPIRED(1002, "验证码已过期"),
    VERIFICATION_CODE_NOT_FOUND(1003, "验证码不存在"),
    
    // 注册相关错误
    USERNAME_EXISTS(2002, "用户名已被占用"),
    EMAIL_EXISTS(2003, "邮箱已被占用"),
    REGISTRATION_FAILED(2004, "注册失败"),
    
    // 登录相关错误
    USER_NOT_FOUND(3001, "用户不存在"),
    INVALID_CREDENTIALS(3002, "账号或密码错误"),
    USER_BANNED(3003, "用户已被封禁"),
    USER_NOT_LOGGED_IN(3004, "用户未登录"),
    /** 账号注销受阻：仍持有平台管理员/学校管理员或班级创建者等身份，需先解除 */
    ACCOUNT_DELETION_FORBIDDEN(3005, "账号注销受阻"),
    
    // 邮件相关错误
    EMAIL_SEND_FAILED(4001, "邮件发送失败"),
    EMAIL_SERVER_NOT_CONFIGURED(4003, "邮件服务器未配置"),
    EMAIL_SENDING_FAILED(4004, "邮件发送失败"),
    
    // 系统错误
    SYSTEM_ERROR(5000, "系统错误"),
    DATABASE_ERROR(5001, "数据库操作失败"),
    
    // 修改邮箱错误
    SAME_EMAIL(6003, "新邮箱不能与原邮箱相同"),
    
    // 修改密码错误
    INVALID_OLD_PASSWORD(6004, "原密码错误"),
    NEW_PASSWORD_SAME_AS_OLD(6005, "新密码不能与原密码相同"),

    // 敏感操作二次验证错误
    /** 未提供二次验证凭据（平台密码或邮箱验证码） */
    OPERATION_VERIFICATION_REQUIRED(6100, "该操作需要验证身份，请提供登录密码或邮箱验证码"),
    /** 二次验证凭据错误（密码错误或验证码无效） */
    OPERATION_VERIFICATION_FAILED(6101, "身份验证失败，请检查登录密码或邮箱验证码"),
    /** 二次验证方式不受支持 */
    OPERATION_VERIFICATION_METHOD_UNSUPPORTED(6102, "不支持的身份验证方式"),
    /** 验证方式与用户当前的安全设置不符（如用户已关闭密码验证却提交了密码） */
    OPERATION_VERIFICATION_METHOD_DISABLED(6103, "该验证方式已被关闭，请使用其他方式"),
    /** 安全验证设置不合法 */
    SECURITY_VERIFICATION_SETTING_INVALID(6104, "安全验证设置不合法"),
    /** 目标敏感操作标识未登记或对该用户不可用 */
    SENSITIVE_OPERATION_NOT_AVAILABLE(6105, "该操作对你的账号不可用，无法配置其验证开关"),
    
    // 作业相关错误
    WORK_NOT_FOUND(7001, "作业不存在"),
    WORK_STATUS_ERROR(7002, "作业状态错误"),
    WORK_ALREADY_SUBMITTED(7003, "已经提交过该作业"),
    SUBMISSION_NOT_FOUND(7004, "提交记录不存在"),
    SUBMISSION_ALREADY_GRADED(7005, "作业已被批改，不能修改"),
    SCORE_OUT_OF_RANGE(7006, "分数超过作业总分"),
    /** 题目不存在 */
    QUESTION_NOT_FOUND(7007, "题目不存在"),
    /** 题型不合法 */
    QUESTION_TYPE_INVALID(7008, "题型不合法"),
    /** 作答与题目不匹配或缺失 */
    ANSWER_INVALID(7009, "作答不完整或与题目不匹配"),
    /** 考试不存在或未开启 */
    EXAM_NOT_FOUND(7010, "考试不存在"),
    /** 考试会话不存在（尚未开考） */
    EXAM_SESSION_NOT_FOUND(7011, "考试尚未开始，请先进入考试"),
    /** 考试会话已结束（已交卷或超时） */
    EXAM_SESSION_FINISHED(7012, "考试已结束"),
    /** 考试时长已用尽 */
    EXAM_TIME_UP(7013, "考试时间已到"),
    /** 考试状态不允许当前操作 */
    EXAM_STATUS_ERROR(7014, "考试状态不允许该操作"),
    /** 非法违规类型 */
    EXAM_VIOLATION_INVALID(7015, "违规类型不合法"),
    
    // 文件上传相关错误
    FILE_UPLOAD_FAILED(8001, "文件上传失败"),
    FILE_TYPE_NOT_ALLOWED(8002, "不允许的文件类型"),
    FILE_SIZE_EXCEEDED(8003, "文件大小超过限制"),
    FILE_IS_INFECTED(8004, "文件可能包含病毒"),
    INVALID_FILE_PATH(8005, "非法的文件路径"),
    
    // 班级管理相关错误
    CLASS_NOT_FOUND(8501, "班级不存在"),
    CLASS_DISSOLVED(8502, "班级已解散"),
    ALREADY_IN_CLASS(8503, "你已经在该班级中"),
    NOT_IN_CLASS(8504, "你不是该班级的成员"),
    CREATOR_CANNOT_LEAVE(8505, "创建者不能退出班级"),
    DUPLICATE_STUDENT_NO(8506, "该学号在班级中已被占用"),
    /** 班级已冻结：创建者教师身份被解除，暂不接受管理操作与新成员加入 */
    CLASS_FROZEN(8507, "该班级的老师已失去教师身份，班级暂不可管理"),
    /** 仅班级已冻结时才可申请接管 */
    TAKEOVER_NOT_ALLOWED(8508, "该班级当前无需接管"),
    /** 已存在待处理的接管申请 */
    TAKEOVER_ALREADY_APPLIED(8509, "你已提交过接管申请，请等待处理"),

    // 学校管理相关错误
    SCHOOL_NOT_FOUND(8601, "学校不存在"),
    SCHOOL_NAME_EXISTS(8602, "学校名称已被占用"),
    ALREADY_IN_SCHOOL(8603, "你已经在该学校中"),
    NOT_IN_SCHOOL(8604, "你不是该学校的成员"),
    DUPLICATE_STAFF_NO(8605, "该学工号在该学校已被占用"),
    SCHOOL_HAS_CLASSES(8606, "学校下仍存在班级，请先处理这些班级"),
    NOT_SCHOOL_TEACHER(8607, "只有学校老师才能创建班级"),

    // 好友与私信相关错误
    /** 只能添加同一学校内的用户为好友 */
    FRIEND_SCHOOL_MISMATCH(8701, "只能添加同一学校内的用户为好友"),
    /** 不能添加自己为好友 */
    FRIEND_SELF(8702, "不能添加自己为好友"),
    /** 已是好友 */
    FRIEND_ALREADY(8703, "你们已经是好友"),
    /** 已存在待处理的申请 */
    FRIEND_REQUEST_PENDING(8704, "已存在待处理的好友申请"),
    /** 好友关系不存在或无权操作 */
    FRIEND_NOT_FOUND(8705, "好友关系不存在"),
    /** 私信会话双方须在同一学校 */
    PRIVATE_MESSAGE_SCHOOL_MISMATCH(8706, "只能给同一学校内的用户发送私信"),
    /** 陌生人私信条数已达上限 */
    STRANGER_MESSAGE_LIMIT_EXCEEDED(8707, "向陌生用户发送私信的条数已达上限，请等待重置或先添加好友"),
    /** 不能给自己发送私信 */
    PRIVATE_MESSAGE_SELF(8708, "不能给自己发送私信"),
    
    // 参数相关错误
    PARAMETER_MISSING(9002, "缺少必要参数"),
    PARAMETER_ERROR(9003, "参数错误"),
    DUPLICATE_APPLICATION(9004, "已有待处理的申请"),
    ALREADY_MEMBER(9005, "已经是班级成员"),
    
    PERMISSION_DENIED(9001, "权限不足");

    private final int code;
    private final String message;

    BusinessErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
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
}