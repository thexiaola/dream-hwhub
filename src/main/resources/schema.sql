CREATE DATABASE IF NOT EXISTS dream_hwhub CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE dream_hwhub;

CREATE TABLE IF NOT EXISTS user (
    id INT AUTO_INCREMENT PRIMARY KEY UNIQUE COMMENT '用户编号',
    user_no VARCHAR(50) NOT NULL UNIQUE COMMENT '学号/工号',
    username VARCHAR(100) NOT NULL COMMENT '用户名',
    email VARCHAR(100) NOT NULL UNIQUE COMMENT '邮箱',
    password VARCHAR(255) NOT NULL COMMENT '密码',
    permission SMALLINT DEFAULT 0 COMMENT '权限级别'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- 创建索引以提高查询性能
CREATE INDEX IF NOT EXISTS idx_user_no ON user(user_no);
CREATE INDEX IF NOT EXISTS idx_email ON user(email);

-- 创建邀请码表
CREATE TABLE IF NOT EXISTS invitation_code (
    id INT AUTO_INCREMENT PRIMARY KEY UNIQUE COMMENT '邀请码ID',
    code VARCHAR(50) NOT NULL UNIQUE COMMENT '邀请码',
    creator_id INT NOT NULL COMMENT '创建者ID',
    used_count INT DEFAULT 0 COMMENT '已使用次数',
    max_usage INT DEFAULT 1 COMMENT '最大使用次数',
    created_time DATETIME NOT NULL COMMENT '创建时间',
    expire_time DATETIME NOT NULL COMMENT '过期时间',
    is_active BOOLEAN DEFAULT TRUE COMMENT '是否激活',
    FOREIGN KEY (creator_id) REFERENCES user(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='邀请码表';

-- 创建邀请码相关索引
CREATE INDEX IF NOT EXISTS idx_invitation_code ON invitation_code(code);
CREATE INDEX IF NOT EXISTS idx_creator_id ON invitation_code(creator_id);
CREATE INDEX IF NOT EXISTS idx_expire_time ON invitation_code(expire_time);