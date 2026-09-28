const crypto = require('crypto');
const bcrypt = require('bcryptjs');
const nodemailer = require('nodemailer');
const db = require('../config/db');

const transporter = nodemailer.createTransport({
  host: process.env.SMTP_HOST,
  port: Number(process.env.SMTP_PORT || 587),
  secure: false,
  auth: { user: process.env.SMTP_USER, pass: process.env.SMTP_PASS },
});

const CODE_TTL_MIN = 10;
const MAX_ATTEMPTS = 5;
const RESEND_COOLDOWN_SEC = 60;
const MAX_CODES_PER_HOUR = 5;
const ALPHABET = 'ABCDEFGHJKMNPQRSTUVWXYZ23456789'; // no 0/O/1/I/L

const isEmail = (s) => typeof s === 'string' && s.length <= 100 && /^\S+@\S+\.\S+$/.test(s);
const normalizeCode = (c) => String(c || '').toUpperCase().replace(/[^A-Z0-9]/g, '');
const pretty = (c) => `${c.slice(0, 4)}-${c.slice(4)}`;

function generateCode() {
  let out = '';
  for (let i = 0; i < 8; i++) out += ALPHABET[crypto.randomInt(0, ALPHABET.length)];
  return out;
}

async function processForgot(email) {
  const [acc] = await db.query(
    `SELECT a.account_id FROM accounts a
     JOIN students s ON s.student_id = a.student_id
     WHERE s.email = ? AND s.is_deleted = 0 AND a.is_locked = 0 LIMIT 1`, [email]);
  if (!acc.length) return;

  const [recent] = await db.query(
    `SELECT
       COALESCE(SUM(created_at > DATE_SUB(NOW(), INTERVAL ${RESEND_COOLDOWN_SEC} SECOND)), 0) AS in_cooldown,
       COUNT(*) AS last_hour
     FROM password_resets
     WHERE email = ? AND created_at > DATE_SUB(NOW(), INTERVAL 1 HOUR)`, [email]);
  if (Number(recent[0].in_cooldown) > 0 || Number(recent[0].last_hour) >= MAX_CODES_PER_HOUR) return;

  const code = generateCode();
  const codeHash = await bcrypt.hash(code, 10);

  await db.query('UPDATE password_resets SET used = 1 WHERE email = ? AND used = 0', [email]);
  await db.query(
    `INSERT INTO password_resets (email, code_hash, expires_at)
     VALUES (?, ?, DATE_ADD(NOW(), INTERVAL ${CODE_TTL_MIN} MINUTE))`,
    [email, codeHash]);

  await transporter.sendMail({
    from: process.env.MAIL_FROM,
    to: email,
    subject: 'Your Campus Companion reset code',
    text: `Your password reset code is ${pretty(code)}\n\nIt expires in ${CODE_TTL_MIN} minutes. If you didn't request this, ignore this email.`,
  });
}

// POST /api/auth/forgot-password  { email }
exports.forgotPassword = async (req, res) => {
  const email = (req.body.email || '').trim().toLowerCase();
  if (!isEmail(email)) return res.status(400).json({ error: 'Valid email required' });

  // Respond first so timing can't reveal whether the account exists
  res.json({ message: 'If that email is registered, a code has been sent.' });
  processForgot(email).catch((err) => console.error('[forgotPassword]', err));
};

// POST /api/auth/reset-password  { email, code, new_password }
exports.resetPassword = async (req, res) => {
  const email = (req.body.email || '').trim().toLowerCase();
  const code = normalizeCode(req.body.code);
  const { new_password } = req.body;

  if (!isEmail(email) || code.length !== 8 || !new_password)
    return res.status(400).json({ error: 'email, 8-character code and new_password required' });
  if (typeof new_password !== 'string' || new_password.length < 8 || new_password.length > 72)
    return res.status(400).json({ error: 'Password must be 8 to 72 characters' });

  try {
    const [rows] = await db.query(
      `SELECT * FROM password_resets
       WHERE email = ? AND used = 0 AND expires_at > NOW()
       ORDER BY reset_id DESC LIMIT 1`, [email]);
    const reset = rows[0];
    if (!reset) return res.status(400).json({ error: 'Invalid or expired code' });

    if (reset.attempts >= MAX_ATTEMPTS) {
      await db.query('UPDATE password_resets SET used = 1 WHERE reset_id = ?', [reset.reset_id]);
      return res.status(429).json({ error: 'Too many attempts. Request a new code.' });
    }

    // Count the attempt before checking so parallel guesses can't dodge the limit
    await db.query('UPDATE password_resets SET attempts = attempts + 1 WHERE reset_id = ?', [reset.reset_id]);

    const ok = await bcrypt.compare(code, reset.code_hash);
    if (!ok) return res.status(400).json({ error: 'Invalid or expired code' });

    const hash = await bcrypt.hash(new_password, 10);
    await db.query(
      `UPDATE accounts a JOIN students s ON s.student_id = a.student_id
       SET a.password_hash = ?
       WHERE s.email = ? AND s.is_deleted = 0 AND a.is_locked = 0`, [hash, email]);
    await db.query('UPDATE password_resets SET used = 1 WHERE email = ?', [email]);

    return res.json({ message: 'Password updated' });
  } catch (err) {
    console.error('[resetPassword]', err);
    return res.status(500).json({ error: 'Could not reset password' });
  }
};