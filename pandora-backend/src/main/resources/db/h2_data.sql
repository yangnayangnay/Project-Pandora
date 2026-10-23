-- =============================================================================
-- Project: Pandora - H2 测试数据 (MySQL Compatibility Mode)
-- =============================================================================
-- 密码统一为 admin123 (BCrypt: $2a$10$sT4ev7WP3x1xvVoudhxUkeXoXOtQHp3eq2LNQRLQTvPoL39OXwSm6)
-- 使用 MERGE INTO 避免重复插入
-- =============================================================================

-- 用户数据 (admin0已存在, 补充4个用户, 用户名统一使用编号后缀)
-- Round26 E4: 追加 real_name 永久基线（uid5 RegTestEmployee 为 R25 巡检基线保留；uid6~7 无基线保持 NULL）
MERGE INTO `users` (`id`, `username`, `password_hash`, `role`, `department_id`, `team_id`, `real_name`) KEY(`id`) VALUES
(2, 'founder1',      '$2a$10$sT4ev7WP3x1xvVoudhxUkeXoXOtQHp3eq2LNQRLQTvPoL39OXwSm6', 'FOUNDER',     1, 1, '创始人'),
(3, 'dept_head2',    '$2a$10$sT4ev7WP3x1xvVoudhxUkeXoXOtQHp3eq2LNQRLQTvPoL39OXwSm6', 'DEPT_HEAD',   1, 1, '部门负责人'),
(4, 'team_leader3',  '$2a$10$sT4ev7WP3x1xvVoudhxUkeXoXOtQHp3eq2LNQRLQTvPoL39OXwSm6', 'TEAM_LEADER', 1, 2, '组长'),
(5, 'employee4',     '$2a$10$sT4ev7WP3x1xvVoudhxUkeXoXOtQHp3eq2LNQRLQTvPoL39OXwSm6', 'EMPLOYEE',    1, 2, 'RegTestEmployee'),
(6, 'zhang_san4',    '$2a$10$sT4ev7WP3x1xvVoudhxUkeXoXOtQHp3eq2LNQRLQTvPoL39OXwSm6', 'EMPLOYEE',    2, 3, NULL),
(7, 'li_si3',        '$2a$10$sT4ev7WP3x1xvVoudhxUkeXoXOtQHp3eq2LNQRLQTvPoL39OXwSm6', 'TEAM_LEADER', 2, 3, NULL);

-- 部门数据
MERGE INTO `departments` (`id`, `name`, `head_user_id`, `parent_id`) KEY(`id`) VALUES
(1, '研发部',   3, NULL),
(2, '产品部',   7, NULL);

-- 团队数据
MERGE INTO `teams` (`id`, `name`, `leader_user_id`, `department_id`) KEY(`id`) VALUES
(1, '架构组', 4, 1),
(2, '前端组', 4, 1),
(3, '设计组', 7, 2);

-- 任务数据 (15条, 各种状态和优先级)
MERGE INTO `tasks` (`id`, `name`, `priority`, `status`, `start_time`, `end_time`, `progress_note`, `assignee_id`, `dispatcher_id`, `is_important`, `is_urgent`) KEY(`id`) VALUES
(1,  '完成系统架构设计文档',         'HIGH',   'COMPLETED',   '2026-10-01 09:00:00', '2026-10-10 18:00:00', '架构设计已完成, 通过评审',     4, 2, 1, 1),
(2,  '开发用户认证模块',             'HIGH',   'IN_PROGRESS', '2026-10-05 09:00:00', '2026-10-20 18:00:00', 'JWT认证开发中, 预计本周完成',   5, 4, 1, 0),
(3,  '编写API接口文档',              'MEDIUM', 'IN_PROGRESS', '2026-10-10 09:00:00', '2026-10-25 18:00:00', '已完成80%接口文档',             5, 4, 0, 0),
(4,  '设计数据库ER图',               'HIGH',   'COMPLETED',   '2026-10-01 09:00:00', '2026-10-08 18:00:00', 'ER图已完成, 4NF合规',           4, 2, 1, 0),
(5,  '实现任务分发功能',             'URGENT', 'IN_PROGRESS', '2026-10-15 09:00:00', '2026-10-22 18:00:00', '分发逻辑开发中',                4, 2, 1, 1),
(6,  '前端登录页面开发',             'HIGH',   'COMPLETED',   '2026-10-03 09:00:00', '2026-10-12 18:00:00', '登录页已完成, 含服务器连接检测', 5, 4, 1, 0),
(7,  '前端底部导航优化',             'MEDIUM', 'COMPLETED',   '2026-10-10 09:00:00', '2026-10-15 18:00:00', '5项导航已配置完成',             5, 4, 0, 0),

(10, 'AI关键词分析接口对接',         'HIGH',   'IN_PROGRESS', '2026-10-14 09:00:00', '2026-10-28 18:00:00', 'DeepSeek API对接中',            4, 2, 1, 0),
(11, 'MBTI性格测试页面开发',         'MEDIUM', 'PENDING',     NULL,                  '2026-10-05 18:00:00', NULL,                            6, 7, 0, 0),
(12, '飞书多维表格导入功能',         'LOW',    'PENDING',     NULL,                  '2026-10-10 18:00:00', NULL,                            6, 7, 0, 0),
(13, '工作周报自动生成',             'MEDIUM', 'PENDING',     NULL,                  '2026-10-08 18:00:00', NULL,                            5, 4, 0, 0),
(14, '单元测试覆盖率提升',           'MEDIUM', 'IN_PROGRESS', '2026-10-15 09:00:00', '2026-10-30 18:00:00', '当前覆盖率45%, 目标80%',        5, 4, 0, 0),
(15, '非功能需求测试用例编写',       'LOW',    'PENDING',     NULL,                  '2026-10-15 18:00:00', NULL,                            6, 7, 0, 0);

-- 工作日志数据 (12条)
MERGE INTO `work_logs` (`id`, `user_id`, `work_item`, `completion_status`, `time_cost`, `log_date`) KEY(`id`) VALUES
(1,  5, '完成登录页面开发, 添加服务器连接检测',     'COMPLETED',   6.00, '2026-10-12'),
(2,  5, '底部导航从6项改为5项, 删除首页',           'COMPLETED',   2.50, '2026-10-13'),

(5,  4, '系统架构设计文档评审',                     'COMPLETED',   5.00, '2026-10-10'),
(6,  4, '数据库ER图设计, 4NF合规检查',              'COMPLETED',   8.00, '2026-10-08'),
(7,  4, '任务分发功能开发',                         'IN_PROGRESS', 4.50, '2026-10-15'),
(8,  4, 'AI关键词分析接口对接',                     'IN_PROGRESS', 3.00, '2026-10-16'),
(9,  5, '视图页任务表格Tab开发',                    'COMPLETED',   3.50, '2026-10-16'),
(10, 5, '导图页接入getMindMap API, 四大板块动态显示', 'COMPLETED', 4.00, '2026-10-17'),
(11, 5, '生日祝福功能从AI地图移到我的页面',         'COMPLETED',   1.50, '2026-10-18'),
(12, 5, '修复ViewFragment中setupLogNavigation引用',  'COMPLETED',  0.50, '2026-10-20');

-- 十大重要工作 (示例)
MERGE INTO `top10_works` (`id`, `user_id`, `work_type`, `rank_order`, `content`, `related_task_id`) KEY(`id`) VALUES
(1,  2, 'COMPANY_IMPORTANT',  1, '完成潘多拉系统MVP交付',           1),
(2,  2, 'COMPANY_IMPORTANT',  2, '通过课程实训验收',                 NULL),
(3,  2, 'COMPANY_IMPORTANT',  3, '飞书多维表格项目看板维护',         NULL),
(4,  2, 'COMPANY_DISPATCH',   1, '系统架构设计文档',                 1),
(5,  2, 'COMPANY_DISPATCH',   2, '数据库ER图设计',                   4),
(6,  2, 'COMPANY_DISPATCH',   3, '任务分发功能开发',                 5),
(7,  5, 'PERSONAL_IMPORTANT', 1, '前端登录页面开发',                 6),
(8,  5, 'PERSONAL_IMPORTANT', 2, '底部导航优化',                     7);
-- 员工预登记数据 (管理员预录入, 注册时自动匹配)
MERGE INTO `employee_records` (`id`, `real_name`, `id_card`, `email`, `phone`, `department_id`, `team_id`, `role`, `position`, `status`) KEY(`id`) VALUES
(1, '张三', '110101199001011234', 'zhang_san@pandora.com',   '13800138001', 1, 1, 'FOUNDER',     '创始人',     'REGISTERED'),
(2, '李四', '110101199002022345', 'li_si@pandora.com',       '13800138002', 1, 1, 'DEPT_HEAD',   '研发部负责人', 'REGISTERED'),
(3, '王五', '110101199003033456', 'wang_wu@pandora.com',     '13800138003', 1, 2, 'TEAM_LEADER', '前端组长',   'REGISTERED'),
(4, '赵六', '110101199004044567', 'zhao_liu@pandora.com',    '13800138004', 1, 2, 'EMPLOYEE',    '前端工程师', 'REGISTERED'),
(5, '孙七', '110101199005055678', 'sun_qi@pandora.com',      '13800138005', 2, 3, 'TEAM_LEADER', '设计组长',   'REGISTERED'),
(6, '周八', '110101199006066789', 'zhou_ba@pandora.com',     '13800138006', 2, 3, 'EMPLOYEE',    'UI设计师',   'PENDING'),
(7, '吴九', '110101199007077890', 'wu_jiu@pandora.com',      '13800138007', 1, 1, 'EMPLOYEE',    '后端工程师', 'PENDING'),
(8, '郑十', '110101199008088901', 'zheng_shi@pandora.com',   '13800138008', 1, 2, 'EMPLOYEE',    '测试工程师', 'PENDING');
-- 统一所有用户和员工预登记的邮箱
UPDATE `users` SET `email` = '2120951033@qq.com' WHERE `deleted` = 0;
UPDATE `employee_records` SET `email` = '2120951033@qq.com' WHERE `deleted` = 0;-- 统一所有用户和员工预登记的手机号
UPDATE `users` SET `phone` = '19310693809' WHERE `deleted` = 0;
UPDATE `employee_records` SET `phone` = '19310693809' WHERE `deleted` = 0;
-- =============================================================================
-- yjl 测试账号（密码与联系方式由项目所有者另行提供）
-- 组织架构: 创始人 → 部门老总 → 团队长 → 员工
-- =============================================================================
MERGE INTO `users` (`id`, `username`, `password_hash`, `role`, `department_id`, `team_id`, `email`, `real_name`, `phone`, `approval_status`) KEY(`id`) VALUES
(100, 'yjl_founder1',      '$2a$10$fi4vy2jU0b0HW88235.5NuVFSaJi1Ii4DJBTaN5O1cgcY8X62ba8a', 'FOUNDER',     3, 4, '2120951033@qq.com', 'YJL创始人',   '19310693809', 'APPROVED'),
(101, 'yjl_dept_head2',    '$2a$10$fi4vy2jU0b0HW88235.5NuVFSaJi1Ii4DJBTaN5O1cgcY8X62ba8a', 'DEPT_HEAD',   3, 4, '2120951033@qq.com', 'YJL部门老总', '19310693809', 'APPROVED'),
(102, 'yjl_team_leader3',  '$2a$10$fi4vy2jU0b0HW88235.5NuVFSaJi1Ii4DJBTaN5O1cgcY8X62ba8a', 'TEAM_LEADER', 3, 4, '2120951033@qq.com', 'YJL团队长',   '19310693809', 'APPROVED'),
(103, 'yjl_employee4',     '$2a$10$fi4vy2jU0b0HW88235.5NuVFSaJi1Ii4DJBTaN5O1cgcY8X62ba8a', 'EMPLOYEE',    3, 4, '2120951033@qq.com', 'YJL员工',     '19310693809', 'APPROVED'),
(104, 'yjl_employee24',    '$2a$10$fi4vy2jU0b0HW88235.5NuVFSaJi1Ii4DJBTaN5O1cgcY8X62ba8a', 'EMPLOYEE',    3, 4, '2120951033@qq.com', 'YJL员工2',    '19310693809', 'APPROVED');

-- yjl 部门和团队
MERGE INTO `departments` (`id`, `name`, `head_user_id`, `parent_id`) KEY(`id`) VALUES
(3, 'YJL运营部', 101, NULL);

MERGE INTO `teams` (`id`, `name`, `leader_user_id`, `department_id`) KEY(`id`) VALUES
(4, 'YJL运营组', 102, 3);

-- yjl 测试任务 (创始人派发给部门老总, 部门老总派发给团队长, 团队长派发给员工)
MERGE INTO `tasks` (`id`, `name`, `priority`, `status`, `start_time`, `end_time`, `assignee_id`, `dispatcher_id`, `is_important`, `is_urgent`, `progress_note`) KEY(`id`) VALUES
(100, 'YJL-部门季度规划',   'HIGH',   'IN_PROGRESS', '2026-10-01 09:00:00', '2026-10-30 18:00:00', 101, 100, 1, 1, '正在制定Q4规划'),
(101, 'YJL-团队周报汇总',   'MEDIUM', 'PENDING',     '2026-10-15 09:00:00', '2026-10-20 18:00:00', 102, 101, 0, 0, NULL),
(102, 'YJL-客户需求调研',   'HIGH',   'IN_PROGRESS', '2026-10-10 09:00:00', '2026-10-25 18:00:00', 103, 102, 1, 0, '调研进行中，已完成50份问卷'),
(103, 'YJL-运营数据分析',   'MEDIUM', 'PENDING',     '2026-10-20 09:00:00', '2026-10-28 18:00:00', 104, 102, 0, 0, NULL),
(104, 'YJL-晨会准备',       'MEDIUM', 'COMPLETED',   '2026-10-03 09:00:00', '2026-10-03 18:00:00', 103, 102, 0, 1, '已完成'),
(105, 'YJL-周报撰写',       'LOW',    'IN_PROGRESS', '2026-10-15 09:00:00', '2026-10-18 18:00:00', 103, 102, 0, 0, '撰写中'),
(106, 'YJL-竞品调研报告',   'HIGH',   'PENDING',     '2026-10-22 09:00:00', '2026-10-28 18:00:00', 103, 102, 1, 0, NULL),
(107, 'YJL-月度总结',       'MEDIUM', 'PENDING',     '2026-10-28 09:00:00', '2026-10-30 18:00:00', 103, 102, 0, 0, NULL);

-- yjl 测试日志
MERGE INTO `work_logs` (`id`, `user_id`, `work_item`, `completion_status`, `time_cost`, `log_date`) KEY(`id`) VALUES
(100, 101, 'YJL-部门预算审核',           'COMPLETED',   4.00, '2026-10-20'),
(101, 101, 'YJL-团队绩效评估',           'IN_PROGRESS', 2.50, '2026-10-21'),
(102, 102, 'YJL-周报模板设计',           'COMPLETED',   3.00, '2026-10-20'),
(103, 102, 'YJL-团队任务分配',           'COMPLETED',   2.00, '2026-10-21'),
(104, 103, 'YJL-客户问卷收集(50份)',     'COMPLETED',   6.00, '2026-10-20'),
(105, 103, 'YJL-竞品分析报告',           'IN_PROGRESS', 4.00, '2026-10-21'),
(106, 104, 'YJL-数据清洗脚本编写',       'COMPLETED',   5.00, '2026-10-20'),
(107, 104, 'YJL-运营指标看板搭建',       'IN_PROGRESS', 3.50, '2026-10-21');

-- Round28 REQ-28-03: AI 配置种子数据已删除（AI 配置迁移至客户端透传，服务端不再持有）

-- =============================================================================
-- 组织归属迁移 (从users表department_id/team_id迁移至user_org_affiliation)
-- =============================================================================
MERGE INTO `user_org_affiliation` (`user_id`, `department_id`, `team_id`) KEY(`user_id`) VALUES
(1, NULL, NULL),
(2, 1, 1),
(3, 1, 1),
(4, 1, 2),
(5, 1, 2),
(6, 2, 3),
(7, 2, 3),
(100, 3, 4),
(101, 3, 4),
(102, 3, 4),
(103, 3, 4),
(104, 3, 4);

-- =============================================================================
-- 显式层级关系初始化 (替代隐式推导, 支持跨层级递归)
-- 组织架构: ADMIN → FOUNDER → DEPT_HEAD → TEAM_LEADER → EMPLOYEE
-- =============================================================================
MERGE INTO `user_hierarchy` (`id`, `superior_id`, `subordinate_id`) KEY(`id`) VALUES
(1,  1, 2),
(2,  2, 3),
(3,  3, 4),
(4,  4, 5),
(5,  2, 7),
(6,  7, 6),
(7,  1, 100),
(8,  100, 101),
(9,  101, 102),
(10, 102, 103),
(11, 102, 104);

