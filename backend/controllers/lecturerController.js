// controllers/lecturerController.js — Phase 2 lecturer panel (full power).
// All routes are gated by middleware/auth + middleware/roles.requireLecturer,
// so req.actor.level === 'lecturer' and req.actor.lecturerId is set.

const db = require('../config/db');
const { setStudentGroup, isGroupFull } = require('../utils/membership');
const { notifyStudent } = require('../utils/notify');
const { buildMembersWorkbook } = require('../utils/xlsx');

const PROGRAMMES = ['CS', 'IT', 'DS'];
const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
const PHONE_RE = /^\+?\d{7,15}$/;

function genClaimCode() {
  const s = 'ABCDEFGHJKMNPQRSTUVWXYZ23456789';
  let out = '';
  for (let i = 0; i < 6; i++) out += s[Math.floor(Math.random() * s.length)];
  return 'CR-' + out;
}

// GET /api/lecturer/students?q=&programme=&group=&unassigned=&limit=&offset=
async function listStudents(req, res) {
  try {
    const where = ['s.is_deleted = 0'];
    const params = [];
    const q = typeof req.query.q === 'string' ? req.query.q.trim() : '';
    if (q) { where.push('(s.name LIKE ? OR s.student_number LIKE ?)'); params.push(`%${q}%`, `%${q}%`); }
    if (PROGRAMMES.includes(req.query.programme)) { where.push('s.programme_code = ?'); params.push(req.query.programme); }
    if (String(req.query.unassigned).toLowerCase() === 'true') {
      where.push('s.lab_group_id IS NULL');
    } else if (req.query.group) {
      // accept a group id or a group name (e.g. "G01")
      if (/^\d+$/.test(String(req.query.group))) { where.push('s.lab_group_id = ?'); params.push(Number(req.query.group)); }
      else { where.push('g.group_name = ?'); params.push(String(req.query.group)); }
    }
    const limit = Math.min(Math.max(parseInt(req.query.limit, 10) || 50, 1), 200);
    const offset = Math.max(parseInt(req.query.offset, 10) || 0, 0);

    const [rows] = await db.query(
      `SELECT s.student_id, s.student_number, s.name, s.programme_code, s.email, s.phone,
              g.group_name AS lab_group, s.status, s.is_group_leader
         FROM students s
         LEFT JOIN lab_groups g ON g.group_id = s.lab_group_id
        WHERE ${where.join(' AND ')}
        ORDER BY s.name
        LIMIT ${limit} OFFSET ${offset}`,
      params
    );
    const [[{ total }]] = await db.query(
      `SELECT COUNT(*) AS total FROM students s
         LEFT JOIN lab_groups g ON g.group_id = s.lab_group_id
        WHERE ${where.join(' AND ')}`,
      params
    );
    return res.json({ students: rows, total: Number(total), limit, offset });
  } catch (err) {
    console.error('listStudents error:', err);
    return res.status(500).json({ error: 'Failed to load students' });
  }
}

// POST /api/lecturer/students  { name, student_number, programme_code, email?, phone? }
// Creates a profile row (no account) + a claim code the student uses to register.
async function createStudent(req, res) {
  const b = req.body || {};
  const name = typeof b.name === 'string' ? b.name.trim() : '';
  if (name.length < 2 || name.length > 100) return res.status(400).json({ error: 'name must be 2-100 characters' });
  const sn = typeof b.student_number === 'string' ? b.student_number.trim() : '';
  if (!/^\d{9}$/.test(sn)) return res.status(400).json({ error: 'student_number must be exactly 9 digits' });
  if (!PROGRAMMES.includes(b.programme_code)) return res.status(400).json({ error: 'programme_code must be CS, IT or DS' });
  const email = (typeof b.email === 'string' && b.email.trim()) ? b.email.trim().toLowerCase() : `${sn}@student.mu.ac.zm`;
  if (!EMAIL_RE.test(email)) return res.status(400).json({ error: 'email must be a valid email address' });
  const phone = (typeof b.phone === 'string' && b.phone.trim()) ? b.phone.trim() : '0000000000';
  if (!PHONE_RE.test(phone)) return res.status(400).json({ error: 'phone must be 7-15 digits' });
  const claimCode = genClaimCode();

  try {
    const [result] = await db.query(
      `INSERT INTO students (student_number, name, programme_code, email, phone, status, claim_code, claim_code_used)
       VALUES (?, ?, ?, ?, ?, 'unassigned', ?, 0)`,
      [sn, name, b.programme_code, email, phone, claimCode]
    );
    return res.status(201).json({
      student_id: result.insertId, student_number: sn, name,
      programme_code: b.programme_code, email, phone, claim_code: claimCode,
    });
  } catch (err) {
    if (err.code === 'ER_DUP_ENTRY') {
      const msg = err.sqlMessage || '';
      if (msg.includes('email')) return res.status(409).json({ error: 'email already registered' });
      return res.status(409).json({ error: 'student_number already registered' });
    }
    console.error('createStudent error:', err);
    return res.status(500).json({ error: 'Failed to create student' });
  }
}

// PATCH /api/lecturer/students/:id  { name?, programme_code?, student_number? }
async function editStudent(req, res) {
  const id = parseInt(req.params.id, 10);
  if (!Number.isInteger(id) || id < 1) return res.status(400).json({ error: 'invalid student id' });
  const b = req.body || {};
  const sets = [], params = [];
  if (b.name !== undefined) {
    const name = typeof b.name === 'string' ? b.name.trim() : '';
    if (name.length < 2 || name.length > 100) return res.status(400).json({ error: 'name must be 2-100 characters' });
    sets.push('name = ?'); params.push(name);
  }
  if (b.programme_code !== undefined) {
    if (!PROGRAMMES.includes(b.programme_code)) return res.status(400).json({ error: 'programme_code must be CS, IT or DS' });
    sets.push('programme_code = ?'); params.push(b.programme_code);
  }
  if (b.student_number !== undefined) {
    const sn = typeof b.student_number === 'string' ? b.student_number.trim() : '';
    if (!/^\d{9}$/.test(sn)) return res.status(400).json({ error: 'student_number must be exactly 9 digits' });
    sets.push('student_number = ?'); params.push(sn);
  }
  if (sets.length === 0) return res.status(400).json({ error: 'Provide at least one of: name, programme_code, student_number' });

  try {
    const [result] = await db.query(
      `UPDATE students SET ${sets.join(', ')}, record_version = record_version + 1
        WHERE student_id = ? AND is_deleted = 0`,
      [...params, id]
    );
    if (result.affectedRows === 0) return res.status(404).json({ error: 'Student not found' });
    // keep the login username (accounts.username == student_number) in sync on a correction
    if (b.student_number !== undefined) {
      await db.query('UPDATE accounts SET username = ? WHERE student_id = ?', [b.student_number.trim(), id]).catch(() => {});
    }
    return res.json({ student_id: id, updated: true });
  } catch (err) {
    if (err.code === 'ER_DUP_ENTRY') return res.status(409).json({ error: 'student_number already in use' });
    console.error('editStudent error:', err);
    return res.status(500).json({ error: 'Failed to update student' });
  }
}

// DELETE /api/lecturer/students/:id  — soft delete (release place, lock account, reserve number).
async function softDeleteStudent(req, res) {
  const id = parseInt(req.params.id, 10);
  if (!Number.isInteger(id) || id < 1) return res.status(400).json({ error: 'invalid student id' });
  const conn = await db.getConnection();
  try {
    await conn.beginTransaction();
    const [rows] = await conn.query('SELECT student_id FROM students WHERE student_id = ? AND is_deleted = 0 FOR UPDATE', [id]);
    if (rows.length === 0) { await conn.rollback(); return res.status(404).json({ error: 'Student not found' }); }

    await conn.query('UPDATE group_members SET is_active = 0, left_at = NOW() WHERE student_id = ? AND is_active = 1', [id]);
    await conn.query(
      "UPDATE students SET is_deleted = 1, deleted_at = NOW(), lab_group_id = NULL, status = 'deleted' WHERE student_id = ?",
      [id]
    );
    await conn.query('UPDATE accounts SET is_locked = 1 WHERE student_id = ?', [id]);
    await conn.commit();
    return res.json({ student_id: id, deleted: true });
  } catch (err) {
    try { await conn.rollback(); } catch (_) {}
    console.error('softDeleteStudent error:', err);
    return res.status(500).json({ error: 'Failed to delete student' });
  } finally {
    conn.release();
  }
}

// POST /api/lecturer/students/:id/assign  { group_id: number|null }
async function assignStudent(req, res) {
  const id = parseInt(req.params.id, 10);
  if (!Number.isInteger(id) || id < 1) return res.status(400).json({ error: 'invalid student id' });
  let groupId = req.body ? req.body.group_id : undefined;
  if (groupId === null) { /* unassign */ }
  else if (typeof groupId === 'number' && Number.isInteger(groupId) && groupId >= 1) { /* ok */ }
  else if (typeof groupId === 'string' && /^\d+$/.test(groupId.trim())) groupId = Number(groupId.trim());
  else return res.status(400).json({ error: 'group_id must be a positive integer or null' });

  const conn = await db.getConnection();
  try {
    await conn.beginTransaction();
    const [rows] = await conn.query('SELECT student_id, lab_group_id FROM students WHERE student_id = ? AND is_deleted = 0 FOR UPDATE', [id]);
    if (rows.length === 0) { await conn.rollback(); return res.status(404).json({ error: 'Student not found' }); }

    if (groupId !== null) {
      const [g] = await conn.query('SELECT group_name FROM lab_groups WHERE group_id = ?', [groupId]);
      if (g.length === 0) { await conn.rollback(); return res.status(400).json({ error: 'group does not exist' }); }
      await setStudentGroup(conn, id, groupId);
      await notifyStudent(conn, id, 'GROUP_ASSIGNED', 'Group assignment', `You have been assigned to ${g[0].group_name}.`);
    } else {
      await setStudentGroup(conn, id, null);
      await notifyStudent(conn, id, 'GROUP_REMOVED', 'Removed from group', 'You have been removed from your lab group.');
    }
    await conn.commit();
    return res.json({ student_id: id, group_id: groupId });
  } catch (err) {
    try { await conn.rollback(); } catch (_) {}
    if (isGroupFull(err)) return res.status(409).json({ error: 'GROUP_FULL' });
    console.error('assignStudent error:', err);
    return res.status(500).json({ error: 'Failed to assign student' });
  } finally {
    conn.release();
  }
}

// GET /api/lecturer/groups — totals
async function listGroups(req, res) {
  try {
    const [rows] = await db.query('SELECT group_id, group_name, max_members, active_members, places_left FROM v_group_summary ORDER BY group_name');
    return res.json({ groups: rows });
  } catch (err) {
    console.error('listGroups error:', err);
    return res.status(500).json({ error: 'Failed to load groups' });
  }
}

// GET /api/lecturer/requests?status=pending
async function listRequests(req, res) {
  try {
    const status = ['pending', 'approved', 'rejected'].includes(req.query.status) ? req.query.status : null;
    const where = status ? 'WHERE r.status = ?' : '';
    const params = status ? [status] : [];
    const [rows] = await db.query(
      `SELECT r.request_id, r.student_id, s.name AS student_name, s.student_number,
              r.current_group, cg.group_name AS current_group_name,
              r.requested_group, rg.group_name AS requested_group_name,
              r.reason, r.status, r.created_at, r.decided_at
         FROM group_change_requests r
         JOIN students s ON s.student_id = r.student_id
         LEFT JOIN lab_groups cg ON cg.group_id = r.current_group
         LEFT JOIN lab_groups rg ON rg.group_id = r.requested_group
         ${where}
        ORDER BY r.created_at DESC
        LIMIT 100`,
      params
    );
    return res.json({ requests: rows });
  } catch (err) {
    console.error('listRequests error:', err);
    return res.status(500).json({ error: 'Failed to load requests' });
  }
}

// POST /api/lecturer/requests/:id/approve
async function approveRequest(req, res) {
  const id = parseInt(req.params.id, 10);
  if (!Number.isInteger(id) || id < 1) return res.status(400).json({ error: 'invalid request id' });
  const conn = await db.getConnection();
  try {
    await conn.beginTransaction();
    const [rows] = await conn.query(
      "SELECT request_id, student_id, requested_group FROM group_change_requests WHERE request_id = ? AND status = 'pending' FOR UPDATE",
      [id]
    );
    if (rows.length === 0) { await conn.rollback(); return res.status(404).json({ error: 'Pending request not found' }); }
    const r = rows[0];
    await conn.query('SELECT student_id FROM students WHERE student_id = ? AND is_deleted = 0 FOR UPDATE', [r.student_id]);
    const [g] = await conn.query('SELECT group_name FROM lab_groups WHERE group_id = ?', [r.requested_group]);

    await setStudentGroup(conn, r.student_id, r.requested_group);
    await conn.query(
      "UPDATE group_change_requests SET status = 'approved', decided_at = NOW(), decided_by = ? WHERE request_id = ?",
      [req.actor.lecturerId, id]
    );
    await notifyStudent(conn, r.student_id, 'REQUEST_APPROVED', 'Group change approved',
      `Your request to join ${g[0] ? g[0].group_name : 'the group'} was approved.`);
    await notifyStudent(conn, r.student_id, 'GROUP_ASSIGNED', 'Group assignment',
      `You are now in ${g[0] ? g[0].group_name : 'your new group'}.`);
    await conn.commit();
    return res.json({ request_id: id, status: 'approved' });
  } catch (err) {
    try { await conn.rollback(); } catch (_) {}
    if (isGroupFull(err)) return res.status(409).json({ error: 'GROUP_FULL' });
    console.error('approveRequest error:', err);
    return res.status(500).json({ error: 'Failed to approve request' });
  } finally {
    conn.release();
  }
}

// POST /api/lecturer/requests/:id/reject
async function rejectRequest(req, res) {
  const id = parseInt(req.params.id, 10);
  if (!Number.isInteger(id) || id < 1) return res.status(400).json({ error: 'invalid request id' });
  const conn = await db.getConnection();
  try {
    await conn.beginTransaction();
    const [rows] = await conn.query(
      "SELECT request_id, student_id FROM group_change_requests WHERE request_id = ? AND status = 'pending' FOR UPDATE",
      [id]
    );
    if (rows.length === 0) { await conn.rollback(); return res.status(404).json({ error: 'Pending request not found' }); }
    await conn.query(
      "UPDATE group_change_requests SET status = 'rejected', decided_at = NOW(), decided_by = ? WHERE request_id = ?",
      [req.actor.lecturerId, id]
    );
    await notifyStudent(conn, rows[0].student_id, 'REQUEST_REJECTED', 'Group change rejected',
      'Your group change request was not approved.');
    await conn.commit();
    return res.json({ request_id: id, status: 'rejected' });
  } catch (err) {
    try { await conn.rollback(); } catch (_) {}
    console.error('rejectRequest error:', err);
    return res.status(500).json({ error: 'Failed to reject request' });
  } finally {
    conn.release();
  }
}

// GET /api/lecturer/groups/:id/export.xlsx
async function exportGroupXlsx(req, res) {
  const id = parseInt(req.params.id, 10);
  if (!Number.isInteger(id) || id < 1) return res.status(400).json({ error: 'invalid group id' });
  try {
    const [g] = await db.query('SELECT group_name FROM lab_groups WHERE group_id = ?', [id]);
    if (g.length === 0) return res.status(404).json({ error: 'group not found' });
    const [members] = await db.query(
      `SELECT s.name, s.student_number, s.programme_code, s.status
         FROM group_members gm JOIN students s ON s.student_id = gm.student_id
        WHERE gm.group_id = ? AND gm.is_active = 1 AND s.is_deleted = 0
        ORDER BY s.name`,
      [id]
    );
    const buf = await buildMembersWorkbook(g[0].group_name, members);
    res.setHeader('Content-Type', 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet');
    res.setHeader('Content-Disposition', `attachment; filename="${g[0].group_name}-members.xlsx"`);
    return res.send(buf);
  } catch (err) {
    console.error('exportGroupXlsx error:', err);
    return res.status(500).json({ error: 'Failed to export group' });
  }
}

module.exports = {
  listStudents, createStudent, editStudent, softDeleteStudent, assignStudent,
  listGroups, listRequests, approveRequest, rejectRequest, exportGroupXlsx,
};
