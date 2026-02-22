CREATE DATABASE IF NOT EXISTS dream_hwhub CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE dream_hwhub;

CREATE TABLE IF NOT EXISTS user (
    id INT NOT NULL AUTO_INCREMENT COMMENT '用户编号',
    user_no VARCHAR(24) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '学号/工号',
    username VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '用户名',
    email VARCHAR(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '邮箱',
    password BLOB NOT NULL COMMENT '密码',
    permission SMALLINT NOT NULL DEFAULT 1 COMMENT '权限级别',
    PRIMARY KEY (id) USING BTREE,
    UNIQUE INDEX id(id ASC) USING BTREE,
    UNIQUE INDEX user_no(user_no ASC) USING BTREE,
    UNIQUE INDEX email(email ASC) USING BTREE,
    INDEX idx_user_no(user_no ASC) USING BTREE,
    INDEX idx_email(email ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '用户表' ROW_FORMAT = Dynamic;