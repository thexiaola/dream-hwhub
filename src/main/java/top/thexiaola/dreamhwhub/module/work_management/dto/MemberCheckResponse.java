package top.thexiaola.dreamhwhub.module.work_management.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 成员检查结果响应
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
     * 角色（TEACHER 或 STUDENT），非成员时为 null
     */
    private String role;
}
