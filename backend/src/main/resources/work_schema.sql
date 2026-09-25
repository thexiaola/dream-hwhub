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
    `has_questions` BIT(1) NOT NULL DEFAULT b'0' COMMENT '是否含结构化题目：0-否（仅文本作业），1-是',
    `work_type` VARCHAR(10) NOT NULL DEFAULT 'homework' COMMENT '类型：homework-作业，exam-考试',
    `exam_duration_minutes` INT DEFAULT NULL COMMENT '考试时长（分钟）；从学生开考时刻计时，为空则仅以截止时间为准',
    `anti_cheat_enabled` BIT(1) NOT NULL DEFAULT b'0' COMMENT '是否开启反作弊：0-否，1-是',
    `anti_cheat_font` BIT(1) NOT NULL DEFAULT b'0' COMMENT '反作弊-字体映射：题干文字以打乱字体渲染，复制得到乱码',
    `anti_cheat_fullscreen` BIT(1) NOT NULL DEFAULT b'0' COMMENT '反作弊-强制全屏：离开全屏即记违规',
    `anti_cheat_no_copy` BIT(1) NOT NULL DEFAULT b'0' COMMENT '反作弊-禁止复制粘贴：拦截复制/剪切/粘贴操作',
    `anti_cheat_detect_leave` BIT(1) NOT NULL DEFAULT b'0' COMMENT '反作弊-切屏/失焦检测：切出页面或窗口失焦记违规',
    `anti_cheat_max_violations` INT DEFAULT NULL COMMENT '违规次数上限：达到后自动交卷；为空表示不限制',
    `shuffle_questions` BIT(1) NOT NULL DEFAULT b'0' COMMENT '题目乱序：0-按题号，1-为学生随机打乱题序',
    `publish_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发布时间',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_publisher_id (`publisher_id`),
    INDEX idx_class_id (`class_id`),
    INDEX idx_class_publish_deadline (`class_id`, `publish_time`, `deadline`),
    INDEX idx_is_pinned (`is_pinned`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='作业/考试表';

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

-- 作业题目表
-- 老师出题：单选/多选/判断/填空（客观题，可自动评判 + 允许老师手动评判）、
-- 主观题、附加题（附加题属于主观题，仅老师手动评判）
CREATE TABLE IF NOT EXISTS `work_question` (
    `id` INT AUTO_INCREMENT PRIMARY KEY COMMENT '题目ID',
    `work_id` INT NOT NULL COMMENT '作业ID',
    `order_no` INT NOT NULL DEFAULT 0 COMMENT '题号（排序用，从1开始）',
    `question_type` VARCHAR(20) NOT NULL COMMENT '题型：single-单选，multiple-多选，judge-判断，fill-填空，subjective-主观，extra-附加',
    `content` TEXT NOT NULL COMMENT '题干',
    `options` TEXT DEFAULT NULL COMMENT '选项 JSON 数组（单选/多选）：[{"key":"A","text":"..."}]',
    `correct_answer` TEXT DEFAULT NULL COMMENT '参考答案 JSON：单选/判断存字符串，多选存数组，填空存可接受答案数组，主观/附加为空',
    `score` DECIMAL(6,2) NOT NULL DEFAULT 0 COMMENT '本题满分',
    `analysis` TEXT DEFAULT NULL COMMENT '答案解析（可选）',
    `auto_gradable` BIT(1) NOT NULL DEFAULT b'0' COMMENT '是否可自动评判：客观题为1，主观/附加为0',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_work_id (`work_id`),
    INDEX idx_work_order (`work_id`, `order_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='作业题目表';

-- 作业作答表
-- 学生在某次提交中每道题的作答与得分：客观题默认自动评判得分，
-- 老师可手动改判（覆盖自动分）；主观题/附加题由老师手动评分。
CREATE TABLE IF NOT EXISTS `work_answer` (
    `id` INT AUTO_INCREMENT PRIMARY KEY COMMENT '作答ID',
    `submission_id` INT NOT NULL COMMENT '提交ID',
    `question_id` INT NOT NULL COMMENT '题目ID',
    `answer` TEXT DEFAULT NULL COMMENT '学生作答 JSON：单选/判断存字符串，多选存数组，填空存字符串，主观/附加存文本',
    `score` DECIMAL(6,2) DEFAULT NULL COMMENT '本题得分（自动或手动）',
    `is_correct` BIT(1) DEFAULT NULL COMMENT '客观题是否正确：1-对，0-错，NULL-未判定/主观题',
    `grading_type` VARCHAR(10) DEFAULT NULL COMMENT '评分方式：auto-自动，manual-手动',
    `comment` TEXT DEFAULT NULL COMMENT '老师对本题的评语（可选）',
    `grader_id` INT DEFAULT NULL COMMENT '本题评分人ID（手动评分时）',
    `grade_time` DATETIME DEFAULT NULL COMMENT '本题评分时间',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_submission_id (`submission_id`),
    INDEX idx_question_id (`question_id`),
    UNIQUE KEY uk_submission_question (`submission_id`, `question_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='作业作答表';

-- 考试会话表
-- 记录学生一次考试的开始时刻、限时、交卷与违规累计；用于限时判定、强制全屏与反作弊审计。
CREATE TABLE IF NOT EXISTS `exam_session` (
    `id` INT AUTO_INCREMENT PRIMARY KEY COMMENT '考试会话ID',
    `work_id` INT NOT NULL COMMENT '考试（work_info）ID',
    `student_id` INT NOT NULL COMMENT '学生ID',
    `start_time` DATETIME NOT NULL COMMENT '开考时刻（限时起点）',
    `submit_time` DATETIME DEFAULT NULL COMMENT '交卷时刻（为空表示进行中）',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1-进行中，2-已交卷，3-超时自动交卷',
    `violation_count` INT NOT NULL DEFAULT 0 COMMENT '累计违规次数',
    `font_seed` INT DEFAULT NULL COMMENT '本次考试下发的字体映射种子（反作弊-字体映射）',
    `question_order` TEXT DEFAULT NULL COMMENT '题目乱序后的题目ID顺序 JSON 数组（为空表示按题号）',
    `draft_answers` TEXT DEFAULT NULL COMMENT '答题草稿 JSON（自动保存，用于刷新/断线后恢复作答）',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_work_id (`work_id`),
    INDEX idx_student_id (`student_id`),
    UNIQUE KEY uk_work_student (`work_id`, `student_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='考试会话表';

-- 考试违规记录表
-- 每次反作弊触发（离开全屏、切屏/失焦、复制粘贴等）记一条，供老师审计。
CREATE TABLE IF NOT EXISTS `exam_violation` (
    `id` INT AUTO_INCREMENT PRIMARY KEY COMMENT '违规记录ID',
    `session_id` INT NOT NULL COMMENT '考试会话ID',
    `work_id` INT NOT NULL COMMENT '考试ID',
    `student_id` INT NOT NULL COMMENT '学生ID',
    `type` VARCHAR(32) NOT NULL COMMENT '违规类型：fullscreen_exit-退出全屏，visibility_hidden-切出页面，window_blur-窗口失焦，copy-复制，cut-剪切，paste-粘贴',
    `detail` VARCHAR(255) DEFAULT NULL COMMENT '补充说明（可选）',
    `occur_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发生时间',
    INDEX idx_session_id (`session_id`),
    INDEX idx_work_id (`work_id`),
    INDEX idx_student_id (`student_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='考试违规记录表';
