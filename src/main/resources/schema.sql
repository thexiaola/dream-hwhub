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
CREATE INDEX idx_user_no ON user(user_no);
CREATE INDEX idx_email ON user(email);