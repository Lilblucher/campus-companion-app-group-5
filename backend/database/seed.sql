-- =============================================================
-- Campus Companion — Seed Data (fictitious)
-- Run AFTER schema.sql
-- =============================================================
-- Creates:
--   * 1 lecturer + lecturer account
--   * 12 sample students with email, phone, and claim codes
--   * Student accounts linked to each profile
--   * Demo group membership: all 12 assigned to G01 (12/15)
--
-- Programmes (CS, IT, DS) and lab groups (G01–G04) are already
-- inserted by schema.sql, so they are NOT repeated here.
--
-- Passwords (after replacing the placeholder hashes):
--   Lecturer : Lecturer@123
--   Students : Student@123
-- =============================================================

USE campus_companion;

-- -------------------------------------------------------------
-- 1. Lecturer
--    Login username : mwansa.chanda@mulungushi.ac.zm
-- -------------------------------------------------------------
INSERT INTO lecturers (name, email, role) VALUES
  ('Dr. Mwansa Chanda', 'mwansa.chanda@mulungushi.ac.zm', 'lecturer');

INSERT INTO accounts (username, password_hash, role, lecturer_id)
VALUES (
  'mwansa.chanda@mulungushi.ac.zm',
  '$2a$10$e8u8Y1XAORZzG0EnM8tusuNAvCLt0tX7tmxNDy8s94wS9U0ndKYQC',
  'lecturer',
  (SELECT lecturer_id FROM lecturers
    WHERE email = 'mwansa.chanda@mulungushi.ac.zm')
);

-- -------------------------------------------------------------
-- 2. Sample students (fictitious)
--    All 9-digit numbers, mixed programmes.
--    Created as 'unassigned' here; Step 4 assigns them to G01.
-- -------------------------------------------------------------
INSERT INTO students
  (student_number, name, programme_code, email, phone, status, claim_code)
VALUES
  ('123456789', 'Memory Kaonga',  'CS', 'memory.kaonga@student.mu.ac.zm',  '0971000001', 'unassigned', 'MEM-001'),
  ('987654321', 'John Banda',     'IT', 'john.banda@student.mu.ac.zm',     '0971000002', 'unassigned', 'JOH-002'),
  ('111222333', 'Esther Phiri',   'DS', 'esther.phiri@student.mu.ac.zm',   '0971000003', 'unassigned', 'EST-003'),
  ('444555666', 'Moses Mulenga',  'CS', 'moses.mulenga@student.mu.ac.zm',  '0971000004', 'unassigned', 'MOS-004'),
  ('777888999', 'Tina Mwango',    'IT', 'tina.mwango@student.mu.ac.zm',    '0971000005', 'unassigned', 'TIN-005'),
  ('222333444', 'Chanda Kabwe',   'DS', 'chanda.kabwe@student.mu.ac.zm',   '0971000006', 'unassigned', 'CHA-006'),
  ('555666777', 'Grace Tembo',    'CS', 'grace.tembo@student.mu.ac.zm',    '0971000007', 'unassigned', 'GRA-007'),
  ('888999111', 'Peter Zulu',     'IT', 'peter.zulu@student.mu.ac.zm',     '0971000008', 'unassigned', 'PET-008'),
  ('333444555', 'Naomi Sinkala',  'DS', 'naomi.sinkala@student.mu.ac.zm',  '0971000009', 'unassigned', 'NAO-009'),
  ('666777888', 'Brian Musonda',  'CS', 'brian.musonda@student.mu.ac.zm',  '0971000010', 'unassigned', 'BRI-010'),
  ('999111222', 'Linda Chileshe', 'IT', 'linda.chileshe@student.mu.ac.zm', '0971000011', 'unassigned', 'LIN-011'),
  ('101112131', 'Samuel Ngoma',   'DS', 'samuel.ngoma@student.mu.ac.zm',   '0971000012', 'unassigned', 'SAM-012');

-- -------------------------------------------------------------
-- 3. Student accounts (one per profile — uq_student_account
--    enforces this, so insert exactly one row per student)
--    Username = student_number
-- -------------------------------------------------------------
INSERT INTO accounts (username, password_hash, role, student_id)
SELECT
  s.student_number,
  '$2a$10$/fpDXKQlHdAlHOFA42Mque82zeN0YGy1tlb0t5ulrUVMUBzeh9axe',
  'student',
  s.student_id
FROM students s
WHERE s.student_number IN (
  '123456789', '987654321', '111222333', '444555666',
  '777888999', '222333444', '555666777', '888999111',
  '333444555', '666777888', '999111222', '101112131'
);

-- -------------------------------------------------------------
-- 4. Demo group memberships
--    Fill G01 to 12/15 with the sample students above.
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
SELECT 'programmes'    AS table_name, COUNT(*) AS rows_added FROM programmes
UNION ALL
SELECT 'lab_groups',    COUNT(*) FROM lab_groups
UNION ALL
SELECT 'lecturers',     COUNT(*) FROM lecturers
UNION ALL
SELECT 'students',      COUNT(*) FROM students
UNION ALL
SELECT 'accounts',      COUNT(*) FROM accounts
UNION ALL
SELECT 'group_members', COUNT(*) FROM group_members;

SELECT group_name, active_members, places_left
FROM v_group_summary
ORDER BY group_name;