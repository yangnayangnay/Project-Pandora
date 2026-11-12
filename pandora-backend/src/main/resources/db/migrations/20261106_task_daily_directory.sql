-- Project Pandora 2026-11-06 business model upgrade.
-- Apply once to existing MySQL deployments before starting the upgraded service.

ALTER TABLE users ADD COLUMN signature VARCHAR(160) NULL COMMENT '个人签名';

ALTER TABLE daily_items ADD COLUMN start_date DATE NULL;
ALTER TABLE daily_items ADD COLUMN end_date DATE NULL;
UPDATE daily_items SET start_date = item_date, end_date = item_date
 WHERE start_date IS NULL OR end_date IS NULL;
ALTER TABLE daily_items MODIFY start_date DATE NOT NULL;
ALTER TABLE daily_items MODIFY end_date DATE NOT NULL;
CREATE INDEX idx_daily_items_owner_range
    ON daily_items(user_id, start_date, end_date, deleted);

CREATE TABLE task_assignees (
    task_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    assigned_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (task_id, user_id),
    KEY idx_task_assignees_user (user_id, task_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT IGNORE INTO task_assignees(task_id, user_id)
SELECT id, assignee_id FROM tasks WHERE assignee_id IS NOT NULL;

UPDATE tasks SET status = 'IN_PROGRESS'
 WHERE status IN ('PENDING', 'ACCEPTED', 'REJECTED');
UPDATE tasks SET status = 'COMPLETED' WHERE status = 'CONFIRMED';
ALTER TABLE tasks MODIFY status ENUM('IN_PROGRESS','COMPLETED')
    NOT NULL DEFAULT 'IN_PROGRESS' COMMENT '任务状态';
ALTER TABLE tasks DROP INDEX idx_quadrant;
ALTER TABLE tasks DROP COLUMN priority;
ALTER TABLE tasks DROP COLUMN is_important;
