-- =============================================================
-- Campus Companion — Seed Data (fictitious, improved)
-- Run AFTER schema.sql
-- =============================================================
-- Creates:
--   * 2 lecturers + their accounts (one is admin)
--   * 18 students spread across CS, IT, DS
--     - 12 assigned to G01 (full-ish)
--     - 3 assigned to G02
--     - 1 unassigned (pending claim)
--     - 1 soft-deleted (number reserved)
--     - 1 active with a claim code already redeemed
--   * Student accounts (one per active profile)
--   * Group memberships with one group leader per group
--   * A pending group change request
--   * Notifications, reminders, timetable entries
--   * A password reset row (demo)
--
-- Passwords (after replacing placeholder hashes):
--   Lecturer : Lecturer@123
--   Admin    : Admin@123
--   Students : Student@123
-- =============================================================

USE campus_companion;

-- -------------------------------------------------------------
-- 1. Lecturers (one lecturer, one admin)
-- -------------------------------------------------------------
INSERT INTO lecturers (name, email, role) VALUES
  ('Dr. Mwansa Chanda', 'mwansa.chanda@mulungushi.ac.zm', 'lecturer'),
  ('Ms. Ruth Banda',    'ruth.banda@mulungushi.ac.zm',    'admin');

INSERT INTO accounts (username, password_hash, role, lecturer_id)
SELECT l.email,
       CASE
         WHEN l.role = 'admin'
           THEN '$2a$10$REPLACE_WITH_ADMIN_BCRYPT_HASH................'
         ELSE '$2a$10$REPLACE_WITH_LECTURER_BCRYPT_HASH.............'
       END,
       'lecturer',
       l.lecturer_id
FROM lecturers l;

-- -------------------------------------------------------------
-- 2. Students
--    Mix of statuses: active, unassigned, pending, deleted
--    claim_code set for several; one already redeemed.
-- -------------------------------------------------------------
INSERT INTO students
  (student_number, name, programme_code, email, phone,
   status, claim_code, claim_code_used, claimed_at, claimed_by,
   is_deleted, deleted_at, record_version)
VALUES
  -- G01 members (12) — active
  ('123456789', 'Memory Kaonga',  'CS', 'memory.kaonga@student.mu.ac.zm',  '0971000001', 'active',     'MEM-001', 0, NULL, NULL, 0, NULL, 1),
  ('987654321', 'John Banda',     'IT', 'john.banda@student.mu.ac.zm',     '0971000002', 'active',     'JOH-002', 0, NULL, NULL, 0, NULL, 1),
  ('111222333', 'Esther Phiri',   'DS', 'esther.phiri@student.mu.ac.zm',   '0971000003', 'active',     'EST-003', 0, NULL, NULL, 0, NULL, 1),
  ('444555666', 'Moses Mulenga',  'CS', 'moses.mulenga@student.mu.ac.zm',  '0971000004', 'active',     'MOS-004', 0, NULL, NULL, 0, NULL, 1),
  ('777888999', 'Tina Mwango',    'IT', 'tina.mwango@student.mu.ac.zm',    '0971000005', 'active',     'TIN-005', 0, NULL, NULL, 0, NULL, 1),
  ('222333444', 'Chanda Kabwe',   'DS', 'chanda.kabwe@student.mu.ac.zm',   '0971000006', 'active',     'CHA-006', 0, NULL, NULL, 0, NULL, 1),
  ('555666777', 'Grace Tembo',    'CS', 'grace.tembo@student.mu.ac.zm',    '0971000007', 'active',     'GRA-007', 0, NULL, NULL, 0, NULL, 1),
  ('888999111', 'Peter Zulu',     'IT', 'peter.zulu@student.mu.ac.zm',     '0971000008', 'active',     'PET-008', 0, NULL, NULL, 0, NULL, 1),
  ('333444555', 'Naomi Sinkala',  'DS', 'naomi.sinkala@student.mu.ac.zm',  '0971000009', 'active',     'NAO-009', 0, NULL, NULL, 0, NULL, 1),
  ('666777888', 'Brian Musonda',  'CS', 'brian.musonda@student.mu.ac.zm',  '0971000010', 'active',     'BRI-010', 0, NULL, NULL, 0, NULL, 1),
  ('999111222', 'Linda Chileshe', 'IT', 'linda.chileshe@student.mu.ac.zm', '0971000011', 'active',     'LIN-011', 0, NULL, NULL, 0, NULL, 1),
  ('101112131', 'Samuel Ngoma',   'DS', 'samuel.ngoma@student.mu.ac.zm',   '0971000012', 'active',     'SAM-012', 0, NULL, NULL, 0, NULL, 1),

  -- G02 members (3) — active
  ('121212121', 'Ruth Mwale',     'CS', 'ruth.mwale@student.mu.ac.zm',     '0971000013', 'active',     'RUT-013', 0, NULL, NULL, 0, NULL, 1),
  ('131313131', 'Kelvin Mumba',   'IT', 'kelvin.mumba@student.mu.ac.zm',   '0971000014', 'active',     'KEL-014', 0, NULL, NULL, 0, NULL, 1),
  ('141414141', 'Mercy Lungu',    'DS', 'mercy.lungu@student.mu.ac.zm',    '0971000015', 'active',     'MER-015', 0, NULL, NULL, 0, NULL, 1),

  -- Unassigned (waiting for lecturer to place them)
  ('151515151', 'Joseph Kapata',  'CS', 'joseph.kapata@student.mu.ac.zm',  '0971000016', 'unassigned', 'JOS-016', 0, NULL, NULL, 0, NULL, 1),

  -- Pending: has a claim code issued by lecturer, student hasn't registered yet
  ('161616161', 'Agnes Mbewe',    'IT', 'agnes.mbewe@student.mu.ac.zm',    '0971000017', 'pending',    'AGN-017', 0, NULL, NULL, 0, NULL, 1),

  -- Claim already redeemed by an account (claimed_by will be set after accounts insert)
  ('171717171', 'Daniel Chola',   'DS', 'daniel.chola@student.mu.ac.zm',   '0971000018', 'unassigned', 'DAN-018', 1, CURRENT_TIMESTAMP, NULL, 0, NULL, 2),

  -- Soft-deleted (number reserved, unique still enforced)
  ('181818181', 'Former Student', 'CS', 'former.student@student.mu.ac.zm', '0971000019', 'deleted',    'FOR-019', 0, NULL, NULL, 1, CURRENT_TIMESTAMP, 3);

-- -------------------------------------------------------------
-- 3. Student accounts
--    Only for active / unassigned / pending students who have registered.
--    Username = student_number. Deleted student gets no account.
--    Excluded here: 'pending' (no account yet) and 'deleted'.
-- -------------------------------------------------------------
INSERT INTO accounts (username, password_hash, role, student_id)
SELECT
  s.student_number,
  '$2a$10$REPLACE_WITH_STUDENT_BCRYPT_HASH..............',
  'student',
  s.student_id
FROM students s
WHERE s.status IN ('active','unassigned')
  AND s.is_deleted = 0;

-- -------------------------------------------------------------
-- 3b. Link claimed_by on the redeemed claim code
--     (Daniel Chola already redeemed his code)
-- -------------------------------------------------------------
UPDATE students s
JOIN accounts a ON a.student_id = s.student_id
SET s.claimed_by = a.account_id
WHERE s.student_number = '171717171';

-- -------------------------------------------------------------
-- 4. Assign students to groups via students.lab_group_id
-- -------------------------------------------------------------
UPDATE students
SET lab_group_id = (SELECT group_id FROM lab_groups WHERE group_name = 'G01')
WHERE student_number IN (
  '123456789','987654321','111222333','444555666','777888999','222333444',
  '555666777','888999111','333444555','666777888','999111222','101112131'
);

UPDATE students
SET lab_group_id = (SELECT group_id FROM lab_groups WHERE group_name = 'G02')
WHERE student_number IN ('121212121','131313131','141414141');

-- -------------------------------------------------------------
-- 5. Group memberships (active rows in group_members)
-- -------------------------------------------------------------
INSERT INTO group_members (student_id, group_id, is_active)
SELECT s.student_id, s.lab_group_id, 1
FROM students s
WHERE s.lab_group_id IS NOT NULL
  AND s.status = 'active'
  AND s.is_deleted = 0;

-- -------------------------------------------------------------
-- 6. Group leaders (one per group)
-- -------------------------------------------------------------
UPDATE students
SET is_group_leader = 1
WHERE student_number IN ('123456789',  -- G01 leader: Memory Kaonga
                         '121212121'); -- G02 leader: Ruth Mwale

-- -------------------------------------------------------------
-- 7. A pending group change request
--    Brian Musonda (G01) requests a transfer to G02
-- -------------------------------------------------------------
INSERT INTO group_change_requests
  (student_id, current_group, requested_group, reason, status)
SELECT
  s.student_id,
  (SELECT group_id FROM lab_groups WHERE group_name = 'G01'),
  (SELECT group_id FROM lab_groups WHERE group_name = 'G02'),
  'Schedule clash with another course on the same day.',
  'pending'
FROM students s
WHERE s.student_number = '666777888';

-- -------------------------------------------------------------
-- 8. Sample sync_operations rows (offline queue demo)
-- -------------------------------------------------------------
INSERT INTO sync_operations
  (operation_id, account_id, student_id, op_type, payload, base_version, status)
SELECT
  UUID(),
  a.account_id,
  s.student_id,
  'update',
  JSON_OBJECT('phone', '0971999999'),
  1,
  'pending'
FROM accounts a
JOIN students s ON s.student_id = a.student_id
WHERE s.student_number = '123456789';

-- -------------------------------------------------------------
-- 9. Notifications
-- -------------------------------------------------------------
INSERT INTO notifications (account_id, type, title, body, is_read)
SELECT a.account_id, 'group_assigned', 'Group assigned',
       'You have been assigned to lab group G01.', 0
FROM accounts a
JOIN students s ON s.student_id = a.student_id
WHERE s.student_number IN ('123456789','987654321','111222333');

INSERT INTO notifications (account_id, type, title, body, is_read)
SELECT a.account_id, 'change_request', 'New group change request',
       'Brian Musonda requested a transfer from G01 to G02.', 0
FROM accounts a
JOIN lecturers l ON l.lecturer_id = a.lecturer_id
WHERE l.email = 'mwansa.chanda@mulungushi.ac.zm';

-- -------------------------------------------------------------
-- 10. Reminders
-- -------------------------------------------------------------
INSERT INTO reminders
  (created_by, creator_role, scope, target_group_id, target_programme,
   title, body, remind_at)
SELECT
  a.account_id, 'lecturer', 'group',
  (SELECT group_id FROM lab_groups WHERE group_name = 'G01'),
  NULL,
  'Lab 3 submission',
  'Submit your Lab 3 report by Friday 17:00.',
  DATE_ADD(NOW(), INTERVAL 3 DAY)
FROM accounts a
JOIN lecturers l ON l.lecturer_id = a.lecturer_id
WHERE l.email = 'mwansa.chanda@mulungushi.ac.zm';

INSERT INTO reminders
  (created_by, creator_role, scope, target_programme, title, body, remind_at)
SELECT
  a.account_id, 'lecturer', 'programme', 'CS',
  'CS seminar',
  'Guest lecture in Room 12 at 14:00.',
  DATE_ADD(NOW(), INTERVAL 7 DAY)
FROM accounts a
JOIN lecturers l ON l.lecturer_id = a.lecturer_id
WHERE l.email = 'mwansa.chanda@mulungushi.ac.zm';

INSERT INTO reminders
  (created_by, creator_role, scope, title, body, remind_at)
SELECT
  a.account_id, 'group_leader', 'group',
  'Study session',
  'Let''s meet Saturday 10:00 in the library.',
  DATE_ADD(NOW(), INTERVAL 2 DAY)
FROM accounts a
JOIN students s ON s.student_id = a.student_id
WHERE s.student_number = '123456789';

-- -------------------------------------------------------------
-- 11. Timetable (lecturer-created)
--    day_of_week: 0=Sun .. 6=Sat
-- -------------------------------------------------------------
INSERT INTO timetable
  (course_code, title, group_id, day_of_week, start_time, end_time, venue, room, created_by)
SELECT 'ICT361', 'Mobile Application Development',
       (SELECT group_id FROM lab_groups WHERE group_name = 'G01'),
       1, '08:00:00', '10:00:00', 'Main Campus', 'Lab A',
       a.account_id
FROM accounts a
JOIN lecturers l ON l.lecturer_id = a.lecturer_id
WHERE l.email = 'mwansa.chanda@mulungushi.ac.zm';

INSERT INTO timetable
  (course_code, title, group_id, day_of_week, start_time, end_time, venue, room, created_by)
SELECT 'ICT361', 'Mobile Application Development',
       (SELECT group_id FROM lab_groups WHERE group_name = 'G02'),
       3, '10:00:00', '12:00:00', 'Main Campus', 'Lab B',
       a.account_id
FROM accounts a
JOIN lecturers l ON l.lecturer_id = a.lecturer_id
WHERE l.email = 'mwansa.chanda@mulungushi.ac.zm';

-- Applies to all groups (group_id NULL)
INSERT INTO timetable
  (course_code, title, group_id, day_of_week, start_time, end_time, venue, room, created_by)
SELECT 'ICT361', 'Guest Lecture',
       NULL, 5, '14:00:00', '16:00:00', 'Auditorium', NULL,
       a.account_id
FROM accounts a
JOIN lecturers l ON l.lecturer_id = a.lecturer_id
WHERE l.email = 'ruth.banda@mulungushi.ac.zm';

-- -------------------------------------------------------------
-- 12. Password reset (demo row, expired)
-- -------------------------------------------------------------
INSERT INTO password_resets (email, code_hash, expires_at, attempts, used)
VALUES (
  'memory.kaonga@student.mu.ac.zm',
  '$2a$10$REPLACE_WITH_RESET_CODE_BCRYPT_HASH.............',
  DATE_SUB(NOW(), INTERVAL 1 HOUR),
  0, 0
);

-- -------------------------------------------------------------
-- 13. Sanity checks
-- -------------------------------------------------------------
SELECT 'programmes'            AS table_name, COUNT(*) AS rows_added FROM programmes
UNION ALL SELECT 'lab_groups',            COUNT(*) FROM lab_groups
UNION ALL SELECT 'lecturers',             COUNT(*) FROM lecturers
UNION ALL SELECT 'students',              COUNT(*) FROM students
UNION ALL SELECT 'accounts',              COUNT(*) FROM accounts
UNION ALL SELECT 'group_members',         COUNT(*) FROM group_members
UNION ALL SELECT 'notifications',         COUNT(*) FROM notifications
UNION ALL SELECT 'reminders',             COUNT(*) FROM reminders
UNION ALL SELECT 'timetable',             COUNT(*) FROM timetable
UNION ALL SELECT 'group_change_requests', COUNT(*) FROM group_change_requests
UNION ALL SELECT 'sync_operations',       COUNT(*) FROM sync_operations
UNION ALL SELECT 'password_resets',       COUNT(*) FROM password_resets;

SELECT group_name, active_members, places_left
FROM v_group_summary
ORDER BY group_name;

SELECT s.student_number, s.name, s.status, s.is_group_leader,
       s.claim_code, s.claim_code_used, s.record_version,
       g.group_name
FROM students s
LEFT JOIN lab_groups g ON g.group_id = s.lab_group_id
ORDER BY s.student_id;