CREATE DATABASE IF NOT EXISTS dream_hwhub CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE dream_hwhub;

-- 作业表
CREATE TABLE IF NOT EXISTS `work_info` (
    `id` INT AUTO_INCREMENT PRIMARY KEY COMMENT '作业ID',
    `title` VARCHAR(200) NOT NULL COMMENT '作业标题',
    `description` TEXT COMMENT '作业描述',
    `publisher_id` INT NOT NULL COMMENT '发布人ID',
    `class_id` INT NOT NULL COMMENT '所属班级ID',
    `deadline` DATETIME DEFAULT NULL COMMENT '截止时间',
    `total_score` INT NOT NULL DEFAULT 100 COMMENT '作业总分',
    `allow_late_submit` BIT(1) NOT NULL DEFAULT b'1' COMMENT '是否允许逾期提交：0-否，1-是',
    `is_pinned` BIT(1) NOT NULL DEFAULT b'0' COMMENT '是否置顶：0-否，1-是',
    `publish_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发布时间',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_publisher_id (`publisher_id`),
    INDEX idx_class_id (`class_id`),
    INDEX idx_class_publish_deadline (`class_id`, `publish_time`, `deadline`),
    INDEX idx_is_pinned (`is_pinned`)
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
    INDEX idx_work_id (`work_id`)
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
    `grade_time` DATETIME DEFAULT NULL COMMENT '批改时间',
    `grader_id` INT DEFAULT NULL COMMENT '批改人ID',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-未提交，1-已提交，2-已批改',
    `is_late` BIT(1) NOT NULL DEFAULT b'0' COMMENT '是否逾期提交：0-否，1-是',
    `is_deleted` BIT(1) NOT NULL DEFAULT b'0' COMMENT '是否删除：0-否，1-是（软删除）',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_work_id (`work_id`),
    INDEX idx_class_id (`class_id`),
    INDEX idx_submitter_id (`submitter_id`),
    INDEX idx_grader_id (`grader_id`),
    INDEX idx_status (`status`),
    INDEX idx_is_deleted (`is_deleted`),
    INDEX idx_work_submitter_deleted (`work_id`, `submitter_id`, `is_deleted`),
    INDEX idx_work_deleted (`work_id`, `is_deleted`),
    INDEX idx_class_deleted (`class_id`, `is_deleted`),
    UNIQUE KEY uk_work_submitter (`work_id`, `submitter_id`)
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
     `is_deleted` BIT(1) NOT NULL DEFAULT b'0' COMMENT '是否删除：0-否，1-是（软删除）',
     INDEX idx_submission_id (`submission_id`),
     INDEX idx_is_deleted (`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='作业提交附件表';
