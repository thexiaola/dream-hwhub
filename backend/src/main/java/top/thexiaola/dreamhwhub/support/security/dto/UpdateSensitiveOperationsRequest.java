package top.thexiaola.dreamhwhub.support.security.dto;

import lombok.Data;

import java.util.List;

/**
 * 更新「哪些敏感操作需要二次验证」的请求
 * <p>
 * 仅提交用户当前可见（对其可用）的操作的开关状态；后端会校验每个操作对当前用户是否可用，
 * 不可用的操作会被拒绝，避免越权配置。
 */
@Data
public class UpdateSensitiveOperationsRequest {

    /** 逐项设置 */
    private List<Item> settings;

    /**
     * 单个操作的开关目标值
     */
    @Data
    public static class Item {

        /** 操作标识，如 class.dissolve */
        private String key;

        /** 目标：是否需要二次验证（true-需要，false-关闭） */
        private Boolean enabled;
    }
}
