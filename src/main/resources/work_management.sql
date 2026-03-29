CREATE DATABASE IF NOT EXISTS dream_hwhub CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE dream_hwhub;

-- 班级信息表
CREATE TABLE IF NOT EXISTS `class_info` (
    `id` INT AUTO_INCREMENT PRIMARY KEY COMMENT '班级ID',
    `class_name` VARCHAR(100) NOT NULL COMMENT '班级名称',
    `description` VARCHAR(500) DEFAULT NULL COMMENT '班级描述',
    `creator_id` INT NOT NULL COMMENT '创建者ID',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_class_name (`class_name`),
    INDEX idx_creator_id (`creator_id`),
    CONSTRAINT fk_class_creator FOREIGN KEY (`creator_id`) REFERENCES `user`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='班级信息表';

-- 班级成员表
CREATE TABLE IF NOT EXISTS `class_member` (
    `id` INT AUTO_INCREMENT PRIMARY KEY COMMENT '成员ID',
    `class_id` INT NOT NULL COMMENT '班级ID',
    `user_id` INT NOT NULL COMMENT '用户ID',
    `role` BIT(1) NOT NULL COMMENT '角色：1-老师，0-学生',
    `join_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
    `invite_by` INT DEFAULT NULL COMMENT '邀请人ID',
    INDEX idx_class_id (`class_id`),
    INDEX idx_user_id (`user_id`),
    INDEX idx_role (`role`),
    UNIQUE KEY uk_class_user (`class_id`, `user_id`),
    CONSTRAINT fk_member_class FOREIGN KEY (`class_id`) REFERENCES `class_info`(`id`) ON DELETE CASCADE,
    CONSTRAINT fk_member_user FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE,
    CONSTRAINT fk_member_invite FOREIGN KEY (`invite_by`) REFERENCES `user`(`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='班级成员表';

-- 班级申请表（统一管理创建和加入申请）
CREATE TABLE IF NOT EXISTS `class_application` (
    `id` INT AUTO_INCREMENT PRIMARY KEY COMMENT '申请ID',
    `type` TINYINT NOT NULL COMMENT '申请类型：1-创建班级申请，2-加入班级申请',
    `class_id` INT DEFAULT NULL COMMENT '班级ID（创建申请时为 NULL，加入申请时必填）',
    `applicant_id` INT NOT NULL COMMENT '申请人ID',
    `target_role` BIT(1) DEFAULT NULL COMMENT '目标角色：1-老师，0-学生（仅加入申请时有意义）',
    `class_name` VARCHAR(100) DEFAULT NULL COMMENT '班级名称（仅创建申请时有意义）',
    `description` VARCHAR(500) DEFAULT NULL COMMENT '班级描述（仅创建申请时有意义）',
    `status` TINYINT NOT NULL DEFAULT 0 COMMENT '审核状态：0-待审核，1-已通过，2-已拒绝',
    `reviewer_id` INT DEFAULT NULL COMMENT '审核人ID',
    `review_time` DATETIME DEFAULT NULL COMMENT '审核时间',
    `review_comment` VARCHAR(500) DEFAULT NULL COMMENT '审核意见',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '申请时间',
    INDEX idx_type (`type`),
    INDEX idx_class_id (`class_id`),
    INDEX idx_applicant_id (`applicant_id`),
    INDEX idx_status (`status`),
    CONSTRAINT fk_application_class FOREIGN KEY (`class_id`) REFERENCES `class_info`(`id`) ON DELETE CASCADE,
    CONSTRAINT fk_application_applicant FOREIGN KEY (`applicant_id`) REFERENCES `user`(`id`) ON DELETE CASCADE,
    CONSTRAINT fk_application_reviewer FOREIGN KEY (`reviewer_id`) REFERENCES `user`(`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='班级申请表';

-- 作业表
CREATE TABLE IF NOT EXISTS `work_info` (
    `id` INT AUTO_INCREMENT PRIMARY KEY COMMENT '作业ID',
    `title` VARCHAR(200) NOT NULL COMMENT '作业标题',
    `description` TEXT COMMENT '作业描述',
    `publisher_id` INT NOT NULL COMMENT '发布人ID',
    `class_id` INT NOT NULL COMMENT '所属班级ID',
    `deadline` DATETIME DEFAULT NULL COMMENT '截止时间',
    `total_score` INT NOT NULL DEFAULT 100 COMMENT '作业总分',
    `publish_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发布时间',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_publisher_id (`publisher_id`),
    INDEX idx_class_id (`class_id`),
    CONSTRAINT fk_work_publisher FOREIGN KEY (`publisher_id`) REFERENCES `user`(`id`) ON DELETE CASCADE,
    CONSTRAINT fk_work_class FOREIGN KEY (`class_id`) REFERENCES `class_info`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='作业表';

-- 作业附件表
CREATE TABLE IF NOT EXISTS `work_attachment` (
    `id` INT AUTO_INCREMENT PRIMARY KEY COMMENT '附件ID',
    `work_id` INT NOT NULL COMMENT '作业ID',
    `file_name` VARCHAR(255) NOT NULL COMMENT '文件名',
    `file_path` VARCHAR(500) NOT NULL COMMENT '文件路径',
    `file_size` BIGINT NOT NULL COMMENT '文件大小（字节）',
    `file_type` VARCHAR(100) DEFAULT NULL COMMENT '文件类型',
    `upload_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
    INDEX idx_work_id (`work_id`),
    CONSTRAINT fk_work_attachment FOREIGN KEY (`work_id`) REFERENCES `work_info`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='作业附件表';

-- 作业提交表
CREATE TABLE IF NOT EXISTS `work_submission` (
    `id` INT AUTO_INCREMENT PRIMARY KEY COMMENT '提交ID',
    `work_id` INT NOT NULL COMMENT '作业ID',
    `class_id` INT NOT NULL COMMENT '所属班级ID',
    `submitter_id` INT NOT NULL COMMENT '提交人ID',
    `submission_content` TEXT COMMENT '提交内容/文本描述',
    `score` DECIMAL(5,2) DEFAULT NULL COMMENT '提交分数',
    `comment` TEXT COMMENT '批改人评语',
    `submit_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '提交时间',
    `grade_time` DATETIME DEFAULT NULL COMMENT '批改时间',
    `grader_id` INT DEFAULT NULL COMMENT '批改人 ID',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-未提交，1-已提交，2-已批改',
    `is_overdue` BIT(1) NOT NULL DEFAULT b'0' COMMENT '是否逾期：0-否，1-是',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_work_id (`work_id`),
    INDEX idx_class_id (`class_id`),
    INDEX idx_submitter_id (`submitter_id`),
    INDEX idx_grader_id (`grader_id`),
    INDEX idx_status (`status`),
    CONSTRAINT fk_submission_work FOREIGN KEY (`work_id`) REFERENCES `work_info`(`id`) ON DELETE CASCADE,
    CONSTRAINT fk_submission_class FOREIGN KEY (`class_id`) REFERENCES `class_info`(`id`) ON DELETE CASCADE,
    CONSTRAINT fk_submission_submitter FOREIGN KEY (`submitter_id`) REFERENCES `user`(`id`) ON DELETE CASCADE,
    CONSTRAINT fk_submission_grader FOREIGN KEY (`grader_id`) REFERENCES `user`(`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='作业提交表';

-- 作业提交附件表
CREATE TABLE IF NOT EXISTS `work_submission_attachment` (
     `id` INT AUTO_INCREMENT PRIMARY KEY COMMENT '提交附件ID',
     `submission_id` INT NOT NULL COMMENT '提交ID',
     `file_name` VARCHAR(255) NOT NULL COMMENT '文件名',
     `file_path` VARCHAR(500) NOT NULL COMMENT '文件路径',
     `file_size` BIGINT NOT NULL COMMENT '文件大小（字节）',
     `file_type` VARCHAR(100) DEFAULT NULL COMMENT '文件类型',
     `upload_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
     INDEX idx_submission_id (`submission_id`),
     CONSTRAINT fk_submission_attachment FOREIGN KEY (`submission_id`) REFERENCES `work_submission`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='作业提交附件表';