package top.thexiaola.dreamhwhub.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName(value = "invitation_code")
public class InvitationCode {
    @TableId(type = IdType.AUTO)
    private Integer id;
    
    @TableField(value = "code")
    private String code;
    
    @TableField(value = "creator_id")
    private Integer creatorId;
    
    @TableField(value = "used_count")
    private Integer usedCount;
    
    @TableField(value = "max_usage")
    private Integer maxUsage = 1;
    
    @TableField(value = "created_time")
    private LocalDateTime createdTime;
    
    @TableField(value = "expire_time")
    private LocalDateTime expireTime;
    
    @TableField(value = "is_active")
    private Boolean isActive;

    // 关联的创建者用户信息（非数据库字段）
    @TableField(exist = false)
    private User creator;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public Integer getCreatorId() {
        return creatorId;
    }

    public void setCreatorId(Integer creatorId) {
        this.creatorId = creatorId;
    }

    public Integer getUsedCount() {
        return usedCount;
    }

    public void setUsedCount(Integer usedCount) {
        this.usedCount = usedCount;
    }

    public Integer getMaxUsage() {
        return maxUsage;
    }

    public void setMaxUsage(Integer maxUsage) {
        this.maxUsage = maxUsage;
    }

    public LocalDateTime getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(LocalDateTime createdTime) {
        this.createdTime = createdTime;
    }

    public LocalDateTime getExpireTime() {
        return expireTime;
    }

    public void setExpireTime(LocalDateTime expireTime) {
        this.expireTime = expireTime;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public User getCreator() {
        return creator;
    }

    public void setCreator(User creator) {
        this.creator = creator;
    }
}