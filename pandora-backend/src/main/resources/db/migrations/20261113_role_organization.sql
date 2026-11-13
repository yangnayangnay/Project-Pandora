-- Optional idempotent cleanup for an existing database (MySQL / H2).
-- Back up first. This does not change accounts, passwords or hierarchy links.
-- The application projects legacy affiliations by role even before applying this migration.
UPDATE users SET department_id = NULL, team_id = NULL WHERE role = 'FOUNDER'
  AND (department_id IS NOT NULL OR team_id IS NOT NULL);
UPDATE users SET team_id = NULL WHERE role = 'DEPT_HEAD' AND team_id IS NOT NULL;
UPDATE employee_records SET department_id = NULL, team_id = NULL WHERE role = 'FOUNDER'
  AND (department_id IS NOT NULL OR team_id IS NOT NULL);
UPDATE employee_records SET team_id = NULL WHERE role = 'DEPT_HEAD' AND team_id IS NOT NULL;
