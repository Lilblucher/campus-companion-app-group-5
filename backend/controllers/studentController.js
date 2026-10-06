// controllers/studentController.js
// Phase 1 — student dashboard. All routes are student-only (see studentOnly).
// db.query resolves to [rows, fields] (mysql2 pool.execute), so we always
// destructure with: const [rows] = await db.query(...)

const db = require('../config/db');

const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
const PHONE_RE = /^\+?\d{7,15}$/; // same rule as register

// Router-level guard: lecturers (or anything without a student_id) get 403.
function studentOnly(req, res, next) {
  if (!req.user || req.user.role !== 'student' || !req.user.student_id) {
    return res.status(403).json({ error: 'Students only' });
  }
  return next();
}

async function fetchProfile(studentId) {
  const [rows] = await db.query(
    `SELECT s.student_id, s.student_number, s.name, s.programme_code,
            s.email, s.phone, g.group_name AS lab_group,
            s.status, s.record_version
       FROM students s
       LEFT JOIN lab_groups g ON g.group_id = s.lab_group_id
      WHERE s.student_id = ? AND s.is_deleted = 0`,
    [studentId]
  );
  return rows[0] || null;
}

// GET /api/students/me
async function getMe(req, res) {
  try {
    const profile = await fetchProfile(req.user.student_id);
    if (!profile) return res.status(404).json({ error: 'Student not found' });
    return res.json(profile);
  } catch (err) {
    console.error('getMe error:', err);
    return res.status(500).json({ error: 'Failed to load profile' });
  }
}

// PUT /api/students/me   body: { name?, email?, phone? }
async function updateMe(req, res) {
  const body = req.body || {};
  const sets = [];
  const params = [];

  if (body.name !== undefined) {
    const name = typeof body.name === 'string' ? body.name.trim() : '';
    if (name.length < 2 || name.length > 100) {
      return res.status(400).json({ error: 'name must be 2-100 characters' });
    }
    sets.push('name = ?');
    params.push(name);
  }
  if (body.email !== undefined) {
    const email = typeof body.email === 'string' ? body.email.trim().toLowerCase() : '';
    if (!EMAIL_RE.test(email) || email.length > 100) {
      return res.status(400).json({ error: 'email must be a valid email address' });
    }
    sets.push('email = ?');
    params.push(email);
  }
  if (body.phone !== undefined) {
    const phone = typeof body.phone === 'string' ? body.phone.trim() : '';
    if (!PHONE_RE.test(phone)) {
      return res.status(400).json({ error: 'phone must be 7-15 digits, optionally starting with +' });
    }
    sets.push('phone = ?');
    params.push(phone);
  }

  if (sets.length === 0) {
    return res.status(400).json({ error: 'Provide at least one of: name, email, phone' });
  }

  // Any other field in the body (student_number, status, group...) is ignored.
  try {
    const [result] = await db.query(
      `UPDATE students
          SET ${sets.join(', ')}, updated_at = NOW(), record_version = record_version + 1
        WHERE student_id = ? AND is_deleted = 0`,
      [...params, req.user.student_id]
    );
    if (result.affectedRows === 0) {
      return res.status(404).json({ error: 'Student not found' });
    }
    const profile = await fetchProfile(req.user.student_id);
    return res.json(profile);
  } catch (err) {
    if (err.code === 'ER_DUP_ENTRY') {
      return res.status(409).json({ error: 'email already registered' });
    }
    console.error('updateMe error:', err);
    return res.status(500).json({ error: 'Failed to update profile' });
  }
}

// GET /api/students/me/group
async function getMyGroup(req, res) {
  try {
    const [studentRows] = await db.query(
      'SELECT lab_group_id FROM students WHERE student_id = ? AND is_deleted = 0',
      [req.user.student_id]
    );
    if (studentRows.length === 0) {
      return res.status(404).json({ error: 'Student not found' });
    }

    const groupId = studentRows[0].lab_group_id;
    if (groupId === null) {
      return res.json({ group: null, message: 'Not yet assigned to a group' });
    }

    const [groupRows] = await db.query(
      `SELECT group_id, group_name, max_members, active_members
         FROM v_group_summary WHERE group_id = ?`,
      [groupId]
    );
    if (groupRows.length === 0) {
      return res.status(404).json({ error: 'Group not found' });
    }

    const [members] = await db.query(
      `SELECT s.name, s.student_number, s.programme_code
         FROM group_members gm
         JOIN students s ON s.student_id = gm.student_id
        WHERE gm.group_id = ? AND gm.is_active = 1 AND s.is_deleted = 0
        ORDER BY s.name`,
      [groupId]
    );

    const g = groupRows[0];
    return res.json({
      group_id: g.group_id,
      group_name: g.group_name,
      max_members: g.max_members,
      active_members: g.active_members,
      members,
    });
  } catch (err) {
    console.error('getMyGroup error:', err);
    return res.status(500).json({ error: 'Failed to load group' });
  }
}

// POST /api/students/me/group-change-request   body: { requested_group_id, reason? }
async function requestGroupChange(req, res) {
  const body = req.body || {};

  // 1. valid integer id (number or digit-only string)
  const raw = body.requested_group_id;
  const requestedId =
    typeof raw === 'number' ? raw
    : typeof raw === 'string' && /^\d+$/.test(raw.trim()) ? Number(raw.trim())
    : NaN;
  if (!Number.isInteger(requestedId) || requestedId < 1) {
    return res.status(400).json({ error: 'requested_group_id must be a positive integer' });
  }

  let reason = null;
  if (body.reason !== undefined && body.reason !== null) {
    if (typeof body.reason !== 'string' || body.reason.trim().length > 255) {
      return res.status(400).json({ error: 'reason must be a string of at most 255 characters' });
    }
    reason = body.reason.trim() || null;
  }

  try {
    const [studentRows] = await db.query(
      'SELECT lab_group_id FROM students WHERE student_id = ? AND is_deleted = 0',
      [req.user.student_id]
    );
    if (studentRows.length === 0) {
      return res.status(404).json({ error: 'Student not found' });
    }
    const currentGroup = studentRows[0].lab_group_id; // may be null

    // 2. not the group they are already in
    if (currentGroup !== null && currentGroup === requestedId) {
      return res.status(400).json({ error: 'You are already in that group' });
    }

    // 3 + 4. group exists, and has a free place
    const [groupRows] = await db.query(
      'SELECT group_id, places_left FROM v_group_summary WHERE group_id = ?',
      [requestedId]
    );
    if (groupRows.length === 0) {
      return res.status(400).json({ error: 'requested group does not exist' });
    }
    if (groupRows[0].places_left <= 0) {
      return res.status(409).json({ error: 'requested group is full' });
    }

    // 5. no other pending request
    const [pending] = await db.query(
      `SELECT request_id FROM group_change_requests
        WHERE student_id = ? AND status = 'pending' LIMIT 1`,
      [req.user.student_id]
    );
    if (pending.length > 0) {
      return res.status(409).json({ error: 'You already have a pending group change request' });
    }

    // Real column names in schema.sql: current_group, requested_group
    const [result] = await db.query(
      `INSERT INTO group_change_requests
         (student_id, current_group, requested_group, reason, status)
       VALUES (?, ?, ?, ?, 'pending')`,
      [req.user.student_id, currentGroup, requestedId, reason]
    );

    // TODO(Phase 3): notify lecturer CHANGE_REQUEST_RECEIVED

    return res.status(201).json({
      request_id: result.insertId,
      requested_group_id: requestedId,
      status: 'pending',
    });
  } catch (err) {
    console.error('requestGroupChange error:', err);
    return res.status(500).json({ error: 'Failed to submit request' });
  }
}

module.exports = { studentOnly, getMe, updateMe, getMyGroup, requestGroupChange };