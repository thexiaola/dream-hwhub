CREATE DATABASE IF NOT EXISTS dream_hwhub CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE dream_hwhub;

-- 班级信息表
-- 班级必须归属于一所学校，班级内的姓名与学工号统一取自学校成员身份
CREATE TABLE IF NOT EXISTS `class_info` (
    `id` INT AUTO_INCREMENT PRIMARY KEY COMMENT '班级ID',
    `school_id` INT NOT NULL COMMENT '所属学校ID',
    `class_name` VARCHAR(100) NOT NULL COMMENT '班级名称',
    `description` VARCHAR(500) DEFAULT NULL COMMENT '班级描述',
    `owner_id` INT NOT NULL COMMENT '班级所有者ID',
    `invite_code` CHAR(25) DEFAULT NULL COMMENT '班级邀请码（25位随机码）',
    `allow_student_invite` TINYINT NOT NULL DEFAULT 1 COMMENT '是否允许学生邀请同学加入（1-允许，0-不允许）',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_class_name (`class_name`),
    INDEX idx_school_id (`school_id`),
    INDEX idx_owner_id (`owner_id`),
    INDEX idx_invite_code (`invite_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='班级信息表';

-- 班级成员表
-- 姓名与学工号属于「学校内身份」，统一取自 school_member，不在此表存储
CREATE TABLE IF NOT EXISTS `class_member` (
    `id` INT AUTO_INCREMENT PRIMARY KEY COMMENT '成员ID',
    `class_id` INT NOT NULL COMMENT '班级ID',
    `user_id` INT NOT NULL COMMENT '用户ID',
    `role` TINYINT NOT NULL DEFAULT 0 COMMENT '角色：1-老师，0-学生',
    `join_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
    `invite_by` INT DEFAULT NULL COMMENT '邀请人ID',
    INDEX idx_class_id (`class_id`),
    INDEX idx_user_id (`user_id`),
    INDEX idx_role (`role`),
    INDEX idx_class_role (`class_id`, `role`),
    UNIQUE KEY uk_class_user (`class_id`, `user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='班级成员表';

-- 教师邀请表（教师或助理邀请用户加入班级，等待被邀请用户确认）
CREATE TABLE IF NOT EXISTS `class_invitation` (
    `id` INT AUTO_INCREMENT PRIMARY KEY COMMENT '邀请ID',
    `class_id` INT NOT NULL COMMENT '班级ID',
    `inviter_id` INT NOT NULL COMMENT '邀请人ID（教师或助理）',
    `invitee_user_id` INT NOT NULL COMMENT '被邀请人ID',
    `status` TINYINT NOT NULL DEFAULT 0 COMMENT '用户确认状态：0-待处理，1-已同意，2-已拒绝',
    `response_time` DATETIME DEFAULT NULL COMMENT '用户响应时间',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '邀请时间',
    INDEX idx_class_id (`class_id`),
    INDEX idx_inviter_id (`inviter_id`),
    INDEX idx_invitee_user_id (`invitee_user_id`),
    INDEX idx_status (`status`),
    UNIQUE KEY uk_class_inviter_invitee (`class_id`, `inviter_id`, `invitee_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='教师邀请表';

-- 用户邀请申请表（用户发起邀请，等待被邀请用户确认）
CREATE TABLE IF NOT EXISTS `class_user_invitation` (
    `id` INT AUTO_INCREMENT PRIMARY KEY COMMENT '邀请ID',
    `class_id` INT NOT NULL COMMENT '班级ID',
    `inviter_id` INT NOT NULL COMMENT '邀请人ID',
    `invitee_id` INT NOT NULL COMMENT '被邀请人ID',
    `status` TINYINT NOT NULL DEFAULT 0 COMMENT '用户确认状态：0-待确认，1-已同意，2-已拒绝',
    `response_time` DATETIME DEFAULT NULL COMMENT '用户响应时间',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '邀请时间',
    INDEX idx_class_id (`class_id`),
    INDEX idx_inviter_id (`inviter_id`),
    INDEX idx_invitee_id (`invitee_id`),
    INDEX idx_status (`status`),
    UNIQUE KEY uk_class_inviter_invitee (`class_id`, `inviter_id`, `invitee_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户邀请申请表';

-- 教师审核邀请表（用户同意后，等待教师或助理审核）
CREATE TABLE IF NOT EXISTS `class_teacher_approval` (
    `id` INT AUTO_INCREMENT PRIMARY KEY COMMENT '审核ID',
    `class_id` INT NOT NULL COMMENT '班级ID',
    `invitation_id` INT NOT NULL COMMENT '关联的用户邀请ID',
    `invitee_id` INT NOT NULL COMMENT '被邀请人ID',
    `status` TINYINT NOT NULL DEFAULT 0 COMMENT '教师审核状态：0-待审核，1-已通过，2-已拒绝',
    `reviewer_id` INT DEFAULT NULL COMMENT '审核人ID（老师/助理）',
    `review_time` DATETIME DEFAULT NULL COMMENT '审核时间',
    `review_comment` VARCHAR(500) DEFAULT NULL COMMENT '审核意见',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_class_id (`class_id`),
    INDEX idx_invitation_id (`invitation_id`),
    INDEX idx_invitee_id (`invitee_id`),
    INDEX idx_status (`status`),
    UNIQUE KEY uk_invitation (`invitation_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='教师审核邀请表';

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
    UNIQUE KEY uk_class_applicant (`class_id`, `applicant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='班级加入申请表';

-- 班级接管申请表
-- 班级创建者的教师身份被解除后，班级进入失活状态：原创建者无法再管理、不再接纳新学生，
-- 本校其他老师可申请接管。学校可配置自动同意（默认），或由学校管理员审核；通过后转移班级所有权
CREATE TABLE IF NOT EXISTS `class_takeover_application` (
    `id` INT AUTO_INCREMENT PRIMARY KEY COMMENT '申请ID',
    `class_id` INT NOT NULL COMMENT '申请接管的班级ID',
    `applicant_id` INT NOT NULL COMMENT '申请接管的用户ID',
    `status` TINYINT NOT NULL DEFAULT 0 COMMENT '审核状态：0-待审核，1-已通过，2-已拒绝',
    `reviewer_id` INT DEFAULT NULL COMMENT '审核人ID（学校管理员；自动同意时为空）',
    `review_time` DATETIME DEFAULT NULL COMMENT '审核时间',
    `review_comment` VARCHAR(500) DEFAULT NULL COMMENT '审核意见',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '申请时间',
    INDEX idx_class_id (`class_id`),
    INDEX idx_applicant_id (`applicant_id`),
    INDEX idx_status (`status`),
    UNIQUE KEY uk_class_applicant (`class_id`, `applicant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='班级接管申请表';
