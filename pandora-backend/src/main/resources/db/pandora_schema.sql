-- =============================================================================
-- Project: Pandora（潘多拉）- 智能掌上工作系统 数据库建表脚本
-- =============================================================================
-- 版本: v1.0
-- 日期: 2026-09-11
-- 数据库: MySQL 8.0+
-- 字符集: utf8mb4
-- 引擎: InnoDB
-- 规范: 4NF（第四范式）
-- =============================================================================
-- 4NF 合规说明：
--   本脚本所有表均符合 4NF 规范。
--   特别说明：audit_rules_approvers 子表的存在是为了消除 audit_rules 表中
--   审批人列表的多值依赖。若在 audit_rules 中直接存储 approver_ids 列表
--   （如逗号分隔或 JSON 数组），会违反 4NF（存在非平凡的多值依赖）。
--   通过拆分为 audit_rules（主表）+ audit_rule_approvers（子表），
--   每个审批人独立一行，消除了多值依赖，符合 4NF。
-- =============================================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- =============================================================================
-- 1. users 用户主表
-- =============================================================================
DROP TABLE IF EXISTS `users`;
CREATE TABLE `users` (
    `id`              BIGINT       NOT NULL AUTO_INCREMENT          COMMENT '自增主键',
    `username`        VARCHAR(64)  NOT NULL                         COMMENT '用户名',
    `password_hash`   VARCHAR(128) NOT NULL                         COMMENT 'BCrypt加密密码',
    `role`            ENUM('ADMIN','FOUNDER','DEPT_HEAD','TEAM_LEADER','EMPLOYEE') NOT NULL DEFAULT 'EMPLOYEE' COMMENT '角色',
    `department_id`   BIGINT       DEFAULT NULL                     COMMENT '所属部门ID',
    `team_id`         BIGINT       DEFAULT NULL                     COMMENT '所属团队ID',
    `avatar`          VARCHAR(256) DEFAULT NULL                     COMMENT '头像URL',
    `theme_mode`      ENUM('AUTO','MANUAL') NOT NULL DEFAULT 'AUTO' COMMENT '主题模式',
    `manual_theme`    VARCHAR(32)  DEFAULT NULL                     COMMENT '手动指定主题名',
    `created_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`         TINYINT(1)   NOT NULL DEFAULT 0               COMMENT '软删除标记',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`),
    KEY `idx_department` (`department_id`),
    KEY `idx_team` (`team_id`),
    KEY `idx_role` (`role`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户主表';

-- =============================================================================
-- 2. departments 部门表
-- =============================================================================
DROP TABLE IF EXISTS `departments`;
CREATE TABLE `departments` (
    `id`            BIGINT      NOT NULL AUTO_INCREMENT             COMMENT '自增主键',
    `name`          VARCHAR(128) NOT NULL                           COMMENT '部门名称',
    `head_user_id`  BIGINT      DEFAULT NULL                        COMMENT '部门负责人ID',
    `parent_id`     BIGINT      DEFAULT NULL                        COMMENT '上级部门ID（自关联支持多级穿透）',
    `created_at`    DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP   COMMENT '创建时间',
    `updated_at`    DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`       TINYINT(1)  NOT NULL DEFAULT 0                  COMMENT '软删除标记',
    PRIMARY KEY (`id`),
    KEY `idx_parent` (`parent_id`),
    KEY `idx_head` (`head_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='部门表';

-- =============================================================================
-- 3. teams 团队表
-- =============================================================================
DROP TABLE IF EXISTS `teams`;
CREATE TABLE `teams` (
    `id`            BIGINT      NOT NULL AUTO_INCREMENT             COMMENT '自增主键',
    `name`          VARCHAR(128) NOT NULL                           COMMENT '团队名称',
    `leader_user_id` BIGINT     DEFAULT NULL                        COMMENT '团队长ID',
    `department_id` BIGINT      DEFAULT NULL                        COMMENT '所属部门ID',
    `created_at`    DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP   COMMENT '创建时间',
    `updated_at`    DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`       TINYINT(1)  NOT NULL DEFAULT 0                  COMMENT '软删除标记',
    PRIMARY KEY (`id`),
    KEY `idx_department` (`department_id`),
    KEY `idx_leader` (`leader_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='团队表';

-- =============================================================================
-- 4. tasks 任务表
-- =============================================================================
DROP TABLE IF EXISTS `tasks`;
CREATE TABLE `tasks` (
    `id`             BIGINT       NOT NULL AUTO_INCREMENT           COMMENT '自增主键',
    `name`           VARCHAR(256) NOT NULL                          COMMENT '任务名称',
    `priority`       ENUM('LOW','MEDIUM','HIGH','URGENT') NOT NULL DEFAULT 'MEDIUM' COMMENT '优先级',
    `status`         ENUM('PENDING','ACCEPTED','IN_PROGRESS','COMPLETED','CONFIRMED','REJECTED') NOT NULL DEFAULT 'PENDING' COMMENT '任务状态',
    `start_time`     DATETIME     DEFAULT NULL                      COMMENT '开始时间',
    `end_time`       DATETIME     DEFAULT NULL                      COMMENT '截止时间',
    `progress_note`  TEXT         DEFAULT NULL                      COMMENT '进度说明',
    `assignee_id`    BIGINT       NOT NULL                          COMMENT '责任人ID',
    `dispatcher_id`  BIGINT       DEFAULT NULL                      COMMENT '分发人ID（NULL表示个人任务）',
    `is_important`   TINYINT(1)   NOT NULL DEFAULT 0                COMMENT '是否重要',
    `is_urgent`      TINYINT(1)   NOT NULL DEFAULT 0                COMMENT '是否紧急',
    `version`        INT          NOT NULL DEFAULT 0                COMMENT '乐观锁版本号',
    `created_at`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`        TINYINT(1)   NOT NULL DEFAULT 0                COMMENT '软删除标记',
    PRIMARY KEY (`id`),
    KEY `idx_assignee_status` (`assignee_id`, `status`),
    KEY `idx_dispatcher` (`dispatcher_id`),
    KEY `idx_status` (`status`),
    KEY `idx_quadrant` (`is_important`, `is_urgent`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='任务表';

-- =============================================================================
-- 5. work_logs 工作日志表
-- =============================================================================
DROP TABLE IF EXISTS `work_logs`;
CREATE TABLE `work_logs` (
    `id`               BIGINT       NOT NULL AUTO_INCREMENT         COMMENT '自增主键',
    `user_id`          BIGINT       NOT NULL                        COMMENT '用户ID',
    `work_item`        VARCHAR(512) NOT NULL                        COMMENT '工作事项',
    `completion_status` ENUM('NOT_STARTED','IN_PROGRESS','COMPLETED') NOT NULL DEFAULT 'IN_PROGRESS' COMMENT '完成情况',
    `time_cost`        DECIMAL(5,2) DEFAULT NULL                    COMMENT '时间花费（小时）',
    `log_date`         DATE         NOT NULL                        COMMENT '日志日期',
    `created_at`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`          TINYINT(1)   NOT NULL DEFAULT 0              COMMENT '软删除标记',
    PRIMARY KEY (`id`),
    KEY `idx_user_date` (`user_id`, `log_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='工作日志表';

-- =============================================================================
-- 6. top10_works 十大重要工作表
-- =============================================================================
DROP TABLE IF EXISTS `top10_works`;
CREATE TABLE `top10_works` (
    `id`              BIGINT       NOT NULL AUTO_INCREMENT          COMMENT '自增主键',
    `user_id`         BIGINT       NOT NULL                         COMMENT '用户ID',
    `work_type`       ENUM('COMPANY_IMPORTANT','COMPANY_DISPATCH','PERSONAL_IMPORTANT') NOT NULL COMMENT '工作类型',
    `rank_order`      TINYINT      NOT NULL                         COMMENT '排序序号(1-10)',
    `content`         VARCHAR(512) NOT NULL                         COMMENT '工作内容',
    `related_task_id` BIGINT       DEFAULT NULL                     COMMENT '关联任务ID',
    `updated_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`         TINYINT(1)   NOT NULL DEFAULT 0               COMMENT '软删除标记',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_type_rank` (`user_id`, `work_type`, `rank_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='十大重要工作表';

-- =============================================================================
-- 7. mbti_reports MBTI报告表
-- =============================================================================
DROP TABLE IF EXISTS `mbti_reports`;
CREATE TABLE `mbti_reports` (
    `id`              BIGINT       NOT NULL AUTO_INCREMENT          COMMENT '自增主键',
    `user_id`         BIGINT       NOT NULL                         COMMENT '用户ID',
    `personality_type` VARCHAR(4) NOT NULL                         COMMENT '性格类型(如INTJ)',
    `answer_data`     JSON         DEFAULT NULL                     COMMENT '答题数据',
    `report_content`  TEXT         DEFAULT NULL                     COMMENT '报告内容',
    `generated_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '生成时间',
    `expire_at`       DATETIME     DEFAULT NULL                     COMMENT '过期时间',
    `deleted`         TINYINT(1)   NOT NULL DEFAULT 0               COMMENT '软删除标记',
    PRIMARY KEY (`id`),
    KEY `idx_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MBTI报告表';

-- =============================================================================
-- 8. ai_analyses AI分析记录表
-- =============================================================================
DROP TABLE IF EXISTS `ai_analyses`;
CREATE TABLE `ai_analyses` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT            COMMENT '自增主键',
    `user_id`       BIGINT       NOT NULL                           COMMENT '用户ID',
    `analysis_type` ENUM('MBTI','KEYWORD','SUGGESTION','INFO_MAP') NOT NULL COMMENT '分析类型',
    `input_summary` TEXT         DEFAULT NULL                       COMMENT '输入摘要',
    `result`        TEXT         DEFAULT NULL                       COMMENT '分析结果',
    `status`        ENUM('PENDING','SUCCESS','FAILED') NOT NULL DEFAULT 'PENDING' COMMENT '分析状态',
    `generated_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '生成时间',
    `deleted`       TINYINT(1)   NOT NULL DEFAULT 0                 COMMENT '软删除标记',
    PRIMARY KEY (`id`),
    KEY `idx_user_type` (`user_id`, `analysis_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI分析记录表';

-- =============================================================================
-- 9. audit_logs 审计日志表（永不删除）
-- =============================================================================
DROP TABLE IF EXISTS `audit_logs`;
CREATE TABLE `audit_logs` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT              COMMENT '自增主键',
    `operator_id` BIGINT       NOT NULL                             COMMENT '操作人ID',
    `action`      VARCHAR(32)  NOT NULL                             COMMENT '操作动作(CREATE/UPDATE/DELETE)',
    `target_type` VARCHAR(64)  NOT NULL                             COMMENT '目标类型',
    `target_id`   BIGINT       DEFAULT NULL                         COMMENT '目标ID',
    `detail`      TEXT         DEFAULT NULL                         COMMENT '操作详情',
    `operated_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP    COMMENT '操作时间',
    PRIMARY KEY (`id`),
    KEY `idx_operator_time` (`operator_id`, `operated_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='审计日志表（永不删除）';

-- =============================================================================
-- 10. invitation_requests 信息邀约请示表
-- =============================================================================
DROP TABLE IF EXISTS `invitation_requests`;
CREATE TABLE `invitation_requests` (
    `id`         BIGINT       NOT NULL AUTO_INCREMENT               COMMENT '自增主键',
    `from_user_id` BIGINT     NOT NULL                              COMMENT '发起人ID',
    `to_user_id` BIGINT       NOT NULL                              COMMENT '接收人ID',
    `content`    VARCHAR(512) NOT NULL                              COMMENT '请示内容',
    `status`     ENUM('PENDING','APPROVED','REJECTED') NOT NULL DEFAULT 'PENDING' COMMENT '请示状态',
    `created_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP     COMMENT '创建时间',
    `updated_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`    TINYINT(1)   NOT NULL DEFAULT 0                    COMMENT '软删除标记',
    PRIMARY KEY (`id`),
    KEY `idx_from_user` (`from_user_id`),
    KEY `idx_to_user` (`to_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='信息邀约请示表';

-- =============================================================================
-- 11a. audit_rules 分层审核规则表
-- =============================================================================
DROP TABLE IF EXISTS `audit_rules`;
CREATE TABLE `audit_rules` (
    `id`             BIGINT       NOT NULL AUTO_INCREMENT           COMMENT '自增主键',
    `trigger_action` VARCHAR(64)  NOT NULL                          COMMENT '触发动作(如TASK_DISPATCH)',
    `sequence`       INT          NOT NULL DEFAULT 1                COMMENT '审核顺序',
    `enabled`        TINYINT(1)   NOT NULL DEFAULT 1                COMMENT '是否启用',
    `deleted`        TINYINT(1)   NOT NULL DEFAULT 0                COMMENT '软删除标记',
    PRIMARY KEY (`id`),
    KEY `idx_trigger` (`trigger_action`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='分层审核规则表';

-- =============================================================================
-- 11b. audit_rule_approvers 审核规则审批人子表
-- =============================================================================
-- 4NF说明：此子表消除 audit_rules 中审批人列表的多值依赖
-- 每个审批人独立一行，符合4NF规范
-- =============================================================================
DROP TABLE IF EXISTS `audit_rule_approvers`;
CREATE TABLE `audit_rule_approvers` (
    `id`         BIGINT   NOT NULL AUTO_INCREMENT                   COMMENT '自增主键',
    `rule_id`    BIGINT   NOT NULL                                  COMMENT '审核规则ID',
    `approver_id` BIGINT  NOT NULL                                  COMMENT '审批人ID',
    `sequence`   INT      NOT NULL                                  COMMENT '审批顺序',
    PRIMARY KEY (`id`),
    KEY `idx_rule` (`rule_id`),
    KEY `idx_approver` (`approver_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='审核规则审批人子表（4NF拆分）';

-- =============================================================================
-- 初始化数据：管理员账号
-- =============================================================================
-- 密码: admin123 (BCrypt加密)
INSERT INTO `users` (`username`, `password_hash`, `role`) VALUES
('admin', '$2a$10$N.ZOn9G6/YLFixAOPHz/h.z7pCu6v2/nY6YYi5Jm7rXmJr4mZkKm', 'ADMIN');

SET FOREIGN_KEY_CHECKS = 1;

-- =============================================================================
-- 建表完成
-- =============================================================================