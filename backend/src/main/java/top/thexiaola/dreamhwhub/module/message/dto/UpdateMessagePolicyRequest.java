package top.thexiaola.dreamhwhub.module.message.dto;

import lombok.Data;

/**
 * 修改私信策略请求
 * <p>
 * 用于两处：平台管理员改全站默认（两项都必填）；
 * 学校管理员设本校覆盖（两项传 null 表示清除覆盖、继承全站默认）。
 */
@Data
public class UpdateMessagePolicyRequest {

    /**
     * 向陌生用户发送私信的条数上限；学校覆盖场景传 null 表示继承全站默认
     */
    private Integer strangerLimit;

    /**
     * 配额重置小时数；学校覆盖场景传 null 表示继承全站默认
     */
    private Integer resetHours;
}
