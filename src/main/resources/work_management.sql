CREATE DATABASE IF NOT EXISTS dream_hwhub CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE dream_hwhub;

-- 作业表
CREATE TABLE IF NOT EXISTS `work` (
    `id` INT AUTO_INCREMENT PRIMARY KEY COMMENT '作业 ID',
    `title` VARCHAR(200) NOT NULL COMMENT '作业标题',
    `description` TEXT NOT NULL COMMENT '作业描述',
    `teacher_no` VARCHAR(50) NOT NULL COMMENT '发布教师工号',
    `teacher_name` VARCHAR(100) NOT NULL COMMENT '发布教师姓名',
    `deadline` DATETIME NOT NULL COMMENT '截止时间',
    `total_score` INT DEFAULT 100 COMMENT '作业总分',
    `status` TINYINT DEFAULT 1 COMMENT '作业状态：0-未发布，1-已发布，2-已结束',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_teacher_no (`teacher_no`),
    INDEX idx_status (`status`),
    INDEX idx_deadline (`deadline`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='作业表';

-- 作业提交表
CREATE TABLE IF NOT EXISTS `work_submission` (
    `id` INT AUTO_INCREMENT PRIMARY KEY COMMENT '提交 ID',
    `work_id` INT NOT NULL COMMENT '作业 ID',
    `student_no` VARCHAR(50) NOT NULL COMMENT '提交学生学号',
    `student_name` VARCHAR(100) NOT NULL COMMENT '提交学生姓名',
    `submission_content` TEXT NOT NULL COMMENT '提交内容/文件路径',
    `score` DECIMAL(5,2) DEFAULT NULL COMMENT '提交分数',
    `comment` TEXT DEFAULT NULL COMMENT '教师评语',
    `submit_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '提交时间',
    `grade_time` DATETIME DEFAULT NULL COMMENT '批改时间',
    `grade_teacher_no` VARCHAR(50) DEFAULT NULL COMMENT '批改教师工号',
    `status` TINYINT DEFAULT 1 COMMENT '提交状态：0-未提交，1-已提交，2-已批改',
    `is_overdue` TINYINT DEFAULT 0 COMMENT '是否逾期：0-否，1-是',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_work_id (`work_id`),
    INDEX idx_student_no (`student_no`),
    INDEX idx_status (`status`),
    INDEX idx_submit_time (`submit_time`),
    CONSTRAINT fk_work FOREIGN KEY (`work_id`) REFERENCES `work`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='作业提交表';
