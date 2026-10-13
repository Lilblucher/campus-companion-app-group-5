// controllers/reminderController.js
// Gated by auth + roles.requireLeaderOrLecturer (both elevated roles create;
// everyone authenticated can read the ones addressed to them via /visible).
// Leaders are limited to personal/own-group scope; lecturers may use any scope.
// Creating a reminder fans out REMINDER notifications to its recipients.

const db = require('../config/db');
const { createNotification } = require('../utils/notify');
const { resolveActor } = require('../middleware/roles');

const SCOPES = ['personal', 'group', 'programme', 'all'];
const PROGRAMMES = ['CS', 'IT', 'DS'];

// Account ids that should receive a reminder of this scope.
async function recipientAccounts(conn, reminder, creatorAccountId) {
  if (reminder.scope === 'personal') return [creatorAccountId];
  let sql = `SELECT a.account_id FROM accounts a JOIN students s ON s.student_id = a.student_id
             WHERE s.is_deleted = 0 AND a.is_locked = 0`;
  const params = [];
  if (reminder.scope === 'group') { sql += ' AND s.lab_group_id = ?'; params.push(reminder.target_group_id); }
  else if (reminder.scope === 'programme') { sql += ' AND s.programme_code = ?'; params.push(reminder.target_programme); }
  // 'all' => every active student
  const [rows] = await conn.query(sql, params);
  return rows.map((r) => r.account_id);
}

// GET /api/reminders  — reminders addressed to the caller
async function listReminders(req, res) {
  try {
    const a = req.actor || await resolveActor(req); // GET is open to any authenticated user
    let rows;
    if (a.level === 'lecturer') {
      [rows] = await db.query('SELECT * FROM reminders ORDER BY COALESCE(remind_at, created_at) DESC LIMIT 200');
    } else {
      [rows] = await db.query(
        `SELECT * FROM reminders
          WHERE scope = 'all'
             OR (scope = 'group' AND target_group_id = ?)
             OR (scope = 'programme' AND target_programme = ?)
             OR (scope = 'personal' AND created_by = ?)
          ORDER BY COALESCE(remind_at, created_at) DESC LIMIT 200`,
        [a.groupId, a.programme, a.accountId]
      );
    }
    return res.json({ reminders: rows });
  } catch (err) {
    console.error('listReminders error:', err);
    return res.status(500).json({ error: 'Failed to load reminders' });
  }
}

// POST /api/reminders  { title, body?, scope, target_group_id?, target_programme?, remind_at? }
async function createReminder(req, res) {
  const a = req.actor;
  const b = req.body || {};
  const title = typeof b.title === 'string' ? b.title.trim() : '';
  if (title.length < 2 || title.length > 120) return res.status(400).json({ error: 'title must be 2-120 characters' });
  const body = typeof b.body === 'string' ? b.body.trim().slice(0, 500) : '';
  let scope = SCOPES.includes(b.scope) ? b.scope : 'personal';

  // Group leaders are limited to personal or their own group.
  if (a.level === 'group_leader') {
    if (scope === 'programme' || scope === 'all') {
      return res.status(403).json({ error: 'Group leaders can only set personal or group reminders' });
    }
  }

  let targetGroupId = null, targetProgramme = null;
  if (scope === 'group') {
    if (a.level === 'lecturer') {
      const g = b.target_group_id;
      if (!(g != null && /^\d+$/.test(String(g)))) return res.status(400).json({ error: 'target_group_id required for a group reminder' });
      targetGroupId = Number(g);
    } else {
      if (!a.groupId) return res.status(409).json({ error: 'You are not assigned to a group' });
      targetGroupId = a.groupId;
    }
  } else if (scope === 'programme') {
    if (!PROGRAMMES.includes(b.target_programme)) return res.status(400).json({ error: 'target_programme must be CS, IT or DS' });
    targetProgramme = b.target_programme;
  }

  let remindAt = null;
  if (b.remind_at !== undefined && b.remind_at !== null && String(b.remind_at).trim() !== '') {
    const d = new Date(b.remind_at);
    if (isNaN(d.getTime())) return res.status(400).json({ error: 'remind_at must be a valid date/time' });
    remindAt = d.toISOString().slice(0, 19).replace('T', ' ');
  }

  const conn = await db.getConnection();
  try {
    await conn.beginTransaction();
    const [result] = await conn.query(
      `INSERT INTO reminders (created_by, creator_role, scope, target_group_id, target_programme, title, body, remind_at)
       VALUES (?, ?, ?, ?, ?, ?, ?, ?)`,
      [a.accountId, a.level, scope, targetGroupId, targetProgramme, title, body, remindAt]
    );
    const reminder = { scope, target_group_id: targetGroupId, target_programme: targetProgramme };
    const recipients = await recipientAccounts(conn, reminder, a.accountId);
    for (const accId of recipients) {
      await createNotification(conn, accId, 'REMINDER', title, body || 'New reminder');
    }
    await conn.commit();
    return res.status(201).json({ reminder_id: result.insertId, scope, recipients: recipients.length });
  } catch (err) {
    try { await conn.rollback(); } catch (_) {}
    console.error('createReminder error:', err);
    return res.status(500).json({ error: 'Failed to create reminder' });
  } finally {
    conn.release();
  }
}

// PATCH /api/reminders/:id  { title?, body?, remind_at? }  (own, or lecturer override)
async function updateReminder(req, res) {
  const id = parseInt(req.params.id, 10);
  if (!Number.isInteger(id) || id < 1) return res.status(400).json({ error: 'invalid reminder id' });
  const a = req.actor, b = req.body || {};
  const sets = [], params = [];
  if (b.title !== undefined) {
    const t = typeof b.title === 'string' ? b.title.trim() : '';
    if (t.length < 2 || t.length > 120) return res.status(400).json({ error: 'title must be 2-120 characters' });
    sets.push('title = ?'); params.push(t);
  }
  if (b.body !== undefined) { sets.push('body = ?'); params.push(typeof b.body === 'string' ? b.body.trim().slice(0, 500) : ''); }
  if (b.remind_at !== undefined) {
    if (b.remind_at === null || String(b.remind_at).trim() === '') { sets.push('remind_at = NULL'); }
    else { const d = new Date(b.remind_at); if (isNaN(d.getTime())) return res.status(400).json({ error: 'remind_at must be a valid date/time' }); sets.push('remind_at = ?'); params.push(d.toISOString().slice(0, 19).replace('T', ' ')); }
  }
  if (sets.length === 0) return res.status(400).json({ error: 'nothing to update' });
  let sql = `UPDATE reminders SET ${sets.join(', ')} WHERE reminder_id = ?`;
  params.push(id);
  if (a.level !== 'lecturer') { sql += ' AND created_by = ?'; params.push(a.accountId); }
  try {
    const [r] = await db.query(sql, params);
    if (r.affectedRows === 0) return res.status(404).json({ error: 'Reminder not found' });
    return res.json({ reminder_id: id, updated: true });
  } catch (err) {
    console.error('updateReminder error:', err);
    return res.status(500).json({ error: 'Failed to update reminder' });
  }
}

// DELETE /api/reminders/:id  (own, or lecturer override)
async function deleteReminder(req, res) {
  const id = parseInt(req.params.id, 10);
  if (!Number.isInteger(id) || id < 1) return res.status(400).json({ error: 'invalid reminder id' });
  const a = req.actor;
  try {
    let r;
    if (a.level === 'lecturer') [r] = await db.query('DELETE FROM reminders WHERE reminder_id = ?', [id]);
    else [r] = await db.query('DELETE FROM reminders WHERE reminder_id = ? AND created_by = ?', [id, a.accountId]);
    if (r.affectedRows === 0) return res.status(404).json({ error: 'Reminder not found' });
    return res.json({ reminder_id: id, deleted: true });
  } catch (err) {
    console.error('deleteReminder error:', err);
    return res.status(500).json({ error: 'Failed to delete reminder' });
  }
}

module.exports = { listReminders, createReminder, updateReminder, deleteReminder };
