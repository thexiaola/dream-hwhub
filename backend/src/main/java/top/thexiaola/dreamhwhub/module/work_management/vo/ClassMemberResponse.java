package top.thexiaola.dreamhwhub.module.work_management.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 班级成员响应VO
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
     * 角色（OWNER/ASSISTANT/STUDENT）
     */
    private String role;

    /**
     * 加入时间
     */
    private java.time.LocalDateTime joinTime;
}
