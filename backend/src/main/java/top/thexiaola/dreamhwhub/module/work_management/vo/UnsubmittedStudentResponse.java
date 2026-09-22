package top.thexiaola.dreamhwhub.module.work_management.vo;

import lombok.Data;

/**
 * 作业未交学生响应（仅含展示所需基本信息，严禁携带 password 等敏感字段）
 */
@Data
public class UnsubmittedStudentResponse {

    /**
     * 用户 ID
     */
    private Integer id;

    /**
     * 用户名
     */
    private String username;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 班级内姓名（学生入班时填写，可能为空）
     */
    private String studentName;

    /**
     * 班级内学号（学生入班时填写，可能为空）
     */
    private String studentNo;
}
