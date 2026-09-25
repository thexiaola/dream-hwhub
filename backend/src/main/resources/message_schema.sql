CREATE DATABASE IF NOT EXISTS dream_hwhub CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE dream_hwhub;

-- 站内信表
-- 系统内事件（如班级布置了新作业）产生的通知，按学校隔离：同一用户在
-- 不同学校看到各自的站内信。属个人消息，无需权限节点，仅接收人本人可读。
CREATE TABLE IF NOT EXISTS `site_message` (
    `id` INT AUTO_INCREMENT PRIMARY KEY COMMENT '消息ID',
    `recipient_id` INT NOT NULL COMMENT '接收人ID',
    `school_id` INT NOT NULL COMMENT '所属学校ID（站内信按学校隔离）',
    `type` VARCHAR(32) NOT NULL COMMENT '消息类型，如 work_published',
    `title` VARCHAR(200) NOT NULL COMMENT '消息标题',
    `content` VARCHAR(500) DEFAULT NULL COMMENT '消息内容',
    `class_id` INT DEFAULT NULL COMMENT '关联班级ID',
    `class_name` VARCHAR(100) DEFAULT NULL COMMENT '关联班级名称（冗余，便于展示）',
    `work_id` INT DEFAULT NULL COMMENT '关联作业ID',
    `is_read` BIT(1) NOT NULL DEFAULT b'0' COMMENT '是否已读：0-未读，1-已读',
    `read_time` DATETIME DEFAULT NULL COMMENT '阅读时间',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_recipient_school (`recipient_id`, `school_id`),
    INDEX idx_recipient_read (`recipient_id`, `is_read`),
    INDEX idx_school_type (`school_id`, `type`),
    INDEX idx_create_time (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='站内信表';
