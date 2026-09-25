package top.thexiaola.dreamhwhub.module.message.service;

import top.thexiaola.dreamhwhub.module.message.vo.MessagePolicyInfo;

/**
 * 私信策略服务接口
 * <p>
 * 管理「向陌生用户发送私信」的条数上限与重置小时数：
 * 全站默认由平台管理员设置；学校可由学校管理员独立覆盖（未覆盖则继承全站默认）。
 */
public interface MessagePolicyService {

    /**
     * 读取全站默认策略（不存在时返回内置默认值，不落库）
     *
     * @return 全站策略
     */
    MessagePolicyInfo getGlobalPolicy();

    /**
     * 平台管理员修改全站默认策略
     *
     * @param strangerLimit 向陌生用户发送私信的条数上限
     * @param resetHours    配额重置小时数
     * @return 更新后的全站策略
     */
    MessagePolicyInfo updateGlobalPolicy(Integer strangerLimit, Integer resetHours);

    /**
     * 读取某学校生效的策略（学校覆盖优先，否则继承全站默认）
     *
     * @param schoolId 学校 ID
     * @return 生效策略
     */
    MessagePolicyInfo getSchoolPolicy(Integer schoolId);

    /**
     * 学校管理员设置该校覆盖策略（传 null 表示清除覆盖、继承全站默认）
     *
     * @param schoolId      学校 ID
     * @param strangerLimit 条数上限，可为 null
     * @param resetHours    重置小时数，可为 null
     * @return 设置后的生效策略
     */
    MessagePolicyInfo updateSchoolPolicy(Integer schoolId, Integer strangerLimit, Integer resetHours);

    /**
     * 解析某学校实际生效的「陌生私信条数上限」
     *
     * @param schoolId 学校 ID
     * @return 条数上限
     */
    int resolveStrangerLimit(Integer schoolId);

    /**
     * 解析某学校实际生效的「配额重置小时数」
     *
     * @param schoolId 学校 ID
     * @return 重置小时数
     */
    int resolveResetHours(Integer schoolId);
}
