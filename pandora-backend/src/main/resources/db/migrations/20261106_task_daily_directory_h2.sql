-- Safe H2 in-place upgrade for the 2026-11-06 task, daily-item and directory release.
-- This script contains no seed users or business data replacement.

ALTER TABLE users ADD COLUMN IF NOT EXISTS signature VARCHAR(160) DEFAULT NULL;

ALTER TABLE daily_items ADD COLUMN IF NOT EXISTS start_date DATE DEFAULT NULL;
ALTER TABLE daily_items ADD COLUMN IF NOT EXISTS end_date DATE DEFAULT NULL;
UPDATE daily_items SET start_date = item_date WHERE start_date IS NULL;
UPDATE daily_items SET end_date = item_date WHERE end_date IS NULL;
ALTER TABLE daily_items ALTER COLUMN start_date SET NOT NULL;
ALTER TABLE daily_items ALTER COLUMN end_date SET NOT NULL;
CREATE INDEX IF NOT EXISTS idx_daily_items_owner_range
    ON daily_items(user_id, start_date, end_date, deleted);

CREATE TABLE IF NOT EXISTS task_assignees (
    task_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    assigned_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (task_id, user_id)
);
CREATE INDEX IF NOT EXISTS idx_task_assignees_user
    ON task_assignees(user_id, task_id);
INSERT INTO task_assignees(task_id, user_id)
SELECT t.id, t.assignee_id FROM tasks t
WHERE t.assignee_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM task_assignees ta
      WHERE ta.task_id = t.id AND ta.user_id = t.assignee_id
  );

UPDATE tasks SET status = 'IN_PROGRESS'
WHERE status IN ('PENDING', 'ACCEPTED', 'REJECTED');
UPDATE tasks SET status = 'COMPLETED' WHERE status = 'CONFIRMED';
DROP INDEX IF EXISTS idx_task_quadrant;
ALTER TABLE tasks DROP COLUMN IF EXISTS priority;
ALTER TABLE tasks DROP COLUMN IF EXISTS is_important;
