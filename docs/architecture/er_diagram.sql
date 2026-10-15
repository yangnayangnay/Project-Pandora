-- =============================================================================
-- Project Pandora - ER Diagram (Entity-Relationship Schema)
-- 生成日期: 2026-10-09
-- 数据库: H2 (MySQL兼容模式) / Oracle (可切换)
-- =============================================================================
-- 
-- 实体关系说明:
--   users 1───N tasks (assignee_id)      用户作为任务执行者
--   users 1───N tasks (dispatcher_id)    用户作为任务派发者
--   users 1───N work_logs                用户拥有工作日志
--   users 1───N top10_works              用户拥有Top10工作项
--   users 1───N mbti_reports             用户拥有MBTI报告
--   users 1───N ai_analyses              用户拥有AI分析记录
--   users 1───N audit_logs (operator_id) 用户操作审计日志
--   users 1───N notifications            用户接收通知
--   users 1───N mbti_analysis_requests   用户发起MBTI分析请求
--   users 1───N invitation_requests (from) 用户发送邀请
--   users 1───N invitation_requests (to)   用户接收邀请
--   users 1───N password_reset_requests   用户申请密码重置
--   departments 1───N users              部门拥有用户
--   departments 1───N teams              部门拥有团队
--   departments 1───N departments (parent) 部门父子层级
--   teams 1───N users                    团队拥有用户
--   tasks 1───N top10_works              任务关联Top10工作项
--   tasks 1───N notifications             任务关联通知
--   audit_rules 1───N audit_rule_approvers 审批规则拥有审批人
--   mbti_analysis_requests 1───1 mbti_analysis_results 分析请求对应结果
-- =============================================================================

-- =============================================================================
-- 1. 用户表 (核心实体)
-- =============================================================================
CREATE TABLE `users` (
    `id`                    BIGINT       NOT NULL AUTO_INCREMENT,
    `username`              VARCHAR(64)  NOT NULL,
    `password_hash`         VARCHAR(128) NOT NULL,
    `role`                  VARCHAR(20)  NOT NULL DEFAULT 'EMPLOYEE'
        COMMENT '角色: ADMIN/FOUNDER/DEPT_HEAD/TEAM_LEADER/EMPLOYEE',
    `department_id`         BIGINT       DEFAULT NULL,
    `team_id`               BIGINT       DEFAULT NULL,
    `avatar`                VARCHAR(256) DEFAULT NULL,
    `theme_mode`            VARCHAR(10)  NOT NULL DEFAULT 'AUTO',
    `manual_theme`          VARCHAR(32)  DEFAULT NULL,
    `email`                 VARCHAR(128) DEFAULT NULL,
    `real_name`             VARCHAR(64)  DEFAULT NULL,
    `id_card`               VARCHAR(18)  DEFAULT NULL,
    `approval_status`       VARCHAR(20)  NOT NULL DEFAULT 'APPROVED',
    `phone`                 VARCHAR(20)  DEFAULT NULL,
    `birthday`              DATE         DEFAULT NULL,
    `last_birthday_change`  TIMESTAMP    DEFAULT NULL,
    `show_completed_tasks`  TINYINT      NOT NULL DEFAULT 1,
    `created_at`            TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`            TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `deleted`               TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    CONSTRAINT `uk_username` UNIQUE (`username`),
    CONSTRAINT `fk_users_department` FOREIGN KEY (`department_id`) REFERENCES `departments`(`id`),
    CONSTRAINT `fk_users_team` FOREIGN KEY (`team_id`) REFERENCES `teams`(`id`)
) COMMENT = '用户表 - 系统核心实体，存储用户认证与个人信息';

CREATE INDEX `idx_users_department` ON `users` (`department_id`);
CREATE INDEX `idx_users_team` ON `users` (`team_id`);
CREATE INDEX `idx_users_role` ON `users` (`role`);
CREATE INDEX `idx_users_email` ON `users` (`email`);
CREATE INDEX `idx_users_id_card` ON `users` (`id_card`);
CREATE INDEX `idx_users_approval` ON `users` (`approval_status`);

-- =============================================================================
-- 2. 部门表 (组织架构)
-- =============================================================================
CREATE TABLE `departments` (
    `id`            BIGINT      NOT NULL AUTO_INCREMENT,
    `name`          VARCHAR(128) NOT NULL,
    `head_user_id`  BIGINT      DEFAULT NULL,
    `parent_id`     BIGINT      DEFAULT NULL,
    `created_at`    TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`    TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `deleted`       TINYINT     NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    CONSTRAINT `fk_dept_parent` FOREIGN KEY (`parent_id`) REFERENCES `departments`(`id`),
    CONSTRAINT `fk_dept_head` FOREIGN KEY (`head_user_id`) REFERENCES `users`(`id`)
) COMMENT = '部门表 - 组织架构层级，支持父子部门关系';

CREATE INDEX `idx_dept_parent` ON `departments` (`parent_id`);
CREATE INDEX `idx_dept_head` ON `departments` (`head_user_id`);

-- =============================================================================
-- 3. 团队表 (组织架构)
-- =============================================================================
CREATE TABLE `teams` (
    `id`            BIGINT      NOT NULL AUTO_INCREMENT,
    `name`          VARCHAR(128) NOT NULL,
    `leader_user_id` BIGINT     DEFAULT NULL,
    `department_id` BIGINT      DEFAULT NULL,
    `created_at`    TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`    TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `deleted`       TINYINT     NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    CONSTRAINT `fk_team_department` FOREIGN KEY (`department_id`) REFERENCES `departments`(`id`),
    CONSTRAINT `fk_team_leader` FOREIGN KEY (`leader_user_id`) REFERENCES `users`(`id`)
) COMMENT = '团队表 - 隶属于部门的团队单元';

CREATE INDEX `idx_team_department` ON `teams` (`department_id`);
CREATE INDEX `idx_team_leader` ON `teams` (`leader_user_id`);

-- =============================================================================
-- 4. 任务表 (核心业务实体)
-- =============================================================================
CREATE TABLE `tasks` (
    `id`             BIGINT       NOT NULL AUTO_INCREMENT,
    `name`           VARCHAR(256) NOT NULL,
    `priority`       VARCHAR(10)  NOT NULL DEFAULT 'MEDIUM'
        COMMENT '优先级: HIGH/MEDIUM/LOW (兼容保留)',
    `status`         VARCHAR(20)  NOT NULL DEFAULT 'PENDING'
        COMMENT '状态: PENDING/IN_PROGRESS/COMPLETED/CONFIRMED',
    `start_time`     TIMESTAMP    DEFAULT NULL,
    `end_time`       TIMESTAMP    DEFAULT NULL,
    `progress_note`  CLOB         DEFAULT NULL,
    `assignee_id`    BIGINT       NOT NULL,
    `dispatcher_id`  BIGINT       DEFAULT NULL,
    `is_important`   TINYINT      NOT NULL DEFAULT 0
        COMMENT '是否重要 (0=否, 1=是)',
    `is_urgent`      TINYINT      NOT NULL DEFAULT 0
        COMMENT '是否紧急 (0=否, 1=是)',
    `version`        INT          NOT NULL DEFAULT 0,
    `created_at`     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `deleted`        TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    CONSTRAINT `fk_task_assignee` FOREIGN KEY (`assignee_id`) REFERENCES `users`(`id`),
    CONSTRAINT `fk_task_dispatcher` FOREIGN KEY (`dispatcher_id`) REFERENCES `users`(`id`)
) COMMENT = '任务表 - 任务分发与执行的核心实体';

CREATE INDEX `idx_task_assignee_status` ON `tasks` (`assignee_id`, `status`);
CREATE INDEX `idx_task_dispatcher` ON `tasks` (`dispatcher_id`);
CREATE INDEX `idx_task_status` ON `tasks` (`status`);
CREATE INDEX `idx_task_quadrant` ON `tasks` (`is_important`, `is_urgent`);

-- =============================================================================
-- 5. 工作日志表 (每日工作小结)
-- =============================================================================
CREATE TABLE `work_logs` (
    `id`               BIGINT       NOT NULL AUTO_INCREMENT,
    `user_id`          BIGINT       NOT NULL,
    `work_item`        VARCHAR(512) NOT NULL,
    `completion_status` VARCHAR(20) NOT NULL DEFAULT 'PENDING'
        COMMENT '完成状态: PENDING(未完成)/COMPLETED(完成)',
    `time_cost`        DECIMAL(5,2) DEFAULT NULL,
    `log_date`         DATE         NOT NULL,
    `created_at`       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `deleted`          TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    CONSTRAINT `fk_log_user` FOREIGN KEY (`user_id`) REFERENCES `users`(`id`)
) COMMENT = '工作日志表 - 每日工作小结，二元状态(未完成/完成)';

CREATE INDEX `idx_log_user_date` ON `work_logs` (`user_id`, `log_date`);

-- =============================================================================
-- 6. Top10工作项表
-- =============================================================================
CREATE TABLE `top10_works` (
    `id`              BIGINT       NOT NULL AUTO_INCREMENT,
    `user_id`         BIGINT       NOT NULL,
    `work_type`       VARCHAR(30)  NOT NULL
        COMMENT '工作类型: COMPANY_IMPORTANT/COMPANY_DISPATCH/PERSONAL_IMPORTANT/PERSONAL_LOG',
    `rank_order`      TINYINT      NOT NULL,
    `content`         VARCHAR(512) NOT NULL,
    `related_task_id` BIGINT       DEFAULT NULL,
    `updated_at`      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `deleted`         TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    CONSTRAINT `uk_user_type_rank` UNIQUE (`user_id`, `work_type`, `rank_order`),
    CONSTRAINT `fk_top10_user` FOREIGN KEY (`user_id`) REFERENCES `users`(`id`),
    CONSTRAINT `fk_top10_task` FOREIGN KEY (`related_task_id`) REFERENCES `tasks`(`id`)
) COMMENT = 'Top10工作项表 - 四象限矩阵数据源';

-- =============================================================================
-- 7. MBTI报告表 (旧版)
-- =============================================================================
CREATE TABLE `mbti_reports` (
    `id`              BIGINT       NOT NULL AUTO_INCREMENT,
    `user_id`         BIGINT       NOT NULL,
    `personality_type` VARCHAR(4) NOT NULL,
    `answer_data`     CLOB         DEFAULT NULL,
    `report_content`  CLOB         DEFAULT NULL,
    `generated_at`    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `expire_at`       TIMESTAMP    DEFAULT NULL,
    `deleted`         TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    CONSTRAINT `fk_mbti_user` FOREIGN KEY (`user_id`) REFERENCES `users`(`id`)
) COMMENT = 'MBTI报告表 - 旧版问卷式MBTI结果';

CREATE INDEX `idx_mbti_user` ON `mbti_reports` (`user_id`);

-- =============================================================================
-- 8. AI分析表
-- =============================================================================
CREATE TABLE `ai_analyses` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT,
    `user_id`       BIGINT       NOT NULL,
    `analysis_type` VARCHAR(20)  NOT NULL
        COMMENT '分析类型: PROJECT_COMPLETION/KEYWORD/SUGGESTION等',
    `input_summary` CLOB         DEFAULT NULL,
    `result`        CLOB         DEFAULT NULL,
    `status`        VARCHAR(10)  NOT NULL DEFAULT 'PENDING',
    `generated_at`  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `deleted`       TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    CONSTRAINT `fk_ai_user` FOREIGN KEY (`user_id`) REFERENCES `users`(`id`)
) COMMENT = 'AI分析表 - AI分析任务记录与结果';

CREATE INDEX `idx_ai_user_type` ON `ai_analyses` (`user_id`, `analysis_type`);

-- =============================================================================
-- 9. 审计日志表
-- =============================================================================
CREATE TABLE `audit_logs` (
    `id`              BIGINT       NOT NULL AUTO_INCREMENT,
    `operator_id`     BIGINT       NOT NULL,
    `action`          VARCHAR(32)  NOT NULL,
    `target_type`     VARCHAR(64)  NOT NULL,
    `target_id`       BIGINT       DEFAULT NULL,
    `detail`          CLOB         DEFAULT NULL,
    `operation_type`  VARCHAR(32)  DEFAULT NULL,
    `operation_content` VARCHAR(500) DEFAULT NULL,
    `operated_at`     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    CONSTRAINT `fk_audit_operator` FOREIGN KEY (`operator_id`) REFERENCES `users`(`id`)
) COMMENT = '审计日志表 - 用户操作审计追踪';

CREATE INDEX `idx_audit_operator_time` ON `audit_logs` (`operator_id`, `operated_at`);
CREATE INDEX `idx_audit_type_time` ON `audit_logs` (`operation_type`, `operated_at`);

-- =============================================================================
-- 10. 邀请请求表
-- =============================================================================
CREATE TABLE `invitation_requests` (
    `id`         BIGINT       NOT NULL AUTO_INCREMENT,
    `from_user_id` BIGINT     NOT NULL,
    `to_user_id` BIGINT       NOT NULL,
    `content`    VARCHAR(512) NOT NULL,
    `status`     VARCHAR(10)  NOT NULL DEFAULT 'PENDING',
    `created_at` TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `deleted`    TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    CONSTRAINT `fk_inv_from` FOREIGN KEY (`from_user_id`) REFERENCES `users`(`id`),
    CONSTRAINT `fk_inv_to` FOREIGN KEY (`to_user_id`) REFERENCES `users`(`id`)
) COMMENT = '邀请请求表 - 用户间邀请';

CREATE INDEX `idx_inv_from_user` ON `invitation_requests` (`from_user_id`);
CREATE INDEX `idx_inv_to_user` ON `invitation_requests` (`to_user_id`);

-- =============================================================================
-- 11. 审批规则表
-- =============================================================================
CREATE TABLE `audit_rules` (
    `id`             BIGINT       NOT NULL AUTO_INCREMENT,
    `trigger_action` VARCHAR(64)  NOT NULL,
    `sequence`       INT          NOT NULL DEFAULT 1,
    `enabled`        TINYINT      NOT NULL DEFAULT 1,
    `deleted`        TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`)
) COMMENT = '审批规则表 - 定义操作触发审批规则';

CREATE INDEX `idx_rule_trigger` ON `audit_rules` (`trigger_action`);

-- =============================================================================
-- 12. 审批规则审批人表
-- =============================================================================
CREATE TABLE `audit_rule_approvers` (
    `id`         BIGINT   NOT NULL AUTO_INCREMENT,
    `rule_id`    BIGINT   NOT NULL,
    `approver_id` BIGINT  NOT NULL,
    `sequence`   INT      NOT NULL,
    PRIMARY KEY (`id`),
    CONSTRAINT `fk_approver_rule` FOREIGN KEY (`rule_id`) REFERENCES `audit_rules`(`id`),
    CONSTRAINT `fk_approver_user` FOREIGN KEY (`approver_id`) REFERENCES `users`(`id`)
) COMMENT = '审批规则审批人表 - 审批流程中的审批人序列';

CREATE INDEX `idx_approver_rule` ON `audit_rule_approvers` (`rule_id`);
CREATE INDEX `idx_approver_user` ON `audit_rule_approvers` (`approver_id`);

-- =============================================================================
-- 13. 员工预登记表
-- =============================================================================
CREATE TABLE `employee_records` (
    `id`              BIGINT       NOT NULL AUTO_INCREMENT,
    `real_name`       VARCHAR(64)  NOT NULL,
    `id_card`         VARCHAR(18)  NOT NULL,
    `email`           VARCHAR(128) DEFAULT NULL,
    `phone`           VARCHAR(20)  DEFAULT NULL,
    `department_id`   BIGINT       DEFAULT NULL,
    `team_id`         BIGINT       DEFAULT NULL,
    `role`            VARCHAR(20)  NOT NULL DEFAULT 'EMPLOYEE',
    `position`        VARCHAR(64)  DEFAULT NULL,
    `status`          VARCHAR(20)  NOT NULL DEFAULT 'PENDING'
        COMMENT 'PENDING/REGISTERED/DISABLED',
    `registered_user_id` BIGINT    DEFAULT NULL,
    `created_at`      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `deleted`         TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    CONSTRAINT `uk_emp_id_card` UNIQUE (`id_card`),
    CONSTRAINT `fk_emp_dept` FOREIGN KEY (`department_id`) REFERENCES `departments`(`id`),
    CONSTRAINT `fk_emp_team` FOREIGN KEY (`team_id`) REFERENCES `teams`(`id`),
    CONSTRAINT `fk_emp_registered` FOREIGN KEY (`registered_user_id`) REFERENCES `users`(`id`)
) COMMENT = '员工预登记表 - 管理员预录入员工信息，注册时自动匹配';

CREATE INDEX `idx_emp_status` ON `employee_records` (`status`);
CREATE INDEX `idx_emp_dept` ON `employee_records` (`department_id`);

-- =============================================================================
-- 14. 密码重置请求表
-- =============================================================================
CREATE TABLE `password_reset_requests` (
    `id`              BIGINT       NOT NULL AUTO_INCREMENT,
    `user_id`         BIGINT       DEFAULT NULL,
    `username`        VARCHAR(64)  NOT NULL,
    `status`          VARCHAR(20)  NOT NULL DEFAULT 'PENDING'
        COMMENT 'PENDING/APPROVED/REJECTED',
    `request_reason`  VARCHAR(256) DEFAULT NULL,
    `reject_reason`   VARCHAR(256) DEFAULT NULL,
    `processed_by`    BIGINT       DEFAULT NULL,
    `processed_at`    TIMESTAMP    DEFAULT NULL,
    `created_at`      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    CONSTRAINT `fk_prr_user` FOREIGN KEY (`user_id`) REFERENCES `users`(`id`),
    CONSTRAINT `fk_prr_processor` FOREIGN KEY (`processed_by`) REFERENCES `users`(`id`)
) COMMENT = '密码重置请求表 - 用户忘记密码时申请，管理员审批后重置';

CREATE INDEX `idx_prr_status` ON `password_reset_requests` (`status`);

-- =============================================================================
-- 15. 通知表
-- =============================================================================
CREATE TABLE `notifications` (
    `id`              BIGINT       NOT NULL AUTO_INCREMENT,
    `target_user_id`  BIGINT       NOT NULL,
    `type`            VARCHAR(20)  NOT NULL
        COMMENT '通知类型: NEW_TASK/STATUS_CHANGE等',
    `task_id`         BIGINT       DEFAULT NULL,
    `content`         VARCHAR(256) NOT NULL,
    `is_read`         TINYINT      NOT NULL DEFAULT 0,
    `created_at`      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    CONSTRAINT `fk_notif_user` FOREIGN KEY (`target_user_id`) REFERENCES `users`(`id`),
    CONSTRAINT `fk_notif_task` FOREIGN KEY (`task_id`) REFERENCES `tasks`(`id`)
) COMMENT = '通知表 - 应用内消息通知';

CREATE INDEX `idx_notif_user_created` ON `notifications` (`target_user_id`, `created_at`);
CREATE INDEX `idx_notif_user_unread` ON `notifications` (`target_user_id`, `is_read`);

-- =============================================================================
-- 16. MBTI分析请求表 (新版命理分析)
-- =============================================================================
CREATE TABLE `mbti_analysis_requests` (
    `id`                 BIGINT       NOT NULL AUTO_INCREMENT,
    `user_id`            BIGINT       NOT NULL,
    `name`               VARCHAR(64)  NOT NULL,
    `gender`             VARCHAR(10)  NOT NULL,
    `time_type`          VARCHAR(10)  NOT NULL COMMENT '公历/农历',
    `birth_time`         VARCHAR(32)  NOT NULL,
    `birth_place`        VARCHAR(128) NOT NULL,
    `current_residence`  VARCHAR(128) NOT NULL,
    `status`             VARCHAR(20)  NOT NULL DEFAULT 'PENDING'
        COMMENT 'PENDING/COMPLETED/FAILED',
    `created_at`         TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    CONSTRAINT `fk_mbti_req_user` FOREIGN KEY (`user_id`) REFERENCES `users`(`id`)
) COMMENT = 'MBTI分析请求表 - 命理四柱分析请求';

CREATE INDEX `idx_mbti_req_user_status` ON `mbti_analysis_requests` (`user_id`, `status`);

-- =============================================================================
-- 17. MBTI分析结果表
-- =============================================================================
CREATE TABLE `mbti_analysis_results` (
    `id`            BIGINT      NOT NULL AUTO_INCREMENT,
    `request_id`    BIGINT      NOT NULL,
    `year_pillar`   VARCHAR(32) NOT NULL COMMENT '年柱(天干地支)',
    `month_pillar`  VARCHAR(32) NOT NULL COMMENT '月柱',
    `day_pillar`    VARCHAR(32) NOT NULL COMMENT '日柱',
    `hour_pillar`   VARCHAR(32) NOT NULL COMMENT '时柱',
    `insights`      CLOB        DEFAULT NULL COMMENT '深度洞察(多行文本)',
    `generated_at`  TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    CONSTRAINT `uk_mbti_result_req` UNIQUE (`request_id`),
    CONSTRAINT `fk_mbti_result_req` FOREIGN KEY (`request_id`) REFERENCES `mbti_analysis_requests`(`id`)
) COMMENT = 'MBTI分析结果表 - 命理四柱与深度洞察';

-- =============================================================================
-- ER关系总结
-- =============================================================================
-- 一对多关系:
--   users → tasks (assignee_id, dispatcher_id)
--   users → work_logs
--   users → top10_works
--   users → mbti_reports
--   users → ai_analyses
--   users → audit_logs
--   users → notifications
--   users → mbti_analysis_requests
--   users → invitation_requests (from, to)
--   users → password_reset_requests (user, processor)
--   users → employee_records (registered)
--   users → audit_rule_approvers
--   departments → users
--   departments → teams
--   departments → departments (parent)
--   teams → users
--   tasks → top10_works
--   tasks → notifications
--   audit_rules → audit_rule_approvers
--
-- 一对一关系:
--   mbti_analysis_requests → mbti_analysis_results
--
-- 实体总数: 17
-- 关系总数: 20+
-- =============================================================================