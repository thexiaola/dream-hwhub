package top.thexiaola.dreamhwhub.module.work_management.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 用户邀请响应VO（学生发起的邀请）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserInvitationResponse {

    /**
     * 邀请 ID
     */
    private Integer id;

    /**
     * 班级 ID
     */
    private Integer classId;

    /**
     * 班级名称
     */
    private String className;

    /**
     * 邀请人 ID（学生）
     */
    private Integer inviterId;

    /**
     * 邀请人用户名
     */
    private String inviterUsername;

    /**
     * 被邀请人 ID
     */
    private Integer inviteeId;

    /**
     * 被邀请人用户名
     */
    private String inviteeUsername;

    /**
     * 用户确认状态：0-待确认，1-已同意，2-已拒绝
     */
    private Integer status;

    /**
     * 用户响应时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime responseTime;

    /**
     * 用户回复说明
     */
    private String responseComment;

    /**
     * 邀请时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;
}
