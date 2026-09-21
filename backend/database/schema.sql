-- =============================================================
-- Campus Companion — Database Schema
-- ICT361 Mobile Application Development | Group Lab
-- MySQL 8.x / InnoDB
-- =============================================================
-- Rules enforced here:
--   * 9-digit student_number, uniqueness, leading zeros preserved
--   * Immutable student_id (independent of student_number)
--   * Max 15 active students per lab group
--   * One active student belongs to at most one group
--   * Soft deletion with deletion markers + number reservation
--   * Record versions for sync / conflict resolution
--   * Programme reference data (CS, IT, DS)
--   * The lecturer pre-creates the student profile; the student
--     then registers against it by student_number (one account
--     per profile, enforced by uq_student_account).
-- =============================================================

DROP DATABASE IF EXISTS campus_companion;
CREATE DATABASE campus_companion
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_general_ci;
USE campus_companion;

-- -------------------------------------------------------------
-- 1. programmes  (reference data: CS, IT, DS)
-- -------------------------------------------------------------
CREATE TABLE programmes (
  programme_code  VARCHAR(10)  NOT NULL,
  programme_name  VARCHAR(100) NOT NULL,
  is_active       TINYINT(1)   NOT NULL DEFAULT 1,
  PRIMARY KEY (programme_code)
) ENGINE=InnoDB;

-- -------------------------------------------------------------
-- 2. lab_groups  (G01–G04, hard cap 15 members)
-- -------------------------------------------------------------
CREATE TABLE lab_groups (
  group_id      INT          NOT NULL AUTO_INCREMENT,
  group_name    VARCHAR(10)  NOT NULL,                -- G01, G02, G03, G04
  max_members   TINYINT      NOT NULL DEFAULT 15,
  created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (group_id),
  UNIQUE KEY uq_group_name (group_name),
  CONSTRAINT chk_max_members CHECK (max_members BETWEEN 1 AND 15)
) ENGINE=InnoDB;

-- -------------------------------------------------------------
-- 3. lecturers  (created BEFORE students/accounts because both
--    reference it via foreign keys)
-- -------------------------------------------------------------
CREATE TABLE lecturers (
  lecturer_id    BIGINT       NOT NULL AUTO_INCREMENT,
  name           VARCHAR(100) NOT NULL,
  email          VARCHAR(120) NOT NULL,
  role           ENUM('lecturer','admin') NOT NULL DEFAULT 'lecturer',
  is_active      TINYINT(1)   NOT NULL DEFAULT 1,
  created_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (lecturer_id),
  UNIQUE KEY uq_lecturer_email (email)
) ENGINE=InnoDB;

-- -------------------------------------------------------------
-- 4. students  (profile — separate from auth)
-- -------------------------------------------------------------
CREATE TABLE students (
  student_id      BIGINT       NOT NULL AUTO_INCREMENT,   -- immutable
  student_number  CHAR(9)      NOT NULL,                  -- 9 digits, leading zeros kept
  name            VARCHAR(100) NOT NULL,
  programme_code  VARCHAR(10)  NOT NULL,
  lab_group_id    INT          NULL,                      -- NULL = Unassigned
  status          ENUM('active','unassigned','pending','deleted')
                               NOT NULL DEFAULT 'unassigned',
  is_deleted      TINYINT(1)   NOT NULL DEFAULT 0,
  deleted_at      TIMESTAMP    NULL,
  record_version  INT          NOT NULL DEFAULT 1,        -- optimistic locking / sync
  created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
                               ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (student_id),
  UNIQUE KEY uq_student_number (student_number),
  KEY idx_students_group (lab_group_id),
  KEY idx_students_programme (programme_code),
  KEY idx_students_name (name),
  CONSTRAINT fk_students_programme
    FOREIGN KEY (programme_code) REFERENCES programmes (programme_code),
  CONSTRAINT fk_students_group
    FOREIGN KEY (lab_group_id)  REFERENCES lab_groups (group_id)
    ON UPDATE CASCADE ON DELETE SET NULL,
  CONSTRAINT chk_student_number_digits
    CHECK (student_number REGEXP '^[0-9]{9}$')
) ENGINE=InnoDB;

-- -------------------------------------------------------------
-- 5. accounts  (authentication — students AND lecturers)
-- -------------------------------------------------------------
CREATE TABLE accounts (
  account_id     BIGINT       NOT NULL AUTO_INCREMENT,
  username       VARCHAR(50)  NOT NULL,             -- student_number or lecturer email
  password_hash  VARCHAR(255) NOT NULL,             -- bcrypt
  role           ENUM('student','lecturer') NOT NULL,
  student_id     BIGINT       NULL,                 -- set only for role='student'
  lecturer_id    BIGINT       NULL,                 -- set only for role='lecturer'
  is_locked      TINYINT(1)   NOT NULL DEFAULT 0,
  created_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
                              ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (account_id),
  UNIQUE KEY uq_username (username),
  UNIQUE KEY uq_student_account (student_id),
  UNIQUE KEY uq_lecturer_account (lecturer_id),
  CONSTRAINT fk_accounts_student
    FOREIGN KEY (student_id) REFERENCES students (student_id)
    ON DELETE CASCADE,
  CONSTRAINT fk_accounts_lecturer
    FOREIGN KEY (lecturer_id) REFERENCES lecturers (lecturer_id)
    ON DELETE CASCADE,
  CONSTRAINT chk_account_owner
    CHECK (
      (role = 'student'  AND student_id  IS NOT NULL AND lecturer_id IS NULL) OR
      (role = 'lecturer' AND lecturer_id IS NOT NULL AND student_id  IS NULL)
    )
) ENGINE=InnoDB;

-- -------------------------------------------------------------
-- 6. group_members  (membership history + current place)
--    A student has at most ONE active membership row.
-- -------------------------------------------------------------
CREATE TABLE group_members (
  id             BIGINT    NOT NULL AUTO_INCREMENT,
  student_id     BIGINT    NOT NULL,
  group_id       INT       NOT NULL,
  is_active      TINYINT(1) NOT NULL DEFAULT 1,   -- only one active per student
  joined_at      TIMESTAMP  NOT NULL DEFAULT CURRENT_TIMESTAMP,
  left_at        TIMESTAMP  NULL,
  PRIMARY KEY (id),
  KEY idx_gm_student_active (student_id, is_active),
  KEY idx_gm_group_active   (group_id, is_active),
  CONSTRAINT fk_gm_student
    FOREIGN KEY (student_id) REFERENCES students (student_id)
    ON DELETE CASCADE,
  CONSTRAINT fk_gm_group
    FOREIGN KEY (group_id)   REFERENCES lab_groups (group_id)
    ON DELETE CASCADE
) ENGINE=InnoDB;

-- -------------------------------------------------------------
-- 7. sync_operations  (offline queue — WorkManager + Retrofit)
--    Stored on the phone in Room; mirrored here for receipts so
--    a lost response can never cause a duplicate effect.
-- -------------------------------------------------------------
CREATE TABLE sync_operations (
  operation_id    CHAR(36)     NOT NULL,              -- client-generated UUID
  account_id      BIGINT       NOT NULL,
  student_id      BIGINT       NULL,
  op_type         ENUM('create','update','delete','assign_group','transfer_group')
                               NOT NULL,
  payload         JSON         NOT NULL,
  base_version    INT          NOT NULL DEFAULT 0,
  status          ENUM('pending','synced','failed','conflict')
                               NOT NULL DEFAULT 'pending',
  response_body   JSON         NULL,                  -- stored server result
  error_code      VARCHAR(40)  NULL,
  created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  processed_at    TIMESTAMP    NULL,
  PRIMARY KEY (operation_id),
  KEY idx_sync_account (account_id, status),
  CONSTRAINT fk_sync_account
    FOREIGN KEY (account_id) REFERENCES accounts (account_id)
    ON DELETE CASCADE,
  CONSTRAINT fk_sync_student
    FOREIGN KEY (student_id) REFERENCES students (student_id)
    ON DELETE SET NULL
) ENGINE=InnoDB;

-- -------------------------------------------------------------
-- 8. group_change_requests  (student asks lecturer)
-- -------------------------------------------------------------
CREATE TABLE group_change_requests (
  request_id     BIGINT      NOT NULL AUTO_INCREMENT,
  student_id     BIGINT      NOT NULL,
  current_group  INT         NULL,
  requested_group INT        NULL,
  reason         VARCHAR(255) NULL,
  status         ENUM('pending','approved','rejected')
                             NOT NULL DEFAULT 'pending',
  created_at     TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
  decided_at     TIMESTAMP   NULL,
  decided_by     BIGINT      NULL,   -- lecturer_id
  PRIMARY KEY (request_id),
  KEY idx_gcr_student (student_id, status),
  CONSTRAINT fk_gcr_student
    FOREIGN KEY (student_id) REFERENCES students (student_id)
    ON DELETE CASCADE,
  CONSTRAINT fk_gcr_lecturer
    FOREIGN KEY (decided_by) REFERENCES lecturers (lecturer_id)
    ON DELETE SET NULL
) ENGINE=InnoDB;

-- =============================================================
-- TRIGGERS — enforce the 15-member cap and single active group
-- =============================================================

DELIMITER //

-- Guard: one active membership per student
CREATE TRIGGER trg_gm_one_active_membership
BEFORE INSERT ON group_members
FOR EACH ROW
BEGIN
  IF NEW.is_active = 1 THEN
    IF EXISTS (
      SELECT 1 FROM group_members
      WHERE student_id = NEW.student_id AND is_active = 1
    ) THEN
      SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'STUDENT_ALREADY_IN_ACTIVE_GROUP';
    END IF;
  END IF;
END//

-- Guard: 15-member cap per group (also covered transactionally with FOR UPDATE)
CREATE TRIGGER trg_gm_group_capacity
BEFORE INSERT ON group_members
FOR EACH ROW
BEGIN
  DECLARE active_count INT;
  DECLARE cap INT;

  IF NEW.is_active = 1 THEN
    SELECT COUNT(*) INTO active_count
      FROM group_members
      WHERE group_id = NEW.group_id AND is_active = 1;

    SELECT max_members INTO cap
      FROM lab_groups WHERE group_id = NEW.group_id;

    IF active_count >= cap THEN
      SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'GROUP_FULL';
    END IF;
  END IF;
END//

DELIMITER ;

-- =============================================================
-- VIEW — handy for lecturer dashboards and group summary shares
-- =============================================================
CREATE OR REPLACE VIEW v_group_summary AS
SELECT
  g.group_id,
  g.group_name,
  g.max_members,
  COUNT(gm.id)                                   AS active_members,
  (g.max_members - COUNT(gm.id))                 AS places_left
FROM lab_groups g
LEFT JOIN group_members gm
  ON gm.group_id = g.group_id AND gm.is_active = 1
GROUP BY g.group_id, g.group_name, g.max_members;

-- =============================================================
-- End of schema.sql
-- =============================================================
