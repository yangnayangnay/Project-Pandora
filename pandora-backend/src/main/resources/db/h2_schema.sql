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

CREATE TABLE IF NOT EXISTS `top10_works` (
    `id`              BIGINT       NOT NULL AUTO_INCREMENT,
    `user_id`         BIGINT       NOT NULL,
    `work_type`       VARCHAR(30)  NOT NULL,
    `rank_order`      TINYINT      NOT NULL,
    `content`         VARCHAR(512) NOT NULL,
    `related_task_id` BIGINT       DEFAULT NULL,
    `updated_at`      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `deleted`         TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    CONSTRAINT `uk_user_type_rank` UNIQUE (`user_id`, `work_type`, `rank_order`)
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
-- 初始化数据：管理员账号 (密码: admin123, BCrypt加密)
-- =============================================================================
MERGE INTO `users` (`id`, `username`, `password_hash`, `role`) KEY(`id`) VALUES
(1, 'admin', '$2a$10$sT4ev7WP3x1xvVoudhxUkeXoXOtQHp3eq2LNQRLQTvPoL39OXwSm6', 'ADMIN');
