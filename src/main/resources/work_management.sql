CREATE DATABASE IF NOT EXISTS dream_hwhub CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE dream_hwhub;

-- 作业表
CREATE TABLE IF NOT EXISTS `work` (
    `id` INT AUTO_INCREMENT PRIMARY KEY COMMENT '作业ID',
    `title` VARCHAR(200) NOT NULL COMMENT '作业标题',
    `description` TEXT NOT NULL COMMENT '作业描述',
    `publisher_id` INT NOT NULL COMMENT '发布人ID',
    `deadline` DATETIME NOT NULL COMMENT '截止时间',
    `total_score` INT DEFAULT 100 COMMENT '作业总分',
    `status` TINYINT DEFAULT 1 COMMENT '作业状态：0-未发布，1-已发布，2-已结束',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_publisher_id (`publisher_id`),
    INDEX idx_status (`status`),
    INDEX idx_deadline (`deadline`),
    CONSTRAINT fk_work_publisher FOREIGN KEY (`publisher_id`) REFERENCES `user`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='作业表';

-- 作业提交表
CREATE TABLE IF NOT EXISTS `work_submission` (
    `id` INT AUTO_INCREMENT PRIMARY KEY COMMENT '提交ID',
    `work_id` INT NOT NULL COMMENT '作业ID',
    `submitter_id` INT NOT NULL COMMENT '提交人ID',
    `submission_content` TEXT DEFAULT NULL COMMENT '提交内容/文本描述',
    `score` DECIMAL(5,2) DEFAULT NULL COMMENT '提交分数',
    `comment` TEXT DEFAULT NULL COMMENT '批改人评语',
    `submit_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '提交时间',
    `grade_time` DATETIME DEFAULT NULL COMMENT '批改时间',
    `grader_id` INT DEFAULT NULL COMMENT '批改人ID',
    `status` TINYINT DEFAULT 1 COMMENT '提交状态：0-未提交，1-已提交，2-已批改',
    `is_overdue` TINYINT DEFAULT 0 COMMENT '是否逾期：0-否，1-是',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_work_id (`work_id`),
    INDEX idx_submitter_id (`submitter_id`),
    INDEX idx_status (`status`),
    INDEX idx_submit_time (`submit_time`),
    CONSTRAINT fk_work FOREIGN KEY (`work_id`) REFERENCES `work`(`id`) ON DELETE CASCADE,
    CONSTRAINT fk_submission_submitter FOREIGN KEY (`submitter_id`) REFERENCES `user`(`id`) ON DELETE CASCADE,
    CONSTRAINT fk_submission_grader FOREIGN KEY (`grader_id`) REFERENCES `user`(`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='作业提交表';

-- 作业提交附件表
CREATE TABLE IF NOT EXISTS `work_submission_attachment` (
    `id` INT AUTO_INCREMENT PRIMARY KEY COMMENT '提交附件ID',
    `submission_id` INT NOT NULL COMMENT '提交ID',
    `file_name` VARCHAR(255) NOT NULL COMMENT '文件名称',
    `file_path` VARCHAR(500) NOT NULL COMMENT '文件路径',
    `file_size` BIGINT DEFAULT NULL COMMENT '文件大小',
    `file_type` VARCHAR(100) DEFAULT NULL COMMENT '文件类型',
    `upload_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
    INDEX idx_submission_id (`submission_id`),
    CONSTRAINT fk_submission_attachment FOREIGN KEY (`submission_id`) REFERENCES `work_submission`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='作业提交附件表';

-- 作业附件表
CREATE TABLE IF NOT EXISTS `work_attachment` (
    `id` INT AUTO_INCREMENT PRIMARY KEY COMMENT '附件ID',
    `work_id` INT NOT NULL COMMENT '作业ID',
    `file_name` VARCHAR(255) NOT NULL COMMENT '文件名称',
    `file_path` VARCHAR(500) NOT NULL COMMENT '文件路径',
    `file_size` BIGINT DEFAULT NULL COMMENT '文件大小',
    `file_type` VARCHAR(100) DEFAULT NULL COMMENT '文件类型',
    `upload_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
    INDEX idx_work_id (`work_id`),
    CONSTRAINT fk_attachment_work FOREIGN KEY (`work_id`) REFERENCES `work`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='作业附件表';