-- =============================================================================
-- Project: Pandora - H2 Database Schema (MySQL Compatibility Mode)
-- =============================================================================

CREATE TABLE IF NOT EXISTS `users` (
    `id`              BIGINT       NOT NULL AUTO_INCREMENT,
    `username`        VARCHAR(64)  NOT NULL,
    `password_hash`   VARCHAR(128) NOT NULL,
    `role`            VARCHAR(20)  NOT NULL DEFAULT 'EMPLOYEE',
    `department_id`   BIGINT       DEFAULT NULL,
    `team_id`         BIGINT       DEFAULT NULL,
    `avatar`          VARCHAR(256) DEFAULT NULL,
    `theme_mode`      VARCHAR(10)  NOT NULL DEFAULT 'AUTO',
    `manual_theme`    VARCHAR(32)  DEFAULT NULL,
    `created_at`      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `deleted`         TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    CONSTRAINT `uk_username` UNIQUE (`username`)
);
CREATE INDEX IF NOT EXISTS `idx_users_department` ON `users` (`department_id`);
CREATE INDEX IF NOT EXISTS `idx_users_team` ON `users` (`team_id`);
CREATE INDEX IF NOT EXISTS `idx_users_role` ON `users` (`role`);

CREATE TABLE IF NOT EXISTS `departments` (
    `id`            BIGINT      NOT NULL AUTO_INCREMENT,
    `name`          VARCHAR(128) NOT NULL,
    `head_user_id`  BIGINT      DEFAULT NULL,
    `parent_id`     BIGINT      DEFAULT NULL,
    `created_at`    TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`    TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `deleted`       TINYINT     NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`)
);
CREATE INDEX IF NOT EXISTS `idx_dept_parent` ON `departments` (`parent_id`);
CREATE INDEX IF NOT EXISTS `idx_dept_head` ON `departments` (`head_user_id`);

CREATE TABLE IF NOT EXISTS `teams` (
    `id`            BIGINT      NOT NULL AUTO_INCREMENT,
    `name`          VARCHAR(128) NOT NULL,
    `leader_user_id` BIGINT     DEFAULT NULL,
    `department_id` BIGINT      DEFAULT NULL,
    `created_at`    TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`    TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `deleted`       TINYINT     NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`)
);
CREATE INDEX IF NOT EXISTS `idx_team_department` ON `teams` (`department_id`);
CREATE INDEX IF NOT EXISTS `idx_team_leader` ON `teams` (`leader_user_id`);

CREATE TABLE IF NOT EXISTS `tasks` (
    `id`             BIGINT       NOT NULL AUTO_INCREMENT,
    `name`           VARCHAR(256) NOT NULL,
    `priority`       VARCHAR(10)  NOT NULL DEFAULT 'MEDIUM',
    `status`         VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    `start_time`     TIMESTAMP    DEFAULT NULL,
    `end_time`       TIMESTAMP    DEFAULT NULL,
    `progress_note`  CLOB         DEFAULT NULL,
    `assignee_id`    BIGINT       NOT NULL,
    `dispatcher_id`  BIGINT       DEFAULT NULL,
    `is_important`   TINYINT      NOT NULL DEFAULT 0,
    `is_urgent`      TINYINT      NOT NULL DEFAULT 0,
    `version`        INT          NOT NULL DEFAULT 0,
    `created_at`     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `deleted`        TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`)
);
CREATE INDEX IF NOT EXISTS `idx_task_assignee_status` ON `tasks` (`assignee_id`, `status`);
CREATE INDEX IF NOT EXISTS `idx_task_dispatcher` ON `tasks` (`dispatcher_id`);
CREATE INDEX IF NOT EXISTS `idx_task_status` ON `tasks` (`status`);
CREATE INDEX IF NOT EXISTS `idx_task_quadrant` ON `tasks` (`is_important`, `is_urgent`);

CREATE TABLE IF NOT EXISTS `work_logs` (
    `id`               BIGINT       NOT NULL AUTO_INCREMENT,
    `user_id`          BIGINT       NOT NULL,
    `work_item`        VARCHAR(512) NOT NULL,
    `completion_status` VARCHAR(20) NOT NULL DEFAULT 'IN_PROGRESS',
    `time_cost`        DECIMAL(5,2) DEFAULT NULL,
    `log_date`         DATE         NOT NULL,
    `created_at`       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `deleted`          TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`)
);
CREATE INDEX IF NOT EXISTS `idx_log_user_date` ON `work_logs` (`user_id`, `log_date`);
CREATE UNIQUE INDEX IF NOT EXISTS `uk_log_user_date_active` ON `work_logs` (`user_id`, `log_date`);

CREATE TABLE IF NOT EXISTS `top10_works` (
    `id`              BIGINT       NOT NULL AUTO_INCREMENT,
    `user_id`         BIGINT       NOT NULL,
    `work_type`       VARCHAR(30)  NOT NULL,
    `rank_order`      TINYINT      NOT NULL,
    `content`         VARCHAR(512) NOT NULL,
    `related_task_id` BIGINT       DEFAULT NULL,
    `updated_at`      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `deleted`         TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`)
);

CREATE TABLE IF NOT EXISTS `mbti_reports` (
    `id`              BIGINT       NOT NULL AUTO_INCREMENT,
    `user_id`         BIGINT       NOT NULL,
    `personality_type` VARCHAR(4) NOT NULL,
    `answer_data`     CLOB         DEFAULT NULL,
    `report_content`  CLOB         DEFAULT NULL,
    `generated_at`    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `expire_at`       TIMESTAMP    DEFAULT NULL,
    `deleted`         TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`)
);
CREATE INDEX IF NOT EXISTS `idx_mbti_user` ON `mbti_reports` (`user_id`);

CREATE TABLE IF NOT EXISTS `ai_analyses` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT,
    `user_id`       BIGINT       NOT NULL,
    `analysis_type` VARCHAR(20)  NOT NULL,
    `input_summary` CLOB         DEFAULT NULL,
    `result`        CLOB         DEFAULT NULL,
    `status`        VARCHAR(10)  NOT NULL DEFAULT 'PENDING',
    `generated_at`  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `deleted`       TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`)
);
CREATE INDEX IF NOT EXISTS `idx_ai_user_type` ON `ai_analyses` (`user_id`, `analysis_type`);

CREATE TABLE IF NOT EXISTS `audit_logs` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT,
    `operator_id` BIGINT       NOT NULL,
    `action`      VARCHAR(32)  NOT NULL,
    `target_type` VARCHAR(64)  NOT NULL,
    `target_id`   BIGINT       DEFAULT NULL,
    `detail`      CLOB         DEFAULT NULL,
    `operated_at` TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`)
);
CREATE INDEX IF NOT EXISTS `idx_audit_operator_time` ON `audit_logs` (`operator_id`, `operated_at`);
ALTER TABLE `audit_logs` ADD COLUMN IF NOT EXISTS `operation_type` VARCHAR(32) DEFAULT NULL;
ALTER TABLE `audit_logs` ADD COLUMN IF NOT EXISTS `operation_content` VARCHAR(500) DEFAULT NULL;
CREATE INDEX IF NOT EXISTS `idx_audit_type_time` ON `audit_logs` (`operation_type`, `operated_at`);


CREATE TABLE IF NOT EXISTS `invitation_requests` (
    `id`         BIGINT       NOT NULL AUTO_INCREMENT,
    `from_user_id` BIGINT     NOT NULL,
    `to_user_id` BIGINT       NOT NULL,
    `content`    VARCHAR(512) NOT NULL,
    `status`     VARCHAR(10)  NOT NULL DEFAULT 'PENDING',
    `created_at` TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `deleted`    TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`)
);
CREATE INDEX IF NOT EXISTS `idx_inv_from_user` ON `invitation_requests` (`from_user_id`);
CREATE INDEX IF NOT EXISTS `idx_inv_to_user` ON `invitation_requests` (`to_user_id`);

CREATE TABLE IF NOT EXISTS `audit_rules` (
    `id`             BIGINT       NOT NULL AUTO_INCREMENT,
    `trigger_action` VARCHAR(64)  NOT NULL,
    `sequence`       INT          NOT NULL DEFAULT 1,
    `enabled`        TINYINT      NOT NULL DEFAULT 1,
    `deleted`        TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`)
);
CREATE INDEX IF NOT EXISTS `idx_rule_trigger` ON `audit_rules` (`trigger_action`);

CREATE TABLE IF NOT EXISTS `audit_rule_approvers` (
    `id`         BIGINT   NOT NULL AUTO_INCREMENT,
    `rule_id`    BIGINT   NOT NULL,
    `approver_id` BIGINT  NOT NULL,
    `sequence`   INT      NOT NULL,
    PRIMARY KEY (`id`)
);
CREATE INDEX IF NOT EXISTS `idx_approver_rule` ON `audit_rule_approvers` (`rule_id`);
CREATE INDEX IF NOT EXISTS `idx_approver_user` ON `audit_rule_approvers` (`approver_id`);

-- =============================================================================
-- 显式层级关系表 (替代隐式 department_id/team_id 推导, 支持跨层级递归查询)
-- =============================================================================
CREATE TABLE IF NOT EXISTS `user_hierarchy` (
    `id`             BIGINT    NOT NULL AUTO_INCREMENT,
    `superior_id`    BIGINT    NOT NULL,
    `subordinate_id` BIGINT    NOT NULL,
    `created_at`     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    CONSTRAINT `uk_hierarchy_pair` UNIQUE (`superior_id`, `subordinate_id`)
);
CREATE INDEX IF NOT EXISTS `idx_hierarchy_superior` ON `user_hierarchy` (`superior_id`);
CREATE INDEX IF NOT EXISTS `idx_hierarchy_subordinate` ON `user_hierarchy` (`subordinate_id`);

-- =============================================================================
-- 组织归属独立表 (4NF分离: 从users表分离部门/团队归属信息)
-- =============================================================================
CREATE TABLE IF NOT EXISTS `user_org_affiliation` (
    `user_id`       BIGINT NOT NULL,
    `department_id` BIGINT DEFAULT NULL,
    `team_id`       BIGINT DEFAULT NULL,
    PRIMARY KEY (`user_id`)
);
CREATE INDEX IF NOT EXISTS `idx_org_dept` ON `user_org_affiliation` (`department_id`);
CREATE INDEX IF NOT EXISTS `idx_org_team` ON `user_org_affiliation` (`team_id`);

-- =============================================================================
-- 初始化数据：管理员账号 (密码: admin123, BCrypt加密, 用户名统一使用编号后缀)
-- =============================================================================
MERGE INTO `users` (`id`, `username`, `password_hash`, `role`) KEY(`id`) VALUES
(1, 'admin0', '$2a$10$sT4ev7WP3x1xvVoudhxUkeXoXOtQHp3eq2LNQRLQTvPoL39OXwSm6', 'ADMIN');
-- =============================================================================
-- 扩展字段：用户邮箱/实名认证/审批状态 (ALTER TABLE, 幂等)
-- =============================================================================
ALTER TABLE `users` ADD COLUMN IF NOT EXISTS `email` VARCHAR(128) DEFAULT NULL;
ALTER TABLE `users` ADD COLUMN IF NOT EXISTS `real_name` VARCHAR(64) DEFAULT NULL;
ALTER TABLE `users` ADD COLUMN IF NOT EXISTS `id_card` VARCHAR(18) DEFAULT NULL;
ALTER TABLE `users` ADD COLUMN IF NOT EXISTS `approval_status` VARCHAR(20) NOT NULL DEFAULT 'APPROVED';
ALTER TABLE `users` ADD COLUMN IF NOT EXISTS `phone` VARCHAR(20) DEFAULT NULL;
CREATE INDEX IF NOT EXISTS `idx_users_email` ON `users` (`email`);
CREATE INDEX IF NOT EXISTS `idx_users_id_card` ON `users` (`id_card`);
CREATE INDEX IF NOT EXISTS `idx_users_approval` ON `users` (`approval_status`);

-- =============================================================================
-- 员工预登记表 (管理员预录入员工信息, 注册时自动匹配)
-- =============================================================================
CREATE TABLE IF NOT EXISTS `employee_records` (
    `id`              BIGINT       NOT NULL AUTO_INCREMENT,
    `real_name`       VARCHAR(64)  NOT NULL,
    `id_card`         VARCHAR(18)  NOT NULL,
    `email`           VARCHAR(128) DEFAULT NULL,
    `phone`           VARCHAR(20)  DEFAULT NULL,
    `department_id`   BIGINT       DEFAULT NULL,
    `team_id`         BIGINT       DEFAULT NULL,
    `role`            VARCHAR(20)  NOT NULL DEFAULT 'EMPLOYEE',
    `position`        VARCHAR(64)  DEFAULT NULL COMMENT '职位名称',
    `status`          VARCHAR(20)  NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/REGISTERED/DISABLED',
    `registered_user_id` BIGINT    DEFAULT NULL COMMENT '匹配到的用户ID',
    `created_at`      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `deleted`         TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    CONSTRAINT `uk_emp_id_card` UNIQUE (`id_card`)
);
CREATE INDEX IF NOT EXISTS `idx_emp_status` ON `employee_records` (`status`);
CREATE INDEX IF NOT EXISTS `idx_emp_dept` ON `employee_records` (`department_id`);
-- =============================================================================
-- 密码重置申请表 (用户忘记密码时申请, 管理员审批后重置)
-- =============================================================================
CREATE TABLE IF NOT EXISTS `password_reset_requests` (
    `id`              BIGINT       NOT NULL AUTO_INCREMENT,
    `user_id`         BIGINT       DEFAULT NULL COMMENT '匹配到的用户ID, 未匹配则为NULL',
    `username`        VARCHAR(64)  NOT NULL COMMENT '申请时填写的用户名或邮箱',
    `status`          VARCHAR(20)  NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/APPROVED/REJECTED',
    `request_reason`  VARCHAR(256) DEFAULT NULL COMMENT '申请原因',
    `reject_reason`   VARCHAR(256) DEFAULT NULL COMMENT '拒绝原因',
    `processed_by`    BIGINT       DEFAULT NULL COMMENT '审批管理员ID',
    `processed_at`    TIMESTAMP    DEFAULT NULL,
    `created_at`      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`)
);
CREATE INDEX IF NOT EXISTS `idx_prr_status` ON `password_reset_requests` (`status`);
ALTER TABLE `users` ADD COLUMN IF NOT EXISTS `birthday` DATE DEFAULT NULL;
ALTER TABLE `users` ADD COLUMN IF NOT EXISTS `last_birthday_change` TIMESTAMP DEFAULT NULL;
ALTER TABLE `users` ADD COLUMN IF NOT EXISTS `show_completed_tasks` TINYINT NOT NULL DEFAULT 1;
ALTER TABLE `users` ADD COLUMN IF NOT EXISTS `show_yesterday_log` TINYINT NOT NULL DEFAULT 1;
CREATE TABLE IF NOT EXISTS `notifications` (
    `id`              BIGINT       NOT NULL AUTO_INCREMENT,
    `target_user_id`  BIGINT       NOT NULL,
    `type`            VARCHAR(20)  NOT NULL,
    `task_id`         BIGINT       DEFAULT NULL,
    `content`         VARCHAR(256) NOT NULL,
    `is_read`         TINYINT      NOT NULL DEFAULT 0,
    `created_at`      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`)
);
CREATE INDEX IF NOT EXISTS `idx_notif_user_created` ON `notifications` (`target_user_id`, `created_at`);
CREATE INDEX IF NOT EXISTS `idx_notif_user_unread` ON `notifications` (`target_user_id`, `is_read`);


-- TASK-R18-001: notifications.type 列宽扩展 VARCHAR(20)→VARCHAR(32)
-- 支持新通知类型 TOP10_IMPORTANT_CHANGED（22 字符）、TOP10_DISPATCH_CHANGED（21 字符）
-- H2 幂等执行，重复执行无副作用，存量 NEW_TASK/STATUS_CHANGE 数据不受影响
ALTER TABLE `notifications` ALTER COLUMN `type` VARCHAR(32) NOT NULL;

-- TASK-R18-005: 通知偏好表（每用户每类通知至多一条记录）
-- 偏好仅影响 Android 弹窗过滤，不影响通知记录生成（REQ-R18-06 规则 5）
CREATE TABLE IF NOT EXISTS `notification_preferences` (
    `id`       BIGINT      NOT NULL AUTO_INCREMENT,
    `user_id`  BIGINT      NOT NULL,
    `type`     VARCHAR(32) NOT NULL,
    `enabled`  TINYINT     NOT NULL DEFAULT 1,
    `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`)
);
-- user_id + type 联合唯一索引：每用户对每类通知至多一条偏好记录
CREATE UNIQUE INDEX IF NOT EXISTS `idx_notif_pref_user_type` ON `notification_preferences` (`user_id`, `type`);

ALTER TABLE `top10_works` ADD COLUMN IF NOT EXISTS `is_auto_filled` TINYINT NOT NULL DEFAULT 0;
ALTER TABLE `top10_works` ADD COLUMN IF NOT EXISTS `source_note` VARCHAR(256) DEFAULT NULL;
ALTER TABLE `top10_works` ADD COLUMN IF NOT EXISTS `source_log_date` DATE DEFAULT NULL;
ALTER TABLE `top10_works` ADD COLUMN IF NOT EXISTS `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;

CREATE TABLE IF NOT EXISTS `function_permissions` (
    `id`              BIGINT       NOT NULL AUTO_INCREMENT,
    `function_code`   VARCHAR(50)  NOT NULL,
    `subject_user_id` BIGINT       NOT NULL,
    `configured_by`   BIGINT       DEFAULT NULL,
    `configured_at`   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `deleted`         TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`)
);
CREATE INDEX IF NOT EXISTS `idx_func_perm_code` ON `function_permissions` (`function_code`, `deleted`);
CREATE INDEX IF NOT EXISTS `idx_func_perm_subject` ON `function_permissions` (`subject_user_id`, `deleted`);

CREATE TABLE IF NOT EXISTS `ai_config` (
    `id`               BIGINT       NOT NULL AUTO_INCREMENT,
    `api_name`         VARCHAR(100) NOT NULL,
    `api_url`          VARCHAR(512) NOT NULL,
    `api_key`          VARCHAR(256) NOT NULL,
    `model`            VARCHAR(100) NOT NULL DEFAULT 'deepseek-chat',
    `provider`         VARCHAR(30)  NOT NULL DEFAULT 'DEEPSEEK',
    `quota_threshold`  INT          NOT NULL DEFAULT 100,
    `timeout_ms`       INT          NOT NULL DEFAULT 30000,
    `enabled`          TINYINT      NOT NULL DEFAULT 1,
    `fallback_url`     VARCHAR(512) NOT NULL DEFAULT 'http://localhost:8081',
    `fallback_enabled` TINYINT      NOT NULL DEFAULT 1,
    `updated_at`       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`)
 );

ALTER TABLE `ai_config` ADD COLUMN IF NOT EXISTS `model` VARCHAR(100) NOT NULL DEFAULT 'deepseek-chat';
ALTER TABLE `ai_config` ADD COLUMN IF NOT EXISTS `provider` VARCHAR(30) NOT NULL DEFAULT 'DEEPSEEK';

CREATE TABLE IF NOT EXISTS `ai_quota_events` (
    `id`                BIGINT       NOT NULL AUTO_INCREMENT,
    `event_time`        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `api_name`          VARCHAR(100) NOT NULL,
    `remaining_quota`   BIGINT       DEFAULT NULL,
    `trigger_threshold` INT          NOT NULL,
    `action_taken`      VARCHAR(50)  NOT NULL,
    `notify_status`     VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    `recorded_at`       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`)
);
CREATE INDEX IF NOT EXISTS `idx_ai_quota_event_time` ON `ai_quota_events` (`event_time`);
CREATE INDEX IF NOT EXISTS `idx_ai_quota_notify` ON `ai_quota_events` (`notify_status`);
ALTER TABLE `ai_analyses` ADD COLUMN IF NOT EXISTS `trigger_log_id` BIGINT DEFAULT NULL;
ALTER TABLE `ai_analyses` ADD COLUMN IF NOT EXISTS `keywords` CLOB DEFAULT NULL;
ALTER TABLE `ai_analyses` ADD COLUMN IF NOT EXISTS `mbti_type` VARCHAR(4) DEFAULT NULL;
ALTER TABLE `ai_analyses` ADD COLUMN IF NOT EXISTS `source` VARCHAR(10) DEFAULT NULL;
CREATE INDEX IF NOT EXISTS `idx_ai_trigger_log` ON `ai_analyses` (`trigger_log_id`);
CREATE INDEX IF NOT EXISTS `idx_ai_user_generated` ON `ai_analyses` (`user_id`, `generated_at`);
-- =============================================================================
-- 编辑草稿表 (自动保存未提交的编辑内容, 跨设备恢复)
-- =============================================================================
CREATE TABLE IF NOT EXISTS `drafts` (
    `draft_id`   BIGINT      NOT NULL AUTO_INCREMENT,
    `user_id`    BIGINT      NOT NULL,
    `draft_type` VARCHAR(20) NOT NULL,
    `content`    CLOB        DEFAULT NULL,
    `created_at` TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`draft_id`),
    CONSTRAINT `uk_draft_user_type` UNIQUE (`user_id`, `draft_type`)
);

-- =============================================================================
-- 内容模板表 (日志/任务模板, 云端存储, 跨设备同步)
-- =============================================================================
CREATE TABLE IF NOT EXISTS `templates` (
    `template_id`   BIGINT       NOT NULL AUTO_INCREMENT,
    `user_id`       BIGINT       NOT NULL,
    `template_type` VARCHAR(20)  NOT NULL,
    `name`          VARCHAR(200) NOT NULL,
    `content`       CLOB         DEFAULT NULL,
    `created_at`    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `deleted`       TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (`template_id`)
);
CREATE INDEX IF NOT EXISTS `idx_template_user_type` ON `templates` (`user_id`, `template_type`, `deleted`);
