CREATE DATABASE IF NOT EXISTS dream_hwhub CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE dream_hwhub;

-- 学校表
-- 学校由平台管理员（OP）在管理面板创建，学校管理员由平台管理员指派
CREATE TABLE IF NOT EXISTS `school` (
    `id` INT AUTO_INCREMENT PRIMARY KEY COMMENT '学校ID',
    `school_name` VARCHAR(100) NOT NULL COMMENT '学校名称',
    `description` VARCHAR(500) DEFAULT NULL COMMENT '学校描述',
    `allow_join_without_approval` BIT(1) NOT NULL DEFAULT b'0' COMMENT '加入学校是否免审核：0-需学校管理员审核，1-填入学工号与姓名后直接加入',
    `auto_approve_class_takeover` BIT(1) NOT NULL DEFAULT b'1' COMMENT '班级接管是否自动同意：1-其他老师申请接管失活班级时自动通过，0-需学校管理员审核',
    `stranger_message_limit` INT DEFAULT NULL COMMENT '本校向陌生用户发送私信的条数上限；NULL 表示继承全站默认',
    `stranger_message_reset_hours` INT DEFAULT NULL COMMENT '本校陌生私信配额重置小时数；NULL 表示继承全站默认',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_school_name (`school_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='学校表';

-- 学校成员表
-- 姓名与学工号属于「学校内身份」，用户在加入学校时填写，同一学校内学工号唯一；
-- 姓名与学工号一经进入学校即不可由本人修改，仅学校管理员可修改
CREATE TABLE IF NOT EXISTS `school_member` (
    `id` INT AUTO_INCREMENT PRIMARY KEY COMMENT '成员ID',
    `school_id` INT NOT NULL COMMENT '学校ID',
    `user_id` INT NOT NULL COMMENT '用户ID',
    `role` TINYINT NOT NULL DEFAULT 0 COMMENT '角色：2-学校管理员，1-老师，0-学生',
    `staff_no` VARCHAR(24) NOT NULL COMMENT '学工号（同一学校内唯一）',
    `real_name` VARCHAR(32) NOT NULL COMMENT '姓名',
    `join_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
    INDEX idx_school_id (`school_id`),
    INDEX idx_user_id (`user_id`),
    INDEX idx_school_role (`school_id`, `role`),
    UNIQUE KEY uk_school_user (`school_id`, `user_id`),
    UNIQUE KEY uk_school_staff_no (`school_id`, `staff_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='学校成员表';

-- 学校加入申请表（用户填写学工号与姓名后提交，由学校管理员审核）
CREATE TABLE IF NOT EXISTS `school_join_application` (
    `id` INT AUTO_INCREMENT PRIMARY KEY COMMENT '申请ID',
    `school_id` INT NOT NULL COMMENT '申请加入的学校ID',
    `applicant_id` INT NOT NULL COMMENT '申请人ID',
    `applicant_name` VARCHAR(32) NOT NULL COMMENT '申请人姓名',
    `applicant_no` VARCHAR(24) NOT NULL COMMENT '申请人学工号',
    `status` TINYINT NOT NULL DEFAULT 0 COMMENT '审核状态：0-待审核，1-已通过，2-已拒绝',
    `reviewer_id` INT DEFAULT NULL COMMENT '审核人ID（学校管理员）',
    `review_time` DATETIME DEFAULT NULL COMMENT '审核时间',
    `review_comment` VARCHAR(500) DEFAULT NULL COMMENT '审核意见',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '申请时间',
    INDEX idx_school_id (`school_id`),
    INDEX idx_applicant_id (`applicant_id`),
    INDEX idx_status (`status`),
    UNIQUE KEY uk_school_applicant (`school_id`, `applicant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='学校加入申请表';
