package top.thexiaola.dreamhwhub.support.security.vo;

/**
 * 单个敏感操作的验证设置（供「危险操作验证」页签渲染）
 */
public class SensitiveOperationSettingVO {

    /** 操作标识（如 class.dissolve） */
    private String key;

    /** 操作名称（如 解散班级） */
    private String name;

    /** 操作说明 */
    private String description;

    /** 是否要求二次验证：true-需要，false-已关闭（默认 true） */
    private boolean enabled;

    /** 当前用户是否可用该操作（不可用则前端不展示其开关） */
    private boolean available;

    public SensitiveOperationSettingVO() {
    }

    public SensitiveOperationSettingVO(String key, String name, String description, boolean enabled, boolean available) {
        this.key = key;
        this.name = name;
        this.description = description;
        this.enabled = enabled;
        this.available = available;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }
}
