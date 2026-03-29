package top.thexiaola.dreamhwhub.module.work_management.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 班级详情响应
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
     * 创建者 ID
     */
    private Integer creatorId;

    /**
     * 创建者姓名
     */
    private String creatorName;

    /**
     * 班级状态（1-正常，2-已解散）
     */
    private Integer status;

    /**
     * 用户在该班级的角色（TEACHER 或 STUDENT）
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
