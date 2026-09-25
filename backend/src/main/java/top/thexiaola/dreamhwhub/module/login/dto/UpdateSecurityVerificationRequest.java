package top.thexiaola.dreamhwhub.module.login.dto;

import lombok.Data;

/**
 * 更新危险操作安全验证设置请求。
 * <p>
 * 与「查询」不同，更新时**每个被改动的开关都必须用该方式自身的凭据验证身份**：
 * <ul>
 *   <li>改动了「密码验证」开关（开或关）→ 必须携带正确的登录密码；</li>
 *   <li>改动了「邮箱验证码验证」开关（开或关）→ 必须携带发往绑定邮箱的验证码；</li>
 *   <li>两个开关都改动 → 两种凭据都要提供；</li>
 *   <li>开关未变动的方式无需提供凭据。</li>
 * </ul>
 */
@Data
public class UpdateSecurityVerificationRequest {

    /** 目标：是否启用密码验证 */
    private Boolean verifyByPassword;

    /** 目标：是否启用邮箱验证码验证 */
    private Boolean verifyByEmailCode;

    /** 登录密码（改动了「密码验证」开关时必填） */
    private String password;

    /** 邮箱验证码（改动了「邮箱验证码验证」开关时必填） */
    private String emailCode;
}
