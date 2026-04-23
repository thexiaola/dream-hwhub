package top.thexiaola.dreamhwhub.module.work_management.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 成员检查结果响应VO
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class MemberCheckResponse {

    /**
     * 是否是班级成员
     */
    private boolean isMember;

    /**
     * 角色代码（用于前端权限判断）：1-创建者，2-班级助理，3-学生，null-非成员
     */
    private Integer roleCode;

    /**
     * 角色名称（用于展示）：创建者/班级助理/学生，非成员时为 null
     */
    private String roleName;
}
