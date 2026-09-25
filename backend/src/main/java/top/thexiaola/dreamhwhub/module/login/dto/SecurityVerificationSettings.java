package top.thexiaola.dreamhwhub.module.login.dto;

import lombok.Data;

/**
 * 危险操作安全验证设置（查询响应 / 更新请求共用）
 * <p>
 * 两个开关的组合决定危险操作的验证方式：
 * <ol>
 *   <li>都关闭 → 关闭危险操作验证（不验证）；</li>
 *   <li>仅邮箱验证码 → 只进行邮箱验证码验证；</li>
 *   <li>仅密码 → 只进行密码验证；</li>
 *   <li>都开启 → 两种方式均可用。</li>
 * </ol>
 */
@Data
public class SecurityVerificationSettings {

    /** 是否启用密码验证 */
    private Boolean verifyByPassword;

    /** 是否启用邮箱验证码验证 */
    private Boolean verifyByEmailCode;
}
