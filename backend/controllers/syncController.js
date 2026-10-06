// controllers/syncController.js
// Phase 4 — offline sync. The Android SyncWorker sends ONE queued operation per request.
//
// HTTP contract (the Android team should follow this):
//   400  malformed request (bad operation_id / op_type / payload / base_version) — do NOT retry
//   401/403  not logged in / not a student
//   409  operation_id was already used by a different account — do NOT retry
//   500  server problem — retry later
//   200  the operation was processed. Read body.status:
//          "synced"   applied
//          "conflict" base_version was stale; body.result.server has the current profile
//          "failed"   permanent business/validation failure (body.error_code + body.message) — do NOT retry
//   Re-sending the same operation_id returns the stored response and never applies the operation twice.
//
// The student is always taken from the JWT (req.user.student_id). Any student_id in the body is ignored.
// db.getConnection() gives a mysql2 promise connection; conn.query resolves to [rows, fields].

const db = require('../config/db');

const UUID_RE = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;
const OP_TYPES = ['create', 'update', 'delete', 'assign_group', 'transfer_group'];

// Keep these in sync with studentController.updateMe / authController.register
const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
const PHONE_RE = /^\+?\d{7,15}$/;

const isPlainObject = (v) => v !== null && typeof v === 'object' && !Array.isArray(v);
const synced = (result) => ({ status: 'synced', result });
const failed = (error_code, message) => ({ status: 'failed', error_code, message });

function parseId(raw) {
  const id =
    typeof raw === 'number' ? raw
    : typeof raw === 'string' && /^\d+$/.test(raw.trim()) ? Number(raw.trim())
    : NaN;
  return Number.isInteger(id) && id >= 1 ? id : null;
}

async function fetchProfile(conn, studentId) {
  const [rows] = await conn.query(
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

// ---------------------------------------------------------------------------
// op_type = 'update'  payload: { name?, email?, phone? }, base_version required
// ---------------------------------------------------------------------------
async function handleUpdate(conn, user, payload, baseVersion) {
  const sets = [];
  const params = [];

  if (payload.name !== undefined) {
    const name = typeof payload.name === 'string' ? payload.name.trim() : '';
    if (name.length < 2 || name.length > 100) {
      return failed('VALIDATION_FAILED', 'name must be 2-100 characters');
    }
    sets.push('name = ?');
    params.push(name);
  }
  if (payload.email !== undefined) {
    const email = typeof payload.email === 'string' ? payload.email.trim().toLowerCase() : '';
    if (!EMAIL_RE.test(email) || email.length > 100) {
      return failed('VALIDATION_FAILED', 'email must be a valid email address');
    }
    sets.push('email = ?');
    params.push(email);
  }
  if (payload.phone !== undefined) {
    const phone = typeof payload.phone === 'string' ? payload.phone.trim() : '';
    if (!PHONE_RE.test(phone)) {
      return failed('VALIDATION_FAILED', 'phone must be 7-15 digits, optionally starting with +');
    }
    sets.push('phone = ?');
    params.push(phone);
  }
  if (sets.length === 0) {
    return failed('VALIDATION_FAILED', 'payload must include at least one of: name, email, phone');
  }

  // Lock the row so two syncs for the same student can't both pass the version check
  const [rows] = await conn.query(
    'SELECT record_version FROM students WHERE student_id = ? AND is_deleted = 0 FOR UPDATE',
    [user.student_id]
  );
  if (rows.length === 0) return failed('STUDENT_NOT_FOUND', 'Student not found');

  const serverVersion = Number(rows[0].record_version);
  if (serverVersion !== baseVersion) {
    return {
      status: 'conflict',
      error_code: 'VERSION_CONFLICT',
      message: 'The profile changed on the server after this edit was made',
      result: { server_version: serverVersion, server: await fetchProfile(conn, user.student_id) },
    };
  }

  try {
    await conn.query(
      `UPDATE students
          SET ${sets.join(', ')}, updated_at = NOW(), record_version = record_version + 1
        WHERE student_id = ?`,
      [...params, user.student_id]
    );
  } catch (err) {
    if (err.code === 'ER_DUP_ENTRY') return failed('EMAIL_TAKEN', 'email already registered');
    throw err;
  }
  return synced(await fetchProfile(conn, user.student_id));
}

// ---------------------------------------------------------------------------
// op_type = 'transfer_group'  payload: { requested_group_id, reason? }
// Same checks, in the same order, as POST /api/students/me/group-change-request
// ---------------------------------------------------------------------------
async function handleTransferGroup(conn, user, payload) {
  const requestedId = parseId(payload.requested_group_id);
  if (requestedId === null) {
    return failed('VALIDATION_FAILED', 'requested_group_id must be a positive integer');
  }

  let reason = null;
  if (payload.reason !== undefined && payload.reason !== null) {
    if (typeof payload.reason !== 'string' || payload.reason.trim().length > 255) {
      return failed('VALIDATION_FAILED', 'reason must be a string of at most 255 characters');
    }
    reason = payload.reason.trim() || null;
  }

  // Lock the student row so two syncs can't both pass the "no pending request" check
  const [studentRows] = await conn.query(
    'SELECT lab_group_id FROM students WHERE student_id = ? AND is_deleted = 0 FOR UPDATE',
    [user.student_id]
  );
  if (studentRows.length === 0) return failed('STUDENT_NOT_FOUND', 'Student not found');
  const currentGroup = studentRows[0].lab_group_id; // may be null

  if (currentGroup !== null && Number(currentGroup) === requestedId) {
    return failed('ALREADY_IN_GROUP', 'You are already in that group');
  }

  const [groupRows] = await conn.query(
    'SELECT group_id, places_left FROM v_group_summary WHERE group_id = ?',
    [requestedId]
  );
  if (groupRows.length === 0) return failed('GROUP_NOT_FOUND', 'requested group does not exist');
  if (Number(groupRows[0].places_left) <= 0) return failed('GROUP_FULL', 'requested group is full');

  const [pending] = await conn.query(
    `SELECT request_id FROM group_change_requests
      WHERE student_id = ? AND status = 'pending' LIMIT 1`,
    [user.student_id]
  );
  if (pending.length > 0) {
    return failed('PENDING_REQUEST_EXISTS', 'You already have a pending group change request');
  }

  const [result] = await conn.query(
    `INSERT INTO group_change_requests
       (student_id, current_group, requested_group, reason, status)
     VALUES (?, ?, ?, ?, 'pending')`,
    [user.student_id, currentGroup, requestedId, reason]
  );

  // TODO(Phase 3): notify lecturer CHANGE_REQUEST_RECEIVED

  return synced({ request_id: result.insertId, requested_group_id: requestedId, status: 'pending' });
}

// ---------------------------------------------------------------------------
// Operations the schema allows but sync does not support (yet)
// ---------------------------------------------------------------------------
const UNSUPPORTED = {
  create: 'Registration is public: use POST /api/auth/register, not sync',
  delete: 'Students cannot delete records through sync',
  assign_group: 'Group assignment is a lecturer action and is not available through sync yet',
};
const unsupported = (opType) => async () => failed('UNSUPPORTED_OP', UNSUPPORTED[opType]);

const HANDLERS = {
  update: handleUpdate,
  transfer_group: handleTransferGroup,
  create: unsupported('create'),
  delete: unsupported('delete'),
  assign_group: unsupported('assign_group'),
};

// ---------------------------------------------------------------------------
// Idempotent replay
// ---------------------------------------------------------------------------
async function findStored(operationId) {
  const [rows] = await db.query(
    'SELECT account_id, response_body FROM sync_operations WHERE operation_id = ?',
    [operationId]
  );
  return rows[0] || null;
}

function replay(res, stored, accountId) {
  if (Number(stored.account_id) !== Number(accountId)) {
    return res.status(409).json({ error: 'operation_id already used by another account' });
  }
  let body = stored.response_body; // mysql2 returns JSON columns already parsed
  if (typeof body === 'string') {
    try { body = JSON.parse(body); } catch (_) { body = null; }
  }
  if (!body) return res.status(409).json({ error: 'operation is still being processed' });
  return res.json(body);
}

// ---------------------------------------------------------------------------
// POST /api/sync   (also reachable as POST /api/sync/push)
// Body: { operation_id, op_type, payload, base_version }
// ---------------------------------------------------------------------------
async function push(req, res) {
  const body = req.body || {};
  const { operation_id, op_type, payload } = body;

  if (typeof operation_id !== 'string' || !UUID_RE.test(operation_id)) {
    return res.status(400).json({ error: 'operation_id must be a UUID' });
  }
  if (!OP_TYPES.includes(op_type)) {
    return res.status(400).json({ error: `op_type must be one of: ${OP_TYPES.join(', ')}` });
  }
  if (!isPlainObject(payload)) {
    return res.status(400).json({ error: 'payload must be a JSON object' });
  }
  let baseVersion = 0;
  if (body.base_version !== undefined && body.base_version !== null) {
    if (!Number.isInteger(body.base_version) || body.base_version < 0) {
      return res.status(400).json({ error: 'base_version must be a non-negative integer' });
    }
    baseVersion = body.base_version;
  }
  if (op_type === 'update' && baseVersion < 1) {
    return res.status(400).json({ error: 'base_version (>= 1) is required for update' });
  }

  const accountId = req.user.account_id;

  try {
    // 1. Already processed? Return the stored answer.
    const existing = await findStored(operation_id);
    if (existing) return replay(res, existing, accountId);
  } catch (err) {
    console.error('sync lookup error:', err);
    return res.status(500).json({ error: 'Sync failed' });
  }

  // 2. Process inside one transaction: claim the operation_id, apply it, store the outcome.
  const conn = await db.getConnection();
  try {
    await conn.beginTransaction();

    await conn.query(
      `INSERT INTO sync_operations
         (operation_id, account_id, student_id, op_type, payload, base_version)
       VALUES (?, ?, ?, ?, ?, ?)`,
      [operation_id, accountId, req.user.student_id, op_type, JSON.stringify(payload), baseVersion]
    );

    const outcome = await HANDLERS[op_type](conn, req.user, payload, baseVersion);
    const response = { operation_id, ...outcome };

    await conn.query(
      `UPDATE sync_operations
          SET status = ?, response_body = ?, error_code = ?, processed_at = NOW()
        WHERE operation_id = ?`,
      [outcome.status, JSON.stringify(response), outcome.error_code || null, operation_id]
    );

    await conn.commit();
    return res.json(response);
  } catch (err) {
    try { await conn.rollback(); } catch (_) { /* ignore */ }

    // Two requests with the same operation_id raced: the loser rolls back and gets the winner's answer
    if (err.code === 'ER_DUP_ENTRY') {
      try {
        const existing = await findStored(operation_id);
        if (existing) return replay(res, existing, accountId);
      } catch (_) { /* fall through to 500 */ }
    }
    console.error('sync push error:', err);
    return res.status(500).json({ error: 'Sync failed' });
  } finally {
    conn.release();
  }
}

module.exports = { push };