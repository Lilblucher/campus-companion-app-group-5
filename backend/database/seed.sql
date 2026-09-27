-- =============================================================
-- Campus Companion — Seed Data (fictitious)
-- Run AFTER schema.sql
-- =============================================================
-- Creates:
--   * 1 lecturer + lecturer account
--   * 12 sample students (self-registered, unassigned)
--   * Student accounts linked to each profile
--   * A demo group membership set for testing capacity
--
-- Programmes (CS, IT, DS) and lab groups (G01–G04) are already
-- inserted by schema.sql, so they are NOT repeated here.
-- =============================================================

USE campus_companion;

-- -------------------------------------------------------------
-- 1. Lecturer
--    Login username : mwansa.chanda@mulungushi.ac.zm
--    Password       : Lecturer@123
--    (hash below is a placeholder — regenerate with bcrypt)
-- -------------------------------------------------------------
INSERT INTO lecturers (name, email, role) VALUES
  ('Dr. Mwansa Chanda', 'mwansa.chanda@mulungushi.ac.zm', 'lecturer');

INSERT INTO accounts (username, password_hash, role, lecturer_id)
VALUES (
  'mwansa.chanda@mulungushi.ac.zm',
  '$2b$10$d.qEqdpJMwA1bX8Q0sc6J.lVxXVqXHz97XdNdPotpIRxtv.sKFDEm',
  'lecturer',
  (SELECT lecturer_id FROM lecturers
    WHERE email = 'mwansa.chanda@mulungushi.ac.zm')
);

-- -------------------------------------------------------------
-- -- 2. Sample students (fictitious)
--    All 9-digit numbers, mixed programmes.
--    Created as Unassigned here; Step 4 assigns a demo set to G01.
-- -------------------------------------------------------------

INSERT INTO students
  (student_number, name, programme_code, lab_group_id, status, claim_code)
VALUES
  ('123456789', 'Memory Kaonga',   'CS', NULL, 'unassigned', 'MEM-001'),
  ('987654321', 'John Banda',      'IT', NULL, 'unassigned', 'JOH-002'),
  ('111222333', 'Esther Phiri',    'DS', NULL, 'unassigned', 'EST-003'),
  ('444555666', 'Moses Mulenga',   'CS', NULL, 'unassigned', 'MOS-004'),
  ('777888999', 'Tina Mwango',     'IT', NULL, 'unassigned', 'TIN-005'),
  ('222333444', 'Chanda Kabwe',    'DS', NULL, 'unassigned', 'CHA-006'),
  ('555666777', 'Grace Tembo',     'CS', NULL, 'unassigned', 'GRA-007'),
  ('888999111', 'Peter Zulu',      'IT', NULL, 'unassigned', 'PET-008'),
  ('333444555', 'Naomi Sinkala',   'DS', NULL, 'unassigned', 'NAO-009'),
  ('666777888', 'Brian Musonda',   'CS', NULL, 'unassigned', 'BRI-010'),
  ('999111222', 'Linda Chileshe',  'IT', NULL, 'unassigned', 'LIN-011'),
  ('101112131', 'Samuel Ngoma',    'DS', NULL, 'unassigned', 'SAM-012');
  
  -------------
-- 3. Student accounts (one per profile — uq_student_account
--    enforces this, so insert exactly one row per student)
--    Username = student_number
-- -------------------------------------------------------------
INSERT INTO accounts (username, password_hash, role, student_id)
SELECT
  s.student_number,
  '$2b$10$PKJPsRvPttbDv9SubJ98SOaNnOwSGYESjPjQBwtLvoC3q/RYO8Coq',
  'student',
  s.student_id
FROM students s
WHERE s.student_number IN (
  '123456789', '987654321', '111222333', '444555666',
  '777888999', '222333444', '555666777', '888999111',
  '333444555', '666777888', '999111222', '101112131'
);

-- -------------------------------------------------------------
---- 4. Demo group memberships
--    Fill G01 to 12/15 with the sample students above.
--    To prep Challenge 1 (two phones race for the last place),
--    add two more students and a matching UPDATE + INSERT below,
--    or run two concurrent assign requests against the API.
-- -------------------------------------------------------------
UPDATE students
SET lab_group_id = (SELECT group_id FROM lab_groups WHERE group_name = 'G01'),
    status = 'active'
WHERE student_number IN (
  '123456789', '987654321', '111222333', '444555666',
  '777888999', '222333444', '555666777', '888999111',
  '333444555', '666777888', '999111222', '101112131'
);

INSERT INTO group_members (student_id, group_id, is_active)
SELECT s.student_id, g.group_id, 1
FROM students s
JOIN lab_groups g ON g.group_name = 'G01'
WHERE s.student_number IN (
  '123456789', '987654321', '111222333', '444555666',
  '777888999', '222333444', '555666777', '888999111',
  '333444555', '666777888', '999111222', '101112131'
);

-- -------------------------------------------------------------
-- 5. Sanity check
-- -------------------------------------------------------------
SELECT 'programmes'   AS table_name, COUNT(*) AS rows_added FROM programmes
UNION ALL
SELECT 'lab_groups',   COUNT(*) FROM lab_groups
UNION ALL
SELECT 'lecturers',    COUNT(*) FROM lecturers
UNION ALL
SELECT 'students',     COUNT(*) FROM students
UNION ALL
SELECT 'accounts',     COUNT(*) FROM accounts
UNION ALL
SELECT 'group_members',COUNT(*) FROM group_members;

SELECT group_name, active_members, places_left
FROM v_group_summary
ORDER BY group_name;