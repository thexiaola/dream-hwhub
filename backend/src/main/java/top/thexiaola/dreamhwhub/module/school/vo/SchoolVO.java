package top.thexiaola.dreamhwhub.module.school.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 学校简要信息 VO
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SchoolVO {

    /**
     * 学校 ID
     */
    private Integer id;

    /**
     * 学校名称
     */
    private String schoolName;

    /**
     * 学校描述
     */
    private String description;

    /**
     * 加入学校是否免审核：true-填入学工号与姓名后直接加入
     */
    private Boolean allowJoinWithoutApproval;

    /**
     * 成员总数
     */
    private Long memberCount;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
