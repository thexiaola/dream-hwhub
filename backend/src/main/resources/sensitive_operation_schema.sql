CREATE DATABASE IF NOT EXISTS dream_hwhub CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE dream_hwhub;

-- 用户级敏感操作验证设置表
-- 采用「只存关闭项」模型：默认所有可用操作都要求二次验证；用户关闭某操作才插入一行，
-- 重新开启即删除该行；用户失去某操作权限时删除其行（自动重置为默认「启用验证」）。
CREATE TABLE IF NOT EXISTS `user_sensitive_operation` (
    `id` INT AUTO_INCREMENT PRIMARY KEY COMMENT '记录ID',
    `user_id` INT NOT NULL COMMENT '用户ID',
    `operation_key` VARCHAR(64) NOT NULL COMMENT '已关闭二次验证的敏感操作标识，如 class.dissolve',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_user_id (`user_id`),
    UNIQUE KEY uk_user_operation (`user_id`, `operation_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户级敏感操作验证设置表';
