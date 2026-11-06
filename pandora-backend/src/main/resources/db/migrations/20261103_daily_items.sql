-- MySQL incremental migration for existing databases. Adds only the private-todo table.
-- Run against the application's database before using the new daily-items API.
CREATE TABLE IF NOT EXISTS `daily_items` (
    `id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT NOT NULL,
    `content` VARCHAR(500) NOT NULL,
    `item_date` DATE NOT NULL,
    `completed` TINYINT NOT NULL DEFAULT 0,
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `deleted` TINYINT NOT NULL DEFAULT 0,
    KEY `idx_daily_items_owner_date` (`user_id`, `item_date`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
