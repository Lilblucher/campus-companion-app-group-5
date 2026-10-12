// middleware/roles.js
// Runs AFTER middleware/auth.js (which sets req.user from the bearer token).
// Resolves the caller's effective level and ids, and gates by role.
//
//   lecturer      -> accounts.role = 'lecturer'            (full power, any group)
//   group_leader  -> students.is_group_leader = 1          (own group only)
//   student       -> everyone else
//
// Both elevated levels are toggled with a plain SQL UPDATE:
//   UPDATE accounts SET role='lecturer' ...        (lecturer)
//   UPDATE students SET is_group_leader=1 ...       (group leader)

const db = require('../config/db');

// Returns { level, accountId, role, studentId, lecturerId, groupId, programme }.
async function resolveActor(req) {
  const u = req.user;
  if (u.role === 'lecturer') {
    const [rows] = await db.query('SELECT lecturer_id FROM accounts WHERE account_id = ?', [u.account_id]);
    return {
      level: 'lecturer', accountId: u.account_id, role: 'lecturer',
      lecturerId: rows[0] ? rows[0].lecturer_id : null,
      studentId: null, groupId: null, programme: null,
    };
  }
  const [rows] = await db.query(
    'SELECT student_id, lab_group_id, programme_code, is_group_leader FROM students WHERE student_id = ? AND is_deleted = 0',
    [u.student_id]
  );
  const s = rows[0];
  const isLeader = !!s && Number(s.is_group_leader) === 1;
  return {
    level: isLeader ? 'group_leader' : 'student',
    accountId: u.account_id, role: 'student',
    lecturerId: null,
    studentId: u.student_id,
    groupId: s ? s.lab_group_id : null,
    programme: s ? s.programme_code : null,
  };
}

function gate(allowed, message) {
  return function (req, res, next) {
    resolveActor(req)
      .then((actor) => {
        if (!allowed.includes(actor.level)) {
          return res.status(403).json({ error: message });
        }
        req.actor = actor;
        return next();
      })
      .catch((err) => {
        console.error('roles middleware error:', err);
        return res.status(500).json({ error: 'Authorization failed' });
      });
  };
}

const requireLecturer = gate(['lecturer'], 'Lecturer access required');
const requireLeaderOrLecturer = gate(['lecturer', 'group_leader'], 'Group leader or lecturer access required');

module.exports = { resolveActor, requireLecturer, requireLeaderOrLecturer };
