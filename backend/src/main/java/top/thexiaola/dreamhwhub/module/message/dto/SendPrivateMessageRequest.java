package top.thexiaola.dreamhwhub.module.message.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 发送私信请求（按学校隔离会话）
 */
@Data
public class SendPrivateMessageRequest {

    /**
     * 学校 ID（私信按学校隔离）
     */
    @NotNull(message = "请选择学校")
    private Integer schoolId;

    /**
     * 接收人用户 ID
     */
    @NotNull(message = "请选择接收人")
    private Integer receiverId;

    /**
     * 消息内容
     */
    @NotBlank(message = "消息内容不能为空")
    @Size(max = 1000, message = "消息内容长度不能超过 1000 字")
    private String content;
}
