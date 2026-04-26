package top.thexiaola.dreamhwhub.module.work_management.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 班级邀请申请响应VO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class InviteApplicationResponse {

    /**
     * 邀请申请 ID
     */
    private Integer id;

    /**
     * 班级 ID
     */
    private Integer classId;

    /**
     * 邀请人 ID（学生）
     */
    private Integer inviterId;

    /**
     * 被邀请人 ID
     */
    private Integer inviteeId;

    /**
     * 被邀请人用户名
     */
    private String inviteeUsername;

    /**
     * 审核状态：0-待审核，1-已通过，2-已拒绝
     */
    private Integer status;

    /**
     * 审核人 ID
     */
    private Integer reviewerId;

    /**
     * 审核时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime reviewTime;

    /**
     * 审核意见
     */
    private String reviewComment;

    /**
     * 邀请时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;
}
