// utils/membership.js
// Assign / transfer / unassign a student's active lab-group membership while
// keeping group_members (which drives the one-active-membership and 15-member
// triggers) and students.lab_group_id/status consistent.
//
// Call inside a transaction where the student row is already locked FOR UPDATE.
// A full target group makes the INSERT raise SIGNAL 'GROUP_FULL' (SQLSTATE
// 45000) — callers should catch and surface that as 409.

async function setStudentGroup(conn, studentId, groupId) {
  // Release any current active place first.
  await conn.query(
    'UPDATE group_members SET is_active = 0, left_at = NOW() WHERE student_id = ? AND is_active = 1',
    [studentId]
  );

  if (groupId === null || groupId === undefined) {
    await conn.query(
      "UPDATE students SET lab_group_id = NULL, status = 'unassigned' WHERE student_id = ?",
      [studentId]
    );
    return;
  }

  // Triggers enforce one-active-membership + GROUP_FULL (max 15).
  await conn.query(
    'INSERT INTO group_members (student_id, group_id, is_active) VALUES (?, ?, 1)',
    [studentId, groupId]
  );
  await conn.query(
    "UPDATE students SET lab_group_id = ?, status = 'active' WHERE student_id = ?",
    [groupId, studentId]
  );
}

// True when a caught DB error is the capacity trigger firing.
function isGroupFull(err) {
  return !!err && (String(err.sqlMessage || '').includes('GROUP_FULL'));
}

module.exports = { setStudentGroup, isGroupFull };
