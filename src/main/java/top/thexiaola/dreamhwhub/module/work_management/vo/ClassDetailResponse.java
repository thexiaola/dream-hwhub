package top.thexiaola.dreamhwhub.module.work_management.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 班级详情响应VO
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ClassDetailResponse {

    /**
     * 班级 ID
     */
    private Integer id;

    /**
     * 班级名称
     */
    private String className;

    /**
     * 班级所有者 ID
     */
    private Integer ownerId;

    /**
     * 班级所有者姓名
     */
    private String ownerName;

    /**
     * 用户在该班级的角色（OWNER/ASSISTANT/STUDENT）
     */
    private String userRole;

    /**
     * 成员总数
     */
    private Long memberCount;

    /**
     * 教师数量
     */
    private Long teacherCount;

    /**
     * 学生数量
     */
    private Long studentCount;
}
