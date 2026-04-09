package top.thexiaola.dreamhwhub.module.work_management.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 班级申请响应VO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClassApplicationResponse {

    /**
     * 申请 ID
     */
    private Integer id;

    /**
     * 申请类型：1-创建班级申请，2-加入班级申请
     */
    private Integer type;

    /**
     * 班级 ID（加入班级申请时有值）
     */
    private Integer classId;

    /**
     * 申请人 ID
     */
    private Integer applicantId;

    /**
     * 班级名称（创建班级申请时有值）
     */
    private String className;

    /**
     * 班级描述（创建班级申请时有值）
     */
    private String description;

    /**
     * 申请状态：0-待审核，1-已通过，2-已拒绝
     */
    private Integer status;

    /**
     * 申请时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;
}
