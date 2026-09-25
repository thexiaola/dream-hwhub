CREATE DATABASE IF NOT EXISTS dream_hwhub CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE dream_hwhub;

-- 好友关系表
-- 好友按学校隔离：同一用户在不同学校拥有各自的好友列表。
-- 建立好友需先发起申请、对方同意（status=1）。
CREATE TABLE IF NOT EXISTS `user_friend` (
    `id` INT AUTO_INCREMENT PRIMARY KEY COMMENT '关系ID',
    `school_id` INT NOT NULL COMMENT '所属学校ID（好友按学校隔离）',
    `requester_id` INT NOT NULL COMMENT '发起人ID',
    `addressee_id` INT NOT NULL COMMENT '接收人ID',
    `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态：0-待确认，1-已接受，2-已拒绝',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发起时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_school_requester (`school_id`, `requester_id`, `status`),
    INDEX idx_school_addressee (`school_id`, `addressee_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='好友关系表';

-- 私信表
-- 私信按学校隔离：会话双方须在同一学校。非好友也可互发（受陌生私信配额约束）。
CREATE TABLE IF NOT EXISTS `private_message` (
    `id` INT AUTO_INCREMENT PRIMARY KEY COMMENT '私信ID',
    `school_id` INT NOT NULL COMMENT '所属学校ID（私信按学校隔离）',
    `sender_id` INT NOT NULL COMMENT '发送人ID',
    `receiver_id` INT NOT NULL COMMENT '接收人ID',
    `content` VARCHAR(1000) NOT NULL COMMENT '消息内容',
    `is_read` BIT(1) NOT NULL DEFAULT b'0' COMMENT '是否已读：0-未读，1-已读',
    `read_time` DATETIME DEFAULT NULL COMMENT '阅读时间',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发送时间',
    INDEX idx_school_sender (`school_id`, `sender_id`, `id`),
    INDEX idx_school_receiver (`school_id`, `receiver_id`, `id`),
    INDEX idx_receiver_unread (`school_id`, `receiver_id`, `is_read`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='私信表';

-- 陌生私信配额表
-- 记录「发送人 → 陌生接收人」在某学校内的已用条数与窗口起点。
-- 到达重置时间（窗口起点 + reset_hours）后计数归零，可继续发送。
CREATE TABLE IF NOT EXISTS `stranger_message_quota` (
    `id` INT AUTO_INCREMENT PRIMARY KEY COMMENT '配额ID',
    `school_id` INT NOT NULL COMMENT '所属学校ID',
    `sender_id` INT NOT NULL COMMENT '发送人ID',
    `receiver_id` INT NOT NULL COMMENT '接收人ID（陌生人）',
    `used_count` INT NOT NULL DEFAULT 0 COMMENT '当前窗口内已发送条数',
    `window_start` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '当前窗口起点',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_quota (`school_id`, `sender_id`, `receiver_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='陌生私信配额表';

-- 全站私信策略表（单行，id 恒为 1）
-- 平台管理员可修改全站默认：向陌生用户发送私信的条数上限与重置小时数。
-- 学校可在 school 表上覆盖这两项（为空表示继承全站默认）。
CREATE TABLE IF NOT EXISTS `message_policy` (
    `id` INT PRIMARY KEY COMMENT '策略ID（恒为1）',
    `stranger_limit` INT NOT NULL DEFAULT 3 COMMENT '向陌生用户发送私信的条数上限',
    `reset_hours` INT NOT NULL DEFAULT 24 COMMENT '配额重置小时数（距窗口起点超过该时长后可继续发送）',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='全站私信策略表';
