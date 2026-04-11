CREATE DATABASE IF NOT EXISTS dream_hwhub CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE dream_hwhub;

-- 班级信息表
CREATE TABLE IF NOT EXISTS `class_info` (
    `id` INT AUTO_INCREMENT PRIMARY KEY COMMENT '班级ID',
    `class_name` VARCHAR(100) NOT NULL COMMENT '班级名称',
    `description` VARCHAR(500) DEFAULT NULL COMMENT '班级描述',
    `owner_id` INT NOT NULL COMMENT '班级所有者ID',
    `invite_code` VARCHAR(20) DEFAULT NULL COMMENT '班级邀请码（6位随机码）',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_class_name (`class_name`),
    INDEX idx_owner_id (`owner_id`),
    INDEX idx_invite_code (`invite_code`),
    CONSTRAINT fk_class_owner FOREIGN KEY (`owner_id`) REFERENCES `user`(`id`) ON DELETE CASCADE
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

-- 班级邀请申请表（仅学生邀请时使用）
CREATE TABLE IF NOT EXISTS `class_invite_application` (
    `id` INT AUTO_INCREMENT PRIMARY KEY COMMENT '邀请申请ID',
    `class_id` INT NOT NULL COMMENT '班级ID',
    `inviter_id` INT NOT NULL COMMENT '邀请人ID（学生）',
    `invitee_account` VARCHAR(50) NOT NULL COMMENT '被邀请人账号',
    `status` TINYINT NOT NULL DEFAULT 0 COMMENT '审核状态：0-待审核，1-已通过，2-已拒绝',
    `reviewer_id` INT DEFAULT NULL COMMENT '审核人ID（老师/管理员）',
    `review_time` DATETIME DEFAULT NULL COMMENT '审核时间',
    `review_comment` VARCHAR(500) DEFAULT NULL COMMENT '审核意见',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '邀请时间',
    INDEX idx_class_id (`class_id`),
    INDEX idx_inviter_id (`inviter_id`),
    INDEX idx_status (`status`),
    CONSTRAINT fk_invite_app_class FOREIGN KEY (`class_id`) REFERENCES `class_info`(`id`) ON DELETE CASCADE,
    CONSTRAINT fk_invite_app_inviter FOREIGN KEY (`inviter_id`) REFERENCES `user`(`id`) ON DELETE CASCADE,
    CONSTRAINT fk_invite_app_reviewer FOREIGN KEY (`reviewer_id`) REFERENCES `user`(`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='班级邀请申请表';

-- 教师邀请用户表（教师发起的邀请，需用户同意）
CREATE TABLE IF NOT EXISTS `class_invitation` (
    `id` INT AUTO_INCREMENT PRIMARY KEY COMMENT '邀请ID',
    `class_id` INT NOT NULL COMMENT '班级ID',
    `inviter_id` INT NOT NULL COMMENT '邀请人ID（教师）',
    `invitee_user_id` INT NOT NULL COMMENT '被邀请人ID',
    `status` TINYINT NOT NULL DEFAULT 0 COMMENT '邀请状态：0-待处理，1-已同意，2-已拒绝，3-已过期',
    `expire_time` DATETIME DEFAULT NULL COMMENT '过期时间',
    `response_time` DATETIME DEFAULT NULL COMMENT '响应时间',
    `response_comment` VARCHAR(500) DEFAULT NULL COMMENT '用户回复说明',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '邀请时间',
    INDEX idx_class_id (`class_id`),
    INDEX idx_inviter_id (`inviter_id`),
    INDEX idx_invitee_user_id (`invitee_user_id`),
    INDEX idx_status (`status`),
    UNIQUE KEY uk_class_invitee (`class_id`, `invitee_user_id`, `status`),
    CONSTRAINT fk_invitation_class FOREIGN KEY (`class_id`) REFERENCES `class_info`(`id`) ON DELETE CASCADE,
    CONSTRAINT fk_invitation_inviter FOREIGN KEY (`inviter_id`) REFERENCES `user`(`id`) ON DELETE CASCADE,
    CONSTRAINT fk_invitation_invitee FOREIGN KEY (`invitee_user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='教师邀请用户表';

-- 班级创建申请表（管理员审核）
CREATE TABLE IF NOT EXISTS `class_create_application` (
    `id` INT AUTO_INCREMENT PRIMARY KEY COMMENT '申请ID',
    `applicant_id` INT NOT NULL COMMENT '申请人ID',
    `class_name` VARCHAR(100) NOT NULL COMMENT '申请的班级名称',
    `description` VARCHAR(500) DEFAULT NULL COMMENT '申请的班级描述',
    `status` TINYINT NOT NULL DEFAULT 0 COMMENT '审核状态：0-待审核，1-已通过，2-已拒绝',
    `reviewer_id` INT DEFAULT NULL COMMENT '审核人ID（管理员）',
    `review_time` DATETIME DEFAULT NULL COMMENT '审核时间',
    `review_comment` VARCHAR(500) DEFAULT NULL COMMENT '审核意见',
    `created_class_id` INT DEFAULT NULL COMMENT '审核通过后创建的班级ID',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '申请时间',
    INDEX idx_applicant_id (`applicant_id`),
    INDEX idx_status (`status`),
    CONSTRAINT fk_create_app_applicant FOREIGN KEY (`applicant_id`) REFERENCES `user`(`id`) ON DELETE CASCADE,
    CONSTRAINT fk_create_app_reviewer FOREIGN KEY (`reviewer_id`) REFERENCES `user`(`id`) ON DELETE SET NULL,
    CONSTRAINT fk_create_app_class FOREIGN KEY (`created_class_id`) REFERENCES `class_info`(`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='班级创建申请表';

-- 班级加入申请表（老师和管理员审核）
CREATE TABLE IF NOT EXISTS `class_join_application` (
    `id` INT AUTO_INCREMENT PRIMARY KEY COMMENT '申请ID',
    `class_id` INT NOT NULL COMMENT '申请加入的班级ID',
    `applicant_id` INT NOT NULL COMMENT '申请人ID',
    `status` TINYINT NOT NULL DEFAULT 0 COMMENT '审核状态：0-待审核，1-已通过，2-已拒绝',
    `reviewer_id` INT DEFAULT NULL COMMENT '审核人ID（老师或管理员）',
    `review_time` DATETIME DEFAULT NULL COMMENT '审核时间',
    `review_comment` VARCHAR(500) DEFAULT NULL COMMENT '审核意见',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '申请时间',
    INDEX idx_class_id (`class_id`),
    INDEX idx_applicant_id (`applicant_id`),
    INDEX idx_status (`status`),
    UNIQUE KEY uk_class_applicant (`class_id`, `applicant_id`),
    CONSTRAINT fk_join_app_class FOREIGN KEY (`class_id`) REFERENCES `class_info`(`id`) ON DELETE CASCADE,
    CONSTRAINT fk_join_app_applicant FOREIGN KEY (`applicant_id`) REFERENCES `user`(`id`) ON DELETE CASCADE,
    CONSTRAINT fk_join_app_reviewer FOREIGN KEY (`reviewer_id`) REFERENCES `user`(`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='班级加入申请表';

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
    `grader_id` INT DEFAULT NULL COMMENT '批改人ID',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-未提交，1-已提交，2-已批改',
    `is_late` BIT(1) NOT NULL DEFAULT b'0' COMMENT '是否逾期提交：0-否，1-是',
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