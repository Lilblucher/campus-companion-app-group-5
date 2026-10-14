// utils/groupAssignment.js
// Shared by Phase 2 (lecturer panel) and Phase 4 (sync).
//
// assignStudentToGroup(conn, studentId, groupId)
//  - MUST be called with a connection that already has a transaction open.
//    It never commits or rolls back; the caller owns the transaction
//    (e.g. via withTransaction in config/db.js).
//  - Throws an Error whose `.code` is one of:
//      'STUDENT_NOT_FOUND' | 'GROUP_NOT_FOUND' | 'GROUP_FULL'
//  - Returns { studentId, groupId }

function fail(code, message) {
  const err = new Error(message || code);
  err.code = code;
  return err;
}

// The trg_gm_group_capacity trigger raises SQLSTATE 45000 with message GROUP_FULL.
function isGroupFullError(err) {
  return (
    err &&
    (err.sqlState === '45000' || err.code === 'ER_SIGNAL_EXCEPTION') &&
    /GROUP_FULL/.test(err.sqlMessage || err.message || '')
  );
}

async function assignStudentToGroup(conn, studentId, groupId) {
  const sid = Number(studentId);
  const gid = Number(groupId);

  if (!Number.isInteger(sid)) throw fail('STUDENT_NOT_FOUND');
  if (!Number.isInteger(gid)) throw fail('GROUP_NOT_FOUND');

  // 1. Student must exist and not be soft-deleted.
  const [students] = await conn.execute(
    'SELECT student_id FROM students WHERE student_id = ? AND is_deleted = 0 FOR UPDATE',
    [sid]
  );
  if (students.length === 0) throw fail('STUDENT_NOT_FOUND');

  // 2. Group must exist. Lock its row so two simultaneous assignments
  //    cannot both pass the capacity check.
  const [groups] = await conn.execute(
    'SELECT group_id FROM lab_groups WHERE group_id = ? FOR UPDATE',
    [gid]
  );
  if (groups.length === 0) throw fail('GROUP_NOT_FOUND');

  // 3. Already an active member of this group? Nothing to move, and the
  //    capacity check below would wrongly fail if the group is full.
  const [current] = await conn.execute(
    'SELECT group_id FROM group_members WHERE student_id = ? AND is_active = 1',
    [sid]
  );
  if (current.some((row) => Number(row.group_id) === gid)) {
    await conn.execute(
      "UPDATE students SET lab_group_id = ?, status = 'active' WHERE student_id = ?",
      [gid, sid]
    );
    return { studentId: sid, groupId: gid };
  }

  // 4. Capacity check against the view.
  const [summary] = await conn.execute(
    'SELECT places_left FROM v_group_summary WHERE group_id = ?',
    [gid]
  );
  if (summary.length === 0 || Number(summary[0].places_left) <= 0) {
    throw fail('GROUP_FULL');
  }

  // 5. Move the student: profile row, close old membership, open new one.
  try {
    await conn.execute(
      "UPDATE students SET lab_group_id = ?, status = 'active' WHERE student_id = ?",
      [gid, sid]
    );
    await conn.execute(
      'UPDATE group_members SET is_active = 0, left_at = NOW() WHERE student_id = ? AND is_active = 1',
      [sid]
    );
    await conn.execute(
      'INSERT INTO group_members (student_id, group_id, is_active) VALUES (?, ?, 1)',
      [sid, gid]
    );
  } catch (err) {
    if (isGroupFullError(err)) throw fail('GROUP_FULL');
    throw err;
  }

  return { studentId: sid, groupId: gid };
}

module.exports = { assignStudentToGroup };
