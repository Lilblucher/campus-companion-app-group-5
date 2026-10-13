// controllers/passwordController.js — Phase 5 (password reset)
//
// Three PUBLIC endpoints (the user can't log in, so no JWT): forgot-password,
// verify-reset-code, reset-password. Codes are 6-digit numeric (crypto RNG),
// only their bcrypt hash is stored, 15-minute expiry, single-use.
//
// Two reset paths are supported on purpose:
//   * { email, code, new_password }        — the Android "Forgot Password"
//                                             screen's one-shot flow
//   * verify-reset-code -> { reset_token }  — the brief's token flow; the token
//     then resets via { reset_token, new_password }
// Never log codes, tokens or passwords.

const crypto = require('crypto');
const bcrypt = require('bcryptjs');            // same library authController uses
const jwt = require('jsonwebtoken');
const nodemailer = require('nodemailer');
const db = require('../config/db');

const transporter = nodemailer.createTransport({
  host: process.env.SMTP_HOST,
  port: Number(process.env.SMTP_PORT || 587),
  secure: false,
  auth: { user: process.env.SMTP_USER, pass: process.env.SMTP_PASS },
});

// One-time startup check so a bad SMTP config is obvious in the logs instead of
// failing silently the first time someone requests a reset code.
if (!process.env.SMTP_HOST || !process.env.SMTP_USER || !process.env.SMTP_PASS) {
  console.warn('[mail] SMTP is NOT fully configured (SMTP_HOST/SMTP_USER/SMTP_PASS) — reset codes will NOT be emailed.');
} else {
  transporter.verify()
    .then(() => console.log(`[mail] SMTP ready as ${process.env.SMTP_USER} via ${process.env.SMTP_HOST}:${process.env.SMTP_PORT || 587}`))
    .catch((e) => console.error('[mail] SMTP verify FAILED:', e.message));
}

const CODE_TTL_MIN = 15;            // brief: 15-minute expiry
const MAX_ATTEMPTS = 5;
const RESEND_COOLDOWN_SEC = 60;
const MAX_CODES_PER_HOUR = 5;
const RESET_TOKEN_TTL = '10m';      // short-lived reset token
const JWT_SECRET = process.env.JWT_SECRET;

const isEmail = (s) => typeof s === 'string' && s.length <= 100 && /^\S+@\S+\.\S+$/.test(s);
const normalizeCode = (c) => String(c == null ? '' : c).replace(/\D/g, ''); // digits only
const isSixDigit = (c) => /^\d{6}$/.test(c);

// 6-digit code, cryptographically secure, leading zeros preserved.
function generateCode() {
  return String(crypto.randomInt(0, 1000000)).padStart(6, '0');
}

// Look up the student's account by email (not logged in → email is the handle).
// Locked / soft-deleted students are excluded so they can't reset their way back in.
async function findActiveStudentAccount(email) {
  const [rows] = await db.query(
    `SELECT a.account_id FROM accounts a
       JOIN students s ON s.student_id = a.student_id
      WHERE s.email = ? AND s.is_deleted = 0 AND a.is_locked = 0
      LIMIT 1`,
    [email]
  );
  return rows[0] || null;
}

async function processForgot(email) {
  const acc = await findActiveStudentAccount(email);
  if (!acc) {
    // Not an error path for the user (we never reveal this), but logging it means
    // "I'm not getting a code" can be diagnosed instantly: wrong/unregistered email.
    console.warn(`[forgot] no active student account for email "${email}" — no code sent`);
    return;
  }

  const [recent] = await db.query(
    `SELECT
       COALESCE(SUM(created_at > DATE_SUB(NOW(), INTERVAL ${RESEND_COOLDOWN_SEC} SECOND)), 0) AS in_cooldown,
       COUNT(*) AS last_hour
     FROM password_resets
     WHERE email = ? AND created_at > DATE_SUB(NOW(), INTERVAL 1 HOUR)`,
    [email]
  );
  if (Number(recent[0].in_cooldown) > 0 || Number(recent[0].last_hour) >= MAX_CODES_PER_HOUR) {
    console.warn(`[forgot] rate-limited for "${email}" (wait up to ${RESEND_COOLDOWN_SEC}s between requests, max ${MAX_CODES_PER_HOUR}/hour)`);
    return;
  }

  const code = generateCode();
  const codeHash = await bcrypt.hash(code, 10);

  // Invalidate any earlier unused codes for this account, then store the new one.
  await db.query('UPDATE password_resets SET used = 1 WHERE email = ? AND used = 0', [email]);
  await db.query(
    `INSERT INTO password_resets (email, code_hash, expires_at)
     VALUES (?, ?, DATE_ADD(NOW(), INTERVAL ${CODE_TTL_MIN} MINUTE))`,
    [email, codeHash]
  );

  const info = await transporter.sendMail({
    from: process.env.MAIL_FROM,
    to: email,
    subject: 'Your Campus Companion reset code',
    text: `Your password reset code is ${code}\n\nIt expires in ${CODE_TTL_MIN} minutes. If you didn't request this, ignore this email.`,
  });
  console.log(`[forgot] reset code emailed to "${email}" (messageId ${info.messageId})`);
}

// Validate a submitted code against the latest unused, unexpired row.
// Counts the attempt before comparing so parallel guesses can't dodge the cap.
// Returns { ok:true, reset_id } or { ok:false, status, error }.
async function validateCode(email, code) {
  const [rows] = await db.query(
    `SELECT reset_id, code_hash, attempts FROM password_resets
      WHERE email = ? AND used = 0 AND expires_at > NOW()
      ORDER BY reset_id DESC LIMIT 1`,
    [email]
  );
  const reset = rows[0];
  if (!reset) return { ok: false, status: 400, error: 'Invalid or expired code' };

  if (reset.attempts >= MAX_ATTEMPTS) {
    await db.query('UPDATE password_resets SET used = 1 WHERE reset_id = ?', [reset.reset_id]);
    return { ok: false, status: 429, error: 'Too many attempts. Request a new code.' };
  }
  await db.query('UPDATE password_resets SET attempts = attempts + 1 WHERE reset_id = ?', [reset.reset_id]);

  const match = await bcrypt.compare(code, reset.code_hash);
  if (!match) return { ok: false, status: 400, error: 'Invalid or expired code' };
  return { ok: true, reset_id: reset.reset_id };
}

// POST /api/auth/forgot-password  { email }
// Always returns the same generic 200 so attackers can't enumerate emails.
exports.forgotPassword = async (req, res) => {
  const email = typeof req.body.email === 'string' ? req.body.email.trim().toLowerCase() : '';
  if (!isEmail(email)) return res.status(400).json({ error: 'Valid email required' });

  // Respond first so timing can't reveal whether the account exists.
  res.json({ message: 'If that email is registered, a code has been sent.' });
  processForgot(email).catch((err) => console.error('[forgotPassword]', err.message));
};

// POST /api/auth/verify-reset-code  { email, code }
// Right code → a short-lived reset token (purpose:'password_reset'); the login
// middleware rejects any token carrying a purpose, so it can't be used to log in.
exports.verifyResetCode = async (req, res) => {
  const email = typeof req.body.email === 'string' ? req.body.email.trim().toLowerCase() : '';
  const code = normalizeCode(req.body.code);
  if (!isEmail(email) || !isSixDigit(code)) {
    return res.status(400).json({ error: 'Valid email and 6-digit code required' });
  }

  try {
    const r = await validateCode(email, code);
    if (!r.ok) return res.status(r.status).json({ error: r.error });
    const reset_token = jwt.sign(
      { purpose: 'password_reset', email, rid: r.reset_id },
      JWT_SECRET,
      { expiresIn: RESET_TOKEN_TTL }
    );
    return res.json({ reset_token, expires_in_minutes: 10 });
  } catch (err) {
    console.error('[verifyResetCode]', err.message);
    return res.status(500).json({ error: 'Could not verify code' });
  }
};

// POST /api/auth/reset-password
//   { reset_token, new_password }        (token flow) OR
//   { email, code, new_password }        (one-shot code flow)
// Password rule mirrors registration (>= 8 chars); bcrypt caps at 72 bytes.
exports.resetPassword = async (req, res) => {
  const { reset_token, new_password } = req.body;

  if (typeof new_password !== 'string' || new_password.length < 8 || new_password.length > 72) {
    return res.status(400).json({ error: 'Password must be 8 to 72 characters' });
  }

  let email = '';
  let resetId = null;

  if (typeof reset_token === 'string' && reset_token) {
    // Token flow
    let decoded;
    try {
      decoded = jwt.verify(reset_token, JWT_SECRET);
    } catch (_) {
      return res.status(400).json({ error: 'Invalid or expired reset token' });
    }
    if (decoded.purpose !== 'password_reset' || !decoded.email) {
      return res.status(400).json({ error: 'Invalid reset token' });
    }
    email = String(decoded.email).toLowerCase();
    resetId = decoded.rid || null;
  } else {
    // One-shot code flow (the Android forgot-password screen)
    email = typeof req.body.email === 'string' ? req.body.email.trim().toLowerCase() : '';
    const code = normalizeCode(req.body.code);
    if (!isEmail(email) || !isSixDigit(code)) {
      return res.status(400).json({ error: 'email, 6-digit code and new_password required' });
    }
    const r = await validateCode(email, code);
    if (!r.ok) return res.status(r.status).json({ error: r.error });
    resetId = r.reset_id;
  }

  const conn = await db.getConnection();
  try {
    await conn.beginTransaction();

    // Single-use: atomically consume the reset row. 0 rows => already used or
    // expired (e.g. a replayed token/code), so the reset is rejected.
    let consumed;
    if (resetId !== null) {
      [consumed] = await conn.query(
        `UPDATE password_resets SET used = 1
          WHERE reset_id = ? AND email = ? AND used = 0 AND expires_at > NOW()`,
        [resetId, email]
      );
    } else {
      [consumed] = await conn.query(
        `UPDATE password_resets SET used = 1
          WHERE email = ? AND used = 0 AND expires_at > NOW()`,
        [email]
      );
    }
    if (consumed.affectedRows === 0) {
      await conn.rollback();
      return res.status(400).json({ error: 'Invalid or expired code' });
    }

    // Burn any other outstanding codes for this email too.
    await conn.query('UPDATE password_resets SET used = 1 WHERE email = ? AND used = 0', [email]);

    // Update only an active (non-locked, non-deleted) student's password.
    const hash = await bcrypt.hash(new_password, 10);
    const [upd] = await conn.query(
      `UPDATE accounts a JOIN students s ON s.student_id = a.student_id
          SET a.password_hash = ?
        WHERE s.email = ? AND s.is_deleted = 0 AND a.is_locked = 0`,
      [hash, email]
    );
    if (upd.affectedRows === 0) {
      // No eligible account (missing / locked / soft-deleted). Don't reveal which.
      await conn.rollback();
      return res.status(400).json({ error: 'Could not reset password' });
    }

    await conn.commit();
    return res.json({ message: 'Password updated' });
  } catch (err) {
    try { await conn.rollback(); } catch (_) { /* ignore */ }
    console.error('[resetPassword]', err.message);
    return res.status(500).json({ error: 'Could not reset password' });
  } finally {
    conn.release();
  }
};
