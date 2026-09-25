package top.thexiaola.dreamhwhub.module.message.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 私信策略 VO
 * <p>
 * 描述「向陌生用户发送私信」的条数上限与重置小时数。
 * 学校未覆盖时其生效值即全站默认；同时携带覆盖标记，便于前端展示。
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class MessagePolicyInfo {

    /**
     * 生效的条数上限
     */
    private Integer strangerLimit;

    /**
     * 生效的重置小时数
     */
    private Integer resetHours;

    /**
     * 学校是否自定义覆盖（全站策略恒为 false）
     */
    private Boolean schoolOverridden;

    /**
     * 全站默认条数上限（便于学校设置页展示继承值）
     */
    private Integer globalStrangerLimit;

    /**
     * 全站默认重置小时数
     */
    private Integer globalResetHours;
}
