// controllers/authController.js
// Handles what happens when /api/auth/register and /api/auth/login are called.
// Two account types: "student" and "lecturer" (see database/schema.sql + seed.sql).

const bcrypt = require('bcrypt');
const jwt = require('jsonwebtoken');
const db = require('../config/db');

const SALT_ROUNDS = 10;
const JWT_SECRET = process.env.JWT_SECRET;
const JWT_EXPIRES_IN = '2h';

// ---------------------------------------------------------------------------
// POST /api/auth/register  (students only — lecturer account is seeded)
// Body: { student_name, student_number, program_of_study, claim_code, password }
// ---------------------------------------------------------------------------
async function register(req, res) {
  const { student_name, student_number, program_of_study, claim_code, password } = req.body;

  // --- basic validation (server is the source of truth; app also validates) ---
  if (!student_name || student_name.trim().length < 2 || student_name.trim().length > 100) {
    return res.status(400).json({ error: 'student_name must be 2-100 characters' });
  }

  const trimmedNumber = (student_number || '').trim();
  if (!/^\d{9}$/.test(trimmedNumber)) {
    return res.status(400).json({ error: 'student_number must be exactly 9 digits' });
  }

  if (!['CS', 'IT', 'DS'].includes(program_of_study)) {
    return res.status(400).json({ error: 'program_of_study must be CS, IT or DS' });
  }

  if (!password || password.length < 8) {
    return res.status(400).json({ error: 'password must be at least 8 characters' });
  }

  const conn = await db.getConnection();
  try {
    await conn.beginTransaction();

    // Claim code proves the person registering owns that student number.
    const [claims] = await conn.query(
      'SELECT * FROM claim_codes WHERE student_number = ? AND code = ? AND used = 0',
      [trimmedNumber, claim_code]
    );
    if (claims.length === 0) {
      await conn.rollback();
      return res.status(400).json({ error: 'Invalid or already-used claim code' });
    }

    // If the lecturer already created this student's profile, link to it
    // instead of creating a duplicate (per spec: "Never create a second profile").
    const [existing] = await conn.query(
      'SELECT * FROM students WHERE student_number = ? AND deleted_at IS NULL',
      [trimmedNumber]
    );

    const passwordHash = await bcrypt.hash(password, SALT_ROUNDS);
    let studentId;

    if (existing.length > 0) {
      studentId = existing[0].student_id;
      await conn.query(
        'UPDATE students SET student_name = ?, program_of_study = ? WHERE student_id = ?',
        [student_name.trim(), program_of_study, studentId]
      );
    } else {
      const [result] = await conn.query(
        `INSERT INTO students (student_number, student_name, program_of_study, lab_group)
         VALUES (?, ?, ?, NULL)`,
        [trimmedNumber, student_name.trim(), program_of_study]
      );
      studentId = result.insertId;
    }

    await conn.query(
      `INSERT INTO accounts (student_id, role, password_hash)
       VALUES (?, 'student', ?)`,
      [studentId, passwordHash]
    );

    await conn.query('UPDATE claim_codes SET used = 1 WHERE id = ?', [claims[0].id]);

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
        `SELECT a.account_id, a.password_hash, a.role, s.student_id, s.student_name
         FROM accounts a
         JOIN students s ON s.student_id = a.student_id
         WHERE s.student_number = ? AND s.deleted_at IS NULL AND a.is_active = 0`,
        [student_number.trim()]
      );
      account = rows[0];
    } else {
      const [rows] = await db.query(
        `SELECT account_id, password_hash, role, username
         FROM accounts
         WHERE username = ? AND role = 'lecturer' AND is_active = 0`,
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
      name: account.student_name || account.username,
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
  return res.json({ message: 'Logged out. Please discard the token on the device.' });
}

module.exports = { register, login, logout };
