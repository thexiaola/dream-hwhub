package top.thexiaola.dreamhwhub.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * MyBatis-Plus 自动填充处理器
 * 配合实体类 @TableField(fill = FieldFill.INSERT) / FieldFill.INSERT_UPDATE 使用
 */
@Component
public class MyMetaObjectHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        // 插入时填充 createTime（若为空）
        Object createTime = getFieldValByName("createTime", metaObject);
        if (createTime == null) {
            strictInsertFill(metaObject, "createTime", LocalDateTime.class, LocalDateTime.now());
        }
        // 插入时填充 updateTime（若为空）
        Object updateTime = getFieldValByName("updateTime", metaObject);
        if (updateTime == null) {
            strictInsertFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
        }
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        // 更新时填充 updateTime
        strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
    }
}
