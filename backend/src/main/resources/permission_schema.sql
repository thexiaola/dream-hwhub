CREATE DATABASE IF NOT EXISTS dream_hwhub CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE dream_hwhub;

-- 权限组表（LuckPerms 风格的权限组，一组权限节点的集合）
CREATE TABLE IF NOT EXISTS `permission_group` (
    `id` INT AUTO_INCREMENT PRIMARY KEY COMMENT '权限组ID',
    `code` VARCHAR(64) NOT NULL COMMENT '权限组标识（唯一，如 class-admin）',
    `name` VARCHAR(64) NOT NULL COMMENT '权限组名称',
    `description` VARCHAR(255) DEFAULT NULL COMMENT '权限组描述',
    `is_default` BIT(1) NOT NULL DEFAULT b'0' COMMENT '是否为新用户默认加入：0-否，1-是',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_group_code (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='权限组表';

-- 权限组-权限节点关联表（组拥有的权限节点）
CREATE TABLE IF NOT EXISTS `permission_group_node` (
    `id` INT AUTO_INCREMENT PRIMARY KEY COMMENT '关联ID',
    `group_id` INT NOT NULL COMMENT '权限组ID',
    `node` VARCHAR(128) NOT NULL COMMENT '权限节点，如 user:add',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_group_id (`group_id`),
    UNIQUE KEY uk_group_node (`group_id`, `node`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='权限组-权限节点关联表';

-- 用户-权限组关联表（用户所属的权限组）
CREATE TABLE IF NOT EXISTS `user_permission_group` (
    `id` INT AUTO_INCREMENT PRIMARY KEY COMMENT '关联ID',
    `user_id` INT NOT NULL COMMENT '用户ID',
    `group_id` INT NOT NULL COMMENT '权限组ID',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_user_id (`user_id`),
    INDEX idx_group_id (`group_id`),
    UNIQUE KEY uk_user_group (`user_id`, `group_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户-权限组关联表';

-- 用户直接权限节点表（在权限组之外单独授予用户的权限节点）
CREATE TABLE IF NOT EXISTS `user_permission_node` (
    `id` INT AUTO_INCREMENT PRIMARY KEY COMMENT '关联ID',
    `user_id` INT NOT NULL COMMENT '用户ID',
    `node` VARCHAR(128) NOT NULL COMMENT '权限节点，如 user:add',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_user_id (`user_id`),
    UNIQUE KEY uk_user_node (`user_id`, `node`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户直接权限节点表';
