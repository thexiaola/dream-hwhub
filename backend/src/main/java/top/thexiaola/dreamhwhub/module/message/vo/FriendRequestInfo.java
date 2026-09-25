package top.thexiaola.dreamhwhub.module.message.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 好友申请 VO（我收到的、待我处理的申请）
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class FriendRequestInfo {

    /**
     * 好友关系 ID
     */
    private Integer relationId;

    /**
     * 所属学校 ID
     */
    private Integer schoolId;

    /**
     * 学校名称
     */
    private String schoolName;

    /**
     * 申请人用户 ID
     */
    private Integer requesterId;

    /**
     * 申请人用户名
     */
    private String requesterUsername;

    /**
     * 申请人在该校的姓名
     */
    private String requesterRealName;

    /**
     * 申请人在该校的学工号
     */
    private String requesterStaffNo;

    /**
     * 申请人头像
     */
    private String requesterAvatar;

    /**
     * 发起时间
     */
    private LocalDateTime createTime;
}
