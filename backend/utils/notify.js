// utils/notify.js — Phase 3 internal helper (NOT an HTTP endpoint).
//
// `conn` may be either:
//   * a transaction connection from db.getConnection() — PREFERRED, so the
//     notification commits or rolls back together with the action that caused
//     it (a failed transaction must leave no stray notification), or
//   * the pool itself (require('../config/db')) for a standalone send.
// Both expose `.query(sql, params)` resolving to [rows, fields] (mysql2).
//
// The recipient column is accounts.account_id, never student_id — notifyStudent
// resolves the student's account first.

async function createNotification(conn, accountId, type, title, body) {
  await conn.query(
    `INSERT INTO notifications (account_id, type, title, body) VALUES (?, ?, ?, ?)`,
    [accountId, type, title, body]
  );
}

// Notify one student (by student_id). No-op if the student has no account row.
async function notifyStudent(conn, studentId, type, title, body) {
  const [rows] = await conn.query(
    `SELECT account_id FROM accounts WHERE student_id = ? AND role = 'student' LIMIT 1`,
    [studentId]
  );
  if (rows.length === 0) return 0;
  await createNotification(conn, rows[0].account_id, type, title, body);
  return 1;
}

// Notify every active lecturer account. Returns how many were notified.
async function notifyAllLecturers(conn, type, title, body) {
  const [rows] = await conn.query(
    `SELECT account_id FROM accounts WHERE role = 'lecturer' AND is_locked = 0`
  );
  for (const r of rows) {
    await createNotification(conn, r.account_id, type, title, body);
  }
  return rows.length;
}

module.exports = { createNotification, notifyStudent, notifyAllLecturers };
