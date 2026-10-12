// controllers/authController.js
// Handles what happens when /api/auth/register and /api/auth/login are called.
// Two account types: "student" and "lecturer" (see database/schema.sql).

const bcrypt = require('bcryptjs');
const jwt = require('jsonwebtoken');
const db = require('../config/db');

const SALT_ROUNDS = 10;
const JWT_SECRET = process.env.JWT_SECRET;
const JWT_EXPIRES_IN = '2h';

const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
const PHONE_RE = /^\+?\d{7,15}$/;
const CLAIM_CODE_RE = /^[A-Za-z0-9-]{3,20}$/;

// ---------------------------------------------------------------------------
// POST /api/auth/register  (students only — lecturer account is seeded)
// Body: { name, student_number, programme_code, email, phone, claim_code?, password }
// claim_code is OPTIONAL: if omitted or blank the student registers normally and
// the claim columns stay NULL / claim_code_used = 0.
//
// Open self-registration: creates the student's profile row and the linked
// account in one transaction. The claim_code is supplied by the student
// (given to them out-of-band, e.g. by their lecturer) and is recorded on the
// new profile row as already used, since it's consumed at the moment the
// profile is created — see the claim_code / claim_code_used / claimed_at /
// claimed_by columns on students in schema.sql. New students start with
// lab_group_id = NULL and status = 'unassigned' (the schema default) until
// a lecturer assigns them to a lab group.
// ---------------------------------------------------------------------------
async function register(req, res) {
  const { name, student_number, programme_code, email, phone, claim_code, password } = req.body;

  // --- basic validation (server is the source of truth; app also validates) ---
  // Type-guard every field: a JSON number (e.g. "name": 123) would make
  // `.trim()` throw, and an unguarded throw here crashes the whole process
  // (Node exits on the unhandled rejection), not just this request.
  if (typeof name !== 'string' || name.trim().length < 2 || name.trim().length > 100) {
    return res.status(400).json({ error: 'name must be 2-100 characters' });
  }

  // Must be a string: a JSON number can't preserve leading zeros (schema is CHAR(9)).
  const trimmedNumber = typeof student_number === 'string' ? student_number.trim() : '';
  if (!/^\d{9}$/.test(trimmedNumber)) {
    return res.status(400).json({ error: 'student_number must be exactly 9 digits' });
  }

  if (!['CS', 'IT', 'DS'].includes(programme_code)) {
    return res.status(400).json({ error: 'programme_code must be CS, IT or DS' });
  }

  const trimmedEmail = (typeof email === 'string' ? email : '').trim().toLowerCase();
  if (!EMAIL_RE.test(trimmedEmail)) {
    return res.status(400).json({ error: 'email must be a valid email address' });
  }

  const trimmedPhone = (typeof phone === 'string' ? phone : '').trim();
  if (!PHONE_RE.test(trimmedPhone)) {
    return res.status(400).json({ error: 'phone must be 7-15 digits, optionally starting with +' });
  }

  // claim_code is OPTIONAL. Omitted/blank → stored as NULL (never '' —
  // UNIQUE(claim_code) would make a second blank value collide).
  const rawClaimCode = claim_code == null ? '' : String(claim_code).trim();
  const trimmedClaimCode = rawClaimCode === '' ? null : rawClaimCode;
  if (trimmedClaimCode !== null && !CLAIM_CODE_RE.test(trimmedClaimCode)) {
    return res.status(400).json({ error: 'claim_code must be 3-20 letters, digits or hyphens' });
  }
  const hasClaimCode = trimmedClaimCode !== null;

  if (typeof password !== 'string' || password.length < 8) {
    return res.status(400).json({ error: 'password must be at least 8 characters' });
  }

  const conn = await db.getConnection();
  try {
    await conn.beginTransaction();

    // Create the student's profile row. UNIQUE(student_number),
    // UNIQUE(email), UNIQUE(claim_code), and the programme_code FK enforce
    // validity without a separate lookup here. The code is claimed
    // immediately, since this row (and its account) is what claims it.
    const [insertResult] = await conn.query(
      `INSERT INTO students
         (student_number, name, programme_code, email, phone,
          claim_code, claim_code_used, claimed_at)
       VALUES (?, ?, ?, ?, ?, ?, ?, ${hasClaimCode ? 'NOW()' : 'NULL'})`,
      [trimmedNumber, name.trim(), programme_code, trimmedEmail, trimmedPhone,
       trimmedClaimCode, hasClaimCode ? 1 : 0]
    );

    const studentId = insertResult.insertId;
    const passwordHash = await bcrypt.hash(password, SALT_ROUNDS);

    const [accountResult] = await conn.query(
      `INSERT INTO accounts (username, student_id, role, password_hash)
       VALUES (?, ?, 'student', ?)`,
      [trimmedNumber, studentId, passwordHash]
    );

    // Record which account claimed the code, now that it exists.
    if (hasClaimCode) {
      await conn.query(
        `UPDATE students SET claimed_by = ? WHERE student_id = ?`,
        [accountResult.insertId, studentId]
      );
    }

    await conn.commit();
    return res.status(201).json({ message: 'Registered successfully. Please sign in.' });
  } catch (err) {
    await conn.rollback();
    if (err.code === 'ER_DUP_ENTRY') {
      const msg = err.sqlMessage || '';
      if (msg.includes('email')) {
        return res.status(409).json({ error: 'email already registered' });
      }
      if (msg.includes('claim_code')) {
        return res.status(409).json({ error: 'claim_code already used' });
      }
      return res.status(409).json({ error: 'student_number already registered' });
    }
    if (err.code === 'ER_NO_REFERENCED_ROW' || err.code === 'ER_NO_REFERENCED_ROW_2') {
      return res.status(400).json({ error: 'Invalid programme_code' });
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
  // Normalise to strings up front so a numeric field can't crash `.trim()`.
  const sn = typeof student_number === 'string' ? student_number.trim() : '';
  const un = typeof username === 'string' ? username.trim() : '';

  if (typeof password !== 'string' || !password || (!sn && !un)) {
    return res.status(400).json({ error: 'Missing credentials' });
  }

  try {
    let account;

    if (sn) {
      const [rows] = await db.query(
        `SELECT a.account_id, a.password_hash, a.role, s.student_id, s.name, s.is_group_leader
         FROM accounts a
         JOIN students s ON s.student_id = a.student_id
         WHERE s.student_number = ? AND s.is_deleted = 0 AND a.is_locked = 0`,
        [sn]
      );
      account = rows[0];
    } else {
      const [rows] = await db.query(
        `SELECT account_id, password_hash, role, username
         FROM accounts
         WHERE username = ? AND role = 'lecturer' AND is_locked = 0`,
        [un]
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
      is_group_leader: account.is_group_leader ? 1 : 0,
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