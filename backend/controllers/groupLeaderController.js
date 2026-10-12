// controllers/groupLeaderController.js
// Gated by auth + roles.requireLeaderOrLecturer.
//   group leader -> acts on their OWN group (req.actor.groupId)
//   lecturer     -> may target any group via ?group_id= / body.group_id (override)

const db = require('../config/db');
const { setStudentGroup, isGroupFull } = require('../utils/membership');
const { notifyStudent, notifyAllLecturers } = require('../utils/notify');
const { buildMembersWorkbook } = require('../utils/xlsx');

// Resolve the group this request acts on. null => caller must specify / has none.
function targetGroupId(req) {
  if (req.actor.level === 'lecturer') {
    const q = (req.query && req.query.group_id) || (req.body && req.body.group_id);
    return q != null && /^\d+$/.test(String(q)) ? Number(q) : null;
  }
  return req.actor.groupId || null; // leader's own group
}

async function groupNameOf(id) {
  const [g] = await db.query('SELECT group_name FROM lab_groups WHERE group_id = ?', [id]);
  return g.length ? g[0].group_name : null;
}

async function fetchMembers(groupId) {
  const [rows] = await db.query(
    `SELECT s.student_id, s.name, s.student_number, s.programme_code, s.status, s.is_group_leader
       FROM group_members gm JOIN students s ON s.student_id = gm.student_id
      WHERE gm.group_id = ? AND gm.is_active = 1 AND s.is_deleted = 0
      ORDER BY s.name`,
    [groupId]
  );
  return rows;
}

// GET /api/group/members
async function getMembers(req, res) {
  const groupId = targetGroupId(req);
  if (!groupId) {
    return res.status(req.actor.level === 'lecturer' ? 400 : 409)
      .json({ error: req.actor.level === 'lecturer' ? 'group_id is required' : 'You are not assigned to a group yet' });
  }
  try {
    const [g] = await db.query('SELECT group_id, group_name, max_members, active_members, places_left FROM v_group_summary WHERE group_id = ?', [groupId]);
    if (g.length === 0) return res.status(404).json({ error: 'group not found' });
    const members = await fetchMembers(groupId);
    return res.json({ ...g[0], members });
  } catch (err) {
    console.error('getMembers error:', err);
    return res.status(500).json({ error: 'Failed to load members' });
  }
}

// POST /api/group/members  { student_number }
// Adds an existing UNASSIGNED student to the caller's group (15-cap enforced).
async function addMember(req, res) {
  const groupId = targetGroupId(req);
  if (!groupId) return res.status(req.actor.level === 'lecturer' ? 400 : 409)
    .json({ error: req.actor.level === 'lecturer' ? 'group_id is required' : 'You are not assigned to a group yet' });
  const sn = typeof (req.body && req.body.student_number) === 'string' ? req.body.student_number.trim() : '';
  if (!/^\d{9}$/.test(sn)) return res.status(400).json({ error: 'student_number must be exactly 9 digits' });

  const conn = await db.getConnection();
  try {
    await conn.beginTransaction();
    const [rows] = await conn.query(
      'SELECT student_id, lab_group_id FROM students WHERE student_number = ? AND is_deleted = 0 FOR UPDATE', [sn]);
    if (rows.length === 0) { await conn.rollback(); return res.status(404).json({ error: 'No student with that number' }); }
    const student = rows[0];
    if (student.lab_group_id === groupId) { await conn.rollback(); return res.status(409).json({ error: 'Student is already in this group' }); }
    if (student.lab_group_id !== null) { await conn.rollback(); return res.status(409).json({ error: 'Student is already in another group' }); }

    await setStudentGroup(conn, student.student_id, groupId);
    const gname = await groupNameOf(groupId);
    await notifyStudent(conn, student.student_id, 'GROUP_ASSIGNED', 'Added to group', `You have been added to ${gname}.`);
    await conn.commit();
    return res.status(201).json({ student_id: student.student_id, student_number: sn, group_id: groupId });
  } catch (err) {
    try { await conn.rollback(); } catch (_) {}
    if (isGroupFull(err)) return res.status(409).json({ error: 'GROUP_FULL' });
    console.error('addMember error:', err);
    return res.status(500).json({ error: 'Failed to add member' });
  } finally {
    conn.release();
  }
}

// DELETE /api/group/members/:student_id  — remove from the caller's group -> unassigned
async function removeMember(req, res) {
  const groupId = targetGroupId(req);
  if (!groupId) return res.status(req.actor.level === 'lecturer' ? 400 : 409)
    .json({ error: req.actor.level === 'lecturer' ? 'group_id is required' : 'You are not assigned to a group yet' });
  const sid = parseInt(req.params.student_id, 10);
  if (!Number.isInteger(sid) || sid < 1) return res.status(400).json({ error: 'invalid student id' });

  const conn = await db.getConnection();
  try {
    await conn.beginTransaction();
    const [rows] = await conn.query(
      'SELECT student_id, lab_group_id FROM students WHERE student_id = ? AND is_deleted = 0 FOR UPDATE', [sid]);
    if (rows.length === 0) { await conn.rollback(); return res.status(404).json({ error: 'Student not found' }); }
    if (rows[0].lab_group_id !== groupId) { await conn.rollback(); return res.status(409).json({ error: 'Student is not in this group' }); }

    await setStudentGroup(conn, sid, null);
    await notifyStudent(conn, sid, 'GROUP_REMOVED', 'Removed from group', 'You have been removed from your lab group.');
    await conn.commit();
    return res.json({ student_id: sid, removed: true });
  } catch (err) {
    try { await conn.rollback(); } catch (_) {}
    console.error('removeMember error:', err);
    return res.status(500).json({ error: 'Failed to remove member' });
  } finally {
    conn.release();
  }
}

// GET /api/group/export.xlsx
async function exportXlsx(req, res) {
  const groupId = targetGroupId(req);
  if (!groupId) return res.status(400).json({ error: 'no group to export' });
  try {
    const gname = await groupNameOf(groupId);
    if (!gname) return res.status(404).json({ error: 'group not found' });
    const members = await fetchMembers(groupId);
    const buf = await buildMembersWorkbook(gname, members);
    res.setHeader('Content-Type', 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet');
    res.setHeader('Content-Disposition', `attachment; filename="${gname}-members.xlsx"`);
    return res.send(buf);
  } catch (err) {
    console.error('group exportXlsx error:', err);
    return res.status(500).json({ error: 'Failed to export group' });
  }
}

// POST /api/group/export/send-to-lecturer — notify lecturers that the roster was shared.
async function sendToLecturer(req, res) {
  const groupId = targetGroupId(req);
  if (!groupId) return res.status(409).json({ error: 'You are not assigned to a group yet' });
  try {
    const gname = await groupNameOf(groupId);
    const members = await fetchMembers(groupId);
    const count = await notifyAllLecturers(db, 'GROUP_ROSTER_SENT',
      `Group ${gname} roster sent`,
      `The ${gname} group leader shared the member list (${members.length} member${members.length === 1 ? '' : 's'}).`);
    return res.json({ group_id: groupId, members: members.length, lecturers_notified: count });
  } catch (err) {
    console.error('sendToLecturer error:', err);
    return res.status(500).json({ error: 'Failed to send roster to lecturer' });
  }
}

module.exports = { getMembers, addMember, removeMember, exportXlsx, sendToLecturer };
