package top.thexiaola.dreamhwhub.module.work_management.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 邀请响应VO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class InvitationResponse {

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
     * 邀请人 ID
     */
    private Integer inviterId;

    /**
     * 邀请人姓名
     */
    private String inviterName;

    /**
     * 被邀请人 ID
     */
    private Integer inviteeUserId;

    /**
     * 邀请状态：0-待处理，1-已同意，2-已拒绝
     */
    private Integer status;

    /**
     * 响应时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime responseTime;

    /**
     * 邀请时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;
}
