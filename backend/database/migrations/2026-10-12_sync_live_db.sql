-- =============================================================
-- Migration: bring an older live DB up to the current schema.sql
-- Safe to run more than once (idempotent).
--
-- Fixes:
--   * login crash: "Unknown column 's.is_group_leader' in 'field list'"
--   * missing reminders / timetable / group_change_requests tables
--   * missing password_resets table (forgot-password flow)
--
-- Run on the backend box:
--   mysql -u root -p campus_companion < database/migrations/2026-10-12_sync_live_db.sql
-- =============================================================

-- 1. students.is_group_leader  (MySQL has no "ADD COLUMN IF NOT EXISTS",
--    so guard it via information_schema + a prepared statement).
SET @has_col := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME   = 'students'
    AND COLUMN_NAME  = 'is_group_leader'
);
SET @ddl := IF(@has_col = 0,
  'ALTER TABLE students ADD COLUMN is_group_leader TINYINT(1) NOT NULL DEFAULT 0',
  'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 2. group_change_requests
CREATE TABLE IF NOT EXISTS group_change_requests (
  request_id      BIGINT       NOT NULL AUTO_INCREMENT,
  student_id      BIGINT       NOT NULL,
  current_group   INT          NULL,
  requested_group INT          NULL,
  reason          VARCHAR(255) NULL,
  status          ENUM('pending','approved','rejected') NOT NULL DEFAULT 'pending',
  created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  decided_at      TIMESTAMP    NULL,
  decided_by      BIGINT       NULL,
  PRIMARY KEY (request_id),
  KEY idx_gcr_student (student_id, status),
  CONSTRAINT fk_gcr_student  FOREIGN KEY (student_id) REFERENCES students (student_id) ON DELETE CASCADE,
  CONSTRAINT fk_gcr_lecturer FOREIGN KEY (decided_by) REFERENCES lecturers (lecturer_id) ON DELETE SET NULL
) ENGINE=InnoDB;

-- 3. password_resets (forgot-password)
CREATE TABLE IF NOT EXISTS password_resets (
  reset_id     INT AUTO_INCREMENT PRIMARY KEY,
  email        VARCHAR(100) NOT NULL,
  code_hash    VARCHAR(255) NOT NULL,
  expires_at   DATETIME     NOT NULL,
  attempts     TINYINT      NOT NULL DEFAULT 0,
  used         TINYINT(1)   NOT NULL DEFAULT 0,
  created_at   TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_email (email)
);

-- 4. notifications (in-app notifications)
CREATE TABLE IF NOT EXISTS notifications (
  notification_id  BIGINT       NOT NULL AUTO_INCREMENT,
  account_id       BIGINT       NOT NULL,
  type             VARCHAR(50)  NOT NULL,
  title            VARCHAR(100) NOT NULL,
  body             VARCHAR(255) NOT NULL,
  is_read          TINYINT(1)   NOT NULL DEFAULT 0,
  created_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (notification_id),
  KEY idx_notif_account (account_id, is_read),
  CONSTRAINT fk_notif_account FOREIGN KEY (account_id) REFERENCES accounts (account_id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- 5. reminders
CREATE TABLE IF NOT EXISTS reminders (
  reminder_id      BIGINT       NOT NULL AUTO_INCREMENT,
  created_by       BIGINT       NOT NULL,
  creator_role     VARCHAR(20)  NOT NULL,
  scope            ENUM('personal','group','programme','all') NOT NULL DEFAULT 'personal',
  target_group_id  INT          NULL,
  target_programme VARCHAR(10)  NULL,
  title            VARCHAR(120) NOT NULL,
  body             VARCHAR(500) NOT NULL DEFAULT '',
  remind_at        DATETIME     NULL,
  created_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (reminder_id),
  KEY idx_rem_scope (scope, target_group_id),
  KEY idx_rem_creator (created_by),
  CONSTRAINT fk_rem_account FOREIGN KEY (created_by) REFERENCES accounts (account_id) ON DELETE CASCADE,
  CONSTRAINT fk_rem_group   FOREIGN KEY (target_group_id) REFERENCES lab_groups (group_id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- 6. timetable
CREATE TABLE IF NOT EXISTS timetable (
  session_id   BIGINT       NOT NULL AUTO_INCREMENT,
  course_code  VARCHAR(20)  NOT NULL,
  title        VARCHAR(120) NOT NULL,
  group_id     INT          NULL,
  day_of_week  TINYINT      NOT NULL,
  start_time   TIME         NOT NULL,
  end_time     TIME         NOT NULL,
  venue        VARCHAR(120) NULL,
  room         VARCHAR(60)  NULL,
  created_by   BIGINT       NULL,
  created_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (session_id),
  KEY idx_tt_group_day (group_id, day_of_week),
  CONSTRAINT fk_tt_group   FOREIGN KEY (group_id) REFERENCES lab_groups (group_id) ON DELETE CASCADE,
  CONSTRAINT fk_tt_account FOREIGN KEY (created_by) REFERENCES accounts (account_id) ON DELETE SET NULL
) ENGINE=InnoDB;
