-- H2 测试数据库初始化脚本
-- 对应生产环境的 MySQL 结构（H2 MODE=MySQL 兼容模式）

-- 禁用外键约束（测试环境需要）
SET FOREIGN_KEY_CHECKS=0;

-- 先删除所有表（按依赖倒序）
DROP TABLE IF EXISTS user_hierarchy;
DROP TABLE IF EXISTS task_tag_links;
DROP TABLE IF EXISTS task_tags;
DROP TABLE IF EXISTS task_assignees;
DROP TABLE IF EXISTS task_change_requests;
DROP TABLE IF EXISTS tasks;
DROP TABLE IF EXISTS teams;
DROP TABLE IF EXISTS departments;
DROP TABLE IF EXISTS daily_items;
DROP TABLE IF EXISTS log_comments;
DROP TABLE IF EXISTS work_logs;
DROP TABLE IF EXISTS ai_analysis_records;
DROP TABLE IF EXISTS divination_records;
DROP TABLE IF EXISTS mbti_reports;
DROP TABLE IF EXISTS calendar_events;
DROP TABLE IF EXISTS users;

-- 重新启用外键约束
SET FOREIGN_KEY_CHECKS=1;

-- 用户表
CREATE TABLE IF NOT EXISTS users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL,
    approval_status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    real_name VARCHAR(100),
    email VARCHAR(100),
    phone VARCHAR(20),
    signature VARCHAR(200),
    id_card VARCHAR(18),
    birthday DATE,
    avatar VARCHAR(500),
    department_id BIGINT,
    team_id BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0
);

-- 用户层级表
CREATE TABLE IF NOT EXISTS user_hierarchy (
    superior_id BIGINT NOT NULL,
    subordinate_id BIGINT NOT NULL,
    PRIMARY KEY (superior_id, subordinate_id),
    FOREIGN KEY (superior_id) REFERENCES users(id),
    FOREIGN KEY (subordinate_id) REFERENCES users(id)
);

-- 部门表
CREATE TABLE IF NOT EXISTS departments (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0
);

-- 团队表
CREATE TABLE IF NOT EXISTS teams (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    department_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    FOREIGN KEY (department_id) REFERENCES departments(id)
);

-- 任务表
CREATE TABLE IF NOT EXISTS tasks (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(256) NOT NULL,
    content VARCHAR(5000) NOT NULL,
    assignee_id BIGINT NOT NULL,
    dispatcher_id BIGINT NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'IN_PROGRESS',
    is_urgent TINYINT DEFAULT 0,
    start_time TIMESTAMP,
    end_time TIMESTAMP,
    progress_note VARCHAR(500),
    revision INT NOT NULL DEFAULT 0,
    assigned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    FOREIGN KEY (assignee_id) REFERENCES users(id),
    FOREIGN KEY (dispatcher_id) REFERENCES users(id)
);

-- 任务多负责人表
CREATE TABLE IF NOT EXISTS task_assignees (
    task_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    assigned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (task_id, user_id),
    FOREIGN KEY (task_id) REFERENCES tasks(id),
    FOREIGN KEY (user_id) REFERENCES users(id)
);

-- 任务标签表
CREATE TABLE IF NOT EXISTS task_tags (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(50) NOT NULL UNIQUE,
    name_key VARCHAR(50) NOT NULL UNIQUE,
    color_index INT NOT NULL DEFAULT 0,
    color INT NOT NULL DEFAULT 0,
    created_by BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    FOREIGN KEY (created_by) REFERENCES users(id)
);

-- 任务标签关联表
CREATE TABLE IF NOT EXISTS task_tag_links (
    task_id BIGINT NOT NULL,
    tag_id BIGINT NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    PRIMARY KEY (task_id, tag_id),
    FOREIGN KEY (task_id) REFERENCES tasks(id),
    FOREIGN KEY (tag_id) REFERENCES task_tags(id)
);

-- 任务变更请求表
CREATE TABLE IF NOT EXISTS task_change_requests (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    task_id BIGINT,
    actor_id BIGINT NOT NULL,
    requester_id BIGINT NOT NULL,
    approver_id BIGINT,
    base_revision INT NOT NULL DEFAULT 0,
    operation VARCHAR(50) NOT NULL,
    payload TEXT,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    reviewer_id BIGINT,
    review_reason VARCHAR(500),
    reason VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    reviewed_at TIMESTAMP,
    FOREIGN KEY (actor_id) REFERENCES users(id),
    FOREIGN KEY (requester_id) REFERENCES users(id),
    FOREIGN KEY (approver_id) REFERENCES users(id),
    FOREIGN KEY (reviewer_id) REFERENCES users(id)
);

-- 工作日志表
CREATE TABLE IF NOT EXISTS work_logs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    work_item VARCHAR(200) NOT NULL,
    work_content TEXT NOT NULL,
    completion_status VARCHAR(50) NOT NULL,
    time_cost DECIMAL(10,2),
    log_date DATE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    FOREIGN KEY (user_id) REFERENCES users(id),
    UNIQUE KEY uk_user_log_date (user_id, log_date)
);

-- 日志评论表
CREATE TABLE IF NOT EXISTS log_comments (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    log_id BIGINT NOT NULL,
    author_id BIGINT NOT NULL,
    content VARCHAR(500) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (log_id) REFERENCES work_logs(id),
    FOREIGN KEY (author_id) REFERENCES users(id)
);

-- AI 分析记录表
CREATE TABLE IF NOT EXISTS ai_analysis_records (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    trigger_log_id BIGINT,
    analysis_type VARCHAR(50) NOT NULL,
    prompt TEXT,
    result TEXT,
    source VARCHAR(50) NOT NULL DEFAULT 'EXTERNAL',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id)
);

-- 占卜记录表
CREATE TABLE IF NOT EXISTS divination_records (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    original_name VARCHAR(20) NOT NULL,
    mutual_name VARCHAR(20),
    changed_name VARCHAR(20),
    moving_line INT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id)
);

-- MBTI 报告表
CREATE TABLE IF NOT EXISTS mbti_reports (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    mbti_type VARCHAR(4) NOT NULL,
    answers TEXT NOT NULL,
    report TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id)
);

-- 日历事件表
CREATE TABLE IF NOT EXISTS calendar_events (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    title VARCHAR(200) NOT NULL,
    description VARCHAR(500),
    event_date DATE NOT NULL,
    event_type VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id)
);

-- 每日项表
CREATE TABLE IF NOT EXISTS daily_items (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    content VARCHAR(500) NOT NULL,
    item_date DATE NOT NULL,
    completed TINYINT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id),
    UNIQUE KEY uk_user_item_date (user_id, content, item_date)
);

-- 同步中继键表（幂等去重）
CREATE TABLE IF NOT EXISTS sync_relay_keys (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    client_key VARCHAR(200) NOT NULL UNIQUE,
    entity_type VARCHAR(50) NOT NULL,
    source_id BIGINT,
    cloud_id BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 初始化种子数据
-- 密码: test-login-password (BCrypt哈希)
INSERT INTO users (username, password_hash, role, approval_status, real_name) VALUES
('admin0', '$2b$12$vT94hqd.aKcyZhoGcHZ3weNv0QdeoNy20kOjk0XfxeGs4sKjlqnja', 'ADMIN', 'APPROVED', '管理员'),
('founder1', '$2b$12$vT94hqd.aKcyZhoGcHZ3weNv0QdeoNy20kOjk0XfxeGs4sKjlqnja', 'FOUNDER', 'APPROVED', '创始人'),
('dept_head2', '$2b$12$vT94hqd.aKcyZhoGcHZ3weNv0QdeoNy20kOjk0XfxeGs4sKjlqnja', 'DEPT_HEAD', 'APPROVED', '部门主管'),
('team_leader3', '$2b$12$vT94hqd.aKcyZhoGcHZ3weNv0QdeoNy20kOjk0XfxeGs4sKjlqnja', 'TEAM_LEADER', 'APPROVED', '团队负责人'),
('team_leader4', '$2b$12$vT94hqd.aKcyZhoGcHZ3weNv0QdeoNy20kOjk0XfxeGs4sKjlqnja', 'TEAM_LEADER', 'APPROVED', '团队负责人2'),
('employee5', '$2b$12$vT94hqd.aKcyZhoGcHZ3weNv0QdeoNy20kOjk0XfxeGs4sKjlqnja', 'EMPLOYEE', 'APPROVED', '员工1'),
('employee6', '$2b$12$vT94hqd.aKcyZhoGcHZ3weNv0QdeoNy20kOjk0XfxeGs4sKjlqnja', 'EMPLOYEE', 'APPROVED', '员工2');

INSERT INTO departments (name) VALUES ('技术部'), ('市场部'), ('产品部');

INSERT INTO teams (name, department_id) VALUES ('后端组', 1), ('前端组', 1), ('市场组', 2), ('产品组', 3);

INSERT INTO user_hierarchy (superior_id, subordinate_id) VALUES
(2, 3), (3, 4), (3, 5), (4, 6), (4, 7);