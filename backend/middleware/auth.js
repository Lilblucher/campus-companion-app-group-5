// middleware/auth.js
// Verifies the login JWT issued by authController.login and sets req.user.
// Usage in any route file:  const auth = require('../middleware/auth');

const jwt = require('jsonwebtoken');
const db = require('../config/db');

async function authMiddleware(req, res, next) {
  // 1. Authorization: Bearer <token>
  const parts = (req.headers.authorization || '').trim().split(/\s+/);
  if (parts.length !== 2 || parts[0] !== 'Bearer' || !parts[1]) {
    return res.status(401).json({ error: 'Missing or malformed Authorization header' });
  }

  const secret = process.env.JWT_SECRET;
  if (!secret) {
    console.error('auth middleware: JWT_SECRET is not set in .env');
    return res.status(500).json({ error: 'Server configuration error' });
  }

  // 2. Verify signature + expiry (authController signs with the default HS256)
  let decoded;
  try {
    decoded = jwt.verify(parts[1], secret, { algorithms: ['HS256'] });
  } catch (err) {
    const msg = err.name === 'TokenExpiredError' ? 'Token expired' : 'Invalid token';
    return res.status(401).json({ error: msg });
  }

  // 3. Reject non-login tokens (e.g. password-reset tokens carry a `purpose`)
  if (decoded.purpose) {
    return res.status(401).json({ error: 'Invalid token' });
  }
  if (!decoded.account_id || !decoded.role) {
    return res.status(401).json({ error: 'Invalid token' });
  }

  // 4. Locked / removed accounts lose access immediately, not after 2 hours
  try {
    const [rows] = await db.query(
      'SELECT is_locked FROM accounts WHERE account_id = ?',
      [decoded.account_id]
    );
    if (rows.length === 0 || rows[0].is_locked) {
      return res.status(401).json({ error: 'Account is locked or no longer exists' });
    }
  } catch (err) {
    console.error('auth middleware DB error:', err);
    return res.status(500).json({ error: 'Authentication failed' });
  }

  req.user = {
    account_id: decoded.account_id,
    role: decoded.role,
    student_id: decoded.student_id ?? null,
  };
  return next();
}

module.exports = authMiddleware;