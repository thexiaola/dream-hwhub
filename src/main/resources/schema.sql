CREATE DATABASE IF NOT EXISTS dream_hwhub CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE dream_hwhub;

CREATE TABLE IF NOT EXISTS user (
    id INT NOT NULL AUTO_INCREMENT COMMENT '用户编号',
    user_no VARCHAR(24) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '学号/工号',
    username VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '用户名',
    email VARCHAR(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '邮箱',
    password VARCHAR(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '密码',
    permission SMALLINT NOT NULL DEFAULT 1 COMMENT '权限级别',
    is_banned BIT(1) NOT NULL DEFAULT 0 COMMENT '是否被封禁：0-正常，1-封禁',
    register_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '注册时间',
    last_login_time DATETIME DEFAULT NULL COMMENT '最后登录时间',
    PRIMARY KEY (id) USING BTREE,
    UNIQUE INDEX uk_user_no(user_no ASC) USING BTREE COMMENT '学号唯一索引',
    UNIQUE INDEX uk_username(username ASC) USING BTREE COMMENT '用户名唯一索引',
    UNIQUE INDEX uk_email(email ASC) USING BTREE COMMENT '邮箱唯一索引',
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '用户表' ROW_FORMAT = Dynamic;