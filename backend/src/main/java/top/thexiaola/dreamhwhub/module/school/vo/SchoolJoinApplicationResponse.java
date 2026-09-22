package top.thexiaola.dreamhwhub.module.school.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 学校加入申请响应 VO
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SchoolJoinApplicationResponse {

    /**
     * 申请 ID
     */
    private Integer id;

    /**
     * 学校 ID
     */
    private Integer schoolId;

    /**
     * 学校名称
     */
    private String schoolName;

    /**
     * 申请人 ID
     */
    private Integer applicantId;

    /**
     * 申请人用户名
     */
    private String applicantUsername;

    /**
     * 申请人填写的姓名
     */
    private String applicantName;

    /**
     * 申请人填写的学工号
     */
    private String applicantNo;

    /**
     * 审核状态：0-待审核，1-已通过，2-已拒绝
     */
    private Integer status;

    /**
     * 审核人 ID
     */
    private Integer reviewerId;

    /**
     * 审核人用户名
     */
    private String reviewerName;

    /**
     * 审核意见
     */
    private String reviewComment;

    /**
     * 申请时间
     */
    private LocalDateTime createTime;

    /**
     * 审核时间
     */
    private LocalDateTime reviewTime;
}
