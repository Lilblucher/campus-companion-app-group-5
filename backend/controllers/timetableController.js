// controllers/timetableController.js
// GET is readable by any authenticated user (students see their group's +
// all-group sessions); writes are lecturer-only (enforced by the route).

const db = require('../config/db');
const { resolveActor } = require('../middleware/roles');

function validTime(t) { return typeof t === 'string' && /^([01]?\d|2[0-3]):[0-5]\d(:[0-5]\d)?$/.test(t); }

async function applicableSessions(actor) {
  if (actor.level === 'lecturer') {
    const [rows] = await db.query(
      `SELECT t.*, g.group_name FROM timetable t LEFT JOIN lab_groups g ON g.group_id = t.group_id
        ORDER BY t.day_of_week, t.start_time`);
    return rows;
  }
  const [rows] = await db.query(
    `SELECT t.*, g.group_name FROM timetable t LEFT JOIN lab_groups g ON g.group_id = t.group_id
      WHERE t.group_id IS NULL OR t.group_id = ?
      ORDER BY t.day_of_week, t.start_time`,
    [actor.groupId]
  );
  return rows;
}

// GET /api/timetable
async function listTimetable(req, res) {
  try {
    const actor = await resolveActor(req);
    const sessions = await applicableSessions(actor);
    return res.json({ sessions });
  } catch (err) {
    console.error('listTimetable error:', err);
    return res.status(500).json({ error: 'Failed to load timetable' });
  }
}

// GET /api/timetable/next — soonest upcoming session for the caller (this week, wrapping).
async function nextSession(req, res) {
  try {
    const actor = await resolveActor(req);
    const sessions = await applicableSessions(actor);
    if (sessions.length === 0) return res.json({ next: null });
    const now = new Date();
    const nowKey = now.getDay() * 1440 + now.getHours() * 60 + now.getMinutes();
    const withKey = sessions.map((s) => {
      const [h, m] = String(s.start_time).split(':').map(Number);
      const key = s.day_of_week * 1440 + h * 60 + m;
      return { s, delta: (key - nowKey + 7 * 1440) % (7 * 1440) };
    }).sort((a, b) => a.delta - b.delta);
    return res.json({ next: withKey[0].s });
  } catch (err) {
    console.error('nextSession error:', err);
    return res.status(500).json({ error: 'Failed to load next class' });
  }
}

// POST /api/timetable  (lecturer only)
async function createSession(req, res) {
  const b = req.body || {};
  const courseCode = typeof b.course_code === 'string' ? b.course_code.trim() : '';
  const title = typeof b.title === 'string' ? b.title.trim() : '';
  if (!courseCode || courseCode.length > 20) return res.status(400).json({ error: 'course_code required (<=20 chars)' });
  if (title.length < 2 || title.length > 120) return res.status(400).json({ error: 'title must be 2-120 characters' });
  const day = Number(b.day_of_week);
  if (!Number.isInteger(day) || day < 0 || day > 6) return res.status(400).json({ error: 'day_of_week must be 0 (Sun) .. 6 (Sat)' });
  if (!validTime(b.start_time) || !validTime(b.end_time)) return res.status(400).json({ error: 'start_time/end_time must be HH:MM' });
  let groupId = null;
  if (b.group_id != null && String(b.group_id).trim() !== '') {
    if (!/^\d+$/.test(String(b.group_id))) return res.status(400).json({ error: 'group_id must be an integer or null' });
    groupId = Number(b.group_id);
  }
  const venue = typeof b.venue === 'string' ? b.venue.trim().slice(0, 120) : null;
  const room = typeof b.room === 'string' ? b.room.trim().slice(0, 60) : null;
  try {
    const [r] = await db.query(
      `INSERT INTO timetable (course_code, title, group_id, day_of_week, start_time, end_time, venue, room, created_by)
       VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)`,
      [courseCode, title, groupId, day, b.start_time, b.end_time, venue, room, req.actor.accountId]
    );
    return res.status(201).json({ session_id: r.insertId });
  } catch (err) {
    if (err.code === 'ER_NO_REFERENCED_ROW_2') return res.status(400).json({ error: 'group does not exist' });
    console.error('createSession error:', err);
    return res.status(500).json({ error: 'Failed to create session' });
  }
}

// PATCH /api/timetable/:id  (lecturer only)
async function updateSession(req, res) {
  const id = parseInt(req.params.id, 10);
  if (!Number.isInteger(id) || id < 1) return res.status(400).json({ error: 'invalid session id' });
  const b = req.body || {};
  const sets = [], params = [];
  if (b.course_code !== undefined) { const v = String(b.course_code).trim(); if (!v || v.length > 20) return res.status(400).json({ error: 'invalid course_code' }); sets.push('course_code = ?'); params.push(v); }
  if (b.title !== undefined) { const v = String(b.title).trim(); if (v.length < 2 || v.length > 120) return res.status(400).json({ error: 'invalid title' }); sets.push('title = ?'); params.push(v); }
  if (b.day_of_week !== undefined) { const d = Number(b.day_of_week); if (!Number.isInteger(d) || d < 0 || d > 6) return res.status(400).json({ error: 'invalid day_of_week' }); sets.push('day_of_week = ?'); params.push(d); }
  if (b.start_time !== undefined) { if (!validTime(b.start_time)) return res.status(400).json({ error: 'invalid start_time' }); sets.push('start_time = ?'); params.push(b.start_time); }
  if (b.end_time !== undefined) { if (!validTime(b.end_time)) return res.status(400).json({ error: 'invalid end_time' }); sets.push('end_time = ?'); params.push(b.end_time); }
  if (b.group_id !== undefined) {
    if (b.group_id === null || String(b.group_id).trim() === '') { sets.push('group_id = NULL'); }
    else { if (!/^\d+$/.test(String(b.group_id))) return res.status(400).json({ error: 'invalid group_id' }); sets.push('group_id = ?'); params.push(Number(b.group_id)); }
  }
  if (b.venue !== undefined) { sets.push('venue = ?'); params.push(b.venue == null ? null : String(b.venue).slice(0, 120)); }
  if (b.room !== undefined) { sets.push('room = ?'); params.push(b.room == null ? null : String(b.room).slice(0, 60)); }
  if (sets.length === 0) return res.status(400).json({ error: 'nothing to update' });
  try {
    const [r] = await db.query(`UPDATE timetable SET ${sets.join(', ')} WHERE session_id = ?`, [...params, id]);
    if (r.affectedRows === 0) return res.status(404).json({ error: 'Session not found' });
    return res.json({ session_id: id, updated: true });
  } catch (err) {
    console.error('updateSession error:', err);
    return res.status(500).json({ error: 'Failed to update session' });
  }
}

// DELETE /api/timetable/:id  (lecturer only)
async function deleteSession(req, res) {
  const id = parseInt(req.params.id, 10);
  if (!Number.isInteger(id) || id < 1) return res.status(400).json({ error: 'invalid session id' });
  try {
    const [r] = await db.query('DELETE FROM timetable WHERE session_id = ?', [id]);
    if (r.affectedRows === 0) return res.status(404).json({ error: 'Session not found' });
    return res.json({ session_id: id, deleted: true });
  } catch (err) {
    console.error('deleteSession error:', err);
    return res.status(500).json({ error: 'Failed to delete session' });
  }
}

module.exports = { listTimetable, nextSession, createSession, updateSession, deleteSession };
