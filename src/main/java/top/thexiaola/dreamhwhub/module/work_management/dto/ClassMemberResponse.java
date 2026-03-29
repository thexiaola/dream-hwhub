package top.thexiaola.dreamhwhub.module.work_management.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 班级成员响应
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ClassMemberResponse {

    /**
     * 成员 ID
     */
    private Integer id;

    /**
     * 用户 ID
     */
    private Integer userId;

    /**
     * 用户姓名
     */
    private String userName;

    /**
     * 学号/工号
     */
    private String userNo;

    /**
     * 角色（TEACHER 或 STUDENT）
     */
    private String role;

    /**
     * 加入时间
     */
    private java.time.LocalDateTime joinTime;
}
