// controllers/groupsController.js
// Lecturer-only actions on the group list.
const db = require('../config/db');
const { setStudentGroup, isGroupFull } = require('../utils/membership');
const { notifyStudent } = require('../utils/notify');

const SN = /^\d{9}$/;

function fail(http, error, extra = {}) {
  return Object.assign(new Error(error), { http, error, extra });
}

// POST /api/groups/:groupId/members   { student_numbers: ["202400442", ...] }
// (a single "student_number" string is also accepted). All-or-nothing.
async function addMembers(req, res) {
  const groupId = Number(req.params.groupId);
  const b = req.body || {};
  const list = Array.isArray(b.student_numbers) ? b.student_numbers : (b.student_number ? [b.student_number] : []);
  // Numbers must be strings: a JSON number would drop leading zeros.
  const numbers = [...new Set(list.map((n) => (typeof n === 'string' ? n.trim() : n)))];
    console.log('addMembers', req.params, req.body);

  if (!Number.isInteger(groupId) || groupId < 1 || numbers.length === 0 || numbers.length > 15 ||
      !numbers.every((n) => typeof n === 'string' && SN.test(n))) {
    return res.status(400).json({
      error: 'INVALID_INPUT',
      message: 'Provide 1-15 student numbers, each a string of exactly 9 digits',
    });
  }

  try {
    const result = await db.withTransaction(async (conn) => {
      // Lock the group row so simultaneous requests queue up behind each other
      const [g] = await conn.query(
        'SELECT group_id, group_name, max_members FROM lab_groups WHERE group_id = ? FOR UPDATE', [groupId]);
      if (!g.length) throw fail(404, 'GROUP_NOT_FOUND');
      const group = g[0];

      const [[row]] = await conn.query(
        'SELECT COUNT(*) AS active FROM group_members WHERE group_id = ? AND is_active = 1', [groupId]);
      const active = Number(row.active);
      const seatsLeft = group.max_members - active;
      if (numbers.length > seatsLeft) {
        throw fail(409, 'GROUP_FULL', { seats_left: seatsLeft, requested: numbers.length });
      }

      const [students] = await conn.query(
        `SELECT student_id, student_number, name, lab_group_id
           FROM students
          WHERE student_number IN (?) AND is_deleted = 0
          ORDER BY student_id FOR UPDATE`, [numbers]);

      const have = new Set(students.map((s) => s.student_number));
      const missing = numbers.filter((n) => !have.has(n));
      if (missing.length) throw fail(404, 'STUDENT_NOT_FOUND', { student_numbers: missing });

      const taken = students.filter((s) => s.lab_group_id !== null);
      if (taken.length) {
        throw fail(409, 'STUDENT_ALREADY_IN_GROUP', { student_numbers: taken.map((s) => s.student_number) });
      }

      for (const s of students) {
        await setStudentGroup(conn, s.student_id, groupId);
        await notifyStudent(conn, s.student_id, 'GROUP_ASSIGNED', 'Added to group',
          `You have been added to ${group.group_name}.`);
      }

      return {
        group_id: groupId,
        group_name: group.group_name,
        added: students.map((s) => ({ student_id: s.student_id, student_number: s.student_number, name: s.name })),
        active_members: active + students.length,
        max_members: group.max_members,
        places_left: seatsLeft - students.length,
      };
    });
    return res.status(201).json(result);
  } catch (err) {
    if (err.http) return res.status(err.http).json({ error: err.error, ...err.extra });
    if (isGroupFull(err)) return res.status(409).json({ error: 'GROUP_FULL' });
    console.error('addMembers error:', err);
    return res.status(500).json({ error: 'Failed to add members' });
  }
}

// GET /api/groups/assignable-students?q=&programme=
// Picker data: registered, not deleted, no active group.
async function getAssignableStudents(req, res) {
  const q = `%${String(req.query.q || '').trim()}%`;
  const programme = req.query.programme || null;
  try {
    const [rows] = await db.query(
      `SELECT s.student_id, s.student_number, s.name, s.programme_code, s.status
         FROM students s
        WHERE s.is_deleted = 0
          AND s.lab_group_id IS NULL
          AND NOT EXISTS (SELECT 1 FROM group_members gm WHERE gm.student_id = s.student_id AND gm.is_active = 1)
          AND (s.name LIKE ? OR s.student_number LIKE ?)
          AND (? IS NULL OR s.programme_code = ?)
        ORDER BY s.name
        LIMIT 100`,
      [q, q, programme, programme]);
    return res.json({ students: rows });
  } catch (err) {
    console.error('getAssignableStudents error:', err);
    return res.status(500).json({ error: 'Failed to load students' });
  }
}


module.exports = { addMembers, getAssignableStudents };