// controllers/authController.js
// Handles what happens when /api/auth/register and /api/auth/login are called.
// Two account types: "student" and "lecturer" (see database/schema.sql).

const bcrypt = require('bcryptjs');
const jwt = require('jsonwebtoken');
const db = require('../config/db');

const SALT_ROUNDS = 10;
const JWT_SECRET = process.env.JWT_SECRET;
const JWT_EXPIRES_IN = '2h';

// ---------------------------------------------------------------------------
// POST /api/auth/register  (students only — lecturer account is seeded)
// Body: { name, student_number, programme_code, password }
//
// A lecturer pre-creates the student's profile row. Registering here links a
// new account to that existing profile (per spec: "Never create a second
// profile"). If no profile exists, or the profile already has an account,
// registration is rejected.
// ---------------------------------------------------------------------------
async function register(req, res) {
  const { name, student_number, programme_code, password } = req.body;

  // --- basic validation (server is the source of truth; app also validates) ---
  if (!name || name.trim().length < 2 || name.trim().length > 100) {
    return res.status(400).json({ error: 'name must be 2-100 characters' });
  }

  const trimmedNumber = (student_number || '').trim();
  if (!/^\d{9}$/.test(trimmedNumber)) {
    return res.status(400).json({ error: 'student_number must be exactly 9 digits' });
  }

  if (!['CS', 'IT', 'DS'].includes(programme_code)) {
    return res.status(400).json({ error: 'programme_code must be CS, IT or DS' });
  }

  if (!password || password.length < 8) {
    return res.status(400).json({ error: 'password must be at least 8 characters' });
  }

  const conn = await db.getConnection();
  try {
    await conn.beginTransaction();

    // Find the profile the lecturer already created for this student.
    const [existing] = await conn.query(
      'SELECT student_id FROM students WHERE student_number = ? AND is_deleted = 0',
      [trimmedNumber]
    );

    if (existing.length === 0) {
      await conn.rollback();
      return res.status(400).json({ error: 'No student profile found for this student number' });
    }

    const studentId = existing[0].student_id;

    // Stop a second registration against the same profile.
    const [taken] = await conn.query(
      'SELECT account_id FROM accounts WHERE student_id = ?',
      [studentId]
    );
    if (taken.length > 0) {
      await conn.rollback();
      return res.status(409).json({ error: 'student_number already registered' });
    }

    const passwordHash = await bcrypt.hash(password, SALT_ROUNDS);

    await conn.query(
      `UPDATE students
       SET name = ?, programme_code = ?, status = 'active'
       WHERE student_id = ?`,
      [name.trim(), programme_code, studentId]
    );

    await conn.query(
      `INSERT INTO accounts (username, student_id, role, password_hash)
       VALUES (?, ?, 'student', ?)`,
      [trimmedNumber, studentId, passwordHash]
    );

    await conn.commit();
    return res.status(201).json({ message: 'Registered successfully. Please sign in.' });
  } catch (err) {
    await conn.rollback();
    if (err.code === 'ER_DUP_ENTRY') {
      return res.status(409).json({ error: 'student_number already registered' });
    }
    console.error('register error:', err);
    return res.status(500).json({ error: 'Registration failed' });
  } finally {
    conn.release();
  }
}

// ---------------------------------------------------------------------------
// POST /api/auth/login
// Body: { student_number OR username, password }
// Works for both students and the seeded lecturer account.
// ---------------------------------------------------------------------------
async function login(req, res) {
  const { student_number, username, password } = req.body;

  if (!password || (!student_number && !username)) {
    return res.status(400).json({ error: 'Missing credentials' });
  }

  try {
    let account;

    if (student_number) {
      const [rows] = await db.query(
        `SELECT a.account_id, a.password_hash, a.role, s.student_id, s.name
         FROM accounts a
         JOIN students s ON s.student_id = a.student_id
         WHERE s.student_number = ? AND s.is_deleted = 0 AND a.is_locked = 0`,
        [student_number.trim()]
      );
      account = rows[0];
    } else {
      const [rows] = await db.query(
        `SELECT account_id, password_hash, role, username
         FROM accounts
         WHERE username = ? AND role = 'lecturer' AND is_locked = 0`,
        [username.trim()]
      );
      account = rows[0];
    }

    if (!account) {
      return res.status(401).json({ error: 'Invalid credentials' });
    }

    const match = await bcrypt.compare(password, account.password_hash);
    if (!match) {
      return res.status(401).json({ error: 'Invalid credentials' });
    }

    const payload = {
      account_id: account.account_id,
      role: account.role,
      student_id: account.student_id || null,
    };

    const token = jwt.sign(payload, JWT_SECRET, { expiresIn: JWT_EXPIRES_IN });

    return res.json({
      token,
      role: account.role,
      student_id: account.student_id || null,
      name: account.name || account.username,
    });
  } catch (err) {
    console.error('login error:', err);
    return res.status(500).json({ error: 'Login failed' });
  }
}

// ---------------------------------------------------------------------------
// POST /api/auth/logout
// Stateless JWT: real invalidation needs a token blocklist table if required
// later. For now this just gives the app a clean endpoint to call.
// ---------------------------------------------------------------------------
async function logout(req, res) {
  return res.json({ message: 'Logged out' });
}

module.exports = { register, login, logout };
