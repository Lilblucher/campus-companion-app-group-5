// controllers/notificationController.js — Phase 3 (notifications)
// Any logged-in user (student or lecturer). req.user.account_id is the owner.
// db.query resolves to [rows, fields] (mysql2), so always destructure [rows].

const db = require('../config/db');

// GET /api/notifications        (?unread=true for unread-only)
// Returns the caller's own notifications, newest first, max 50, plus unread_count.
async function listNotifications(req, res) {
  try {
    const unreadOnly = String(req.query.unread).toLowerCase() === 'true';
    const sql =
      `SELECT notification_id, type, title, body, is_read, created_at
         FROM notifications
        WHERE account_id = ?${unreadOnly ? ' AND is_read = 0' : ''}
        ORDER BY created_at DESC, notification_id DESC
        LIMIT 50`;
    const [rows] = await db.query(sql, [req.user.account_id]);
    const [[counts]] = await db.query(
      'SELECT COUNT(*) AS unread_count FROM notifications WHERE account_id = ? AND is_read = 0',
      [req.user.account_id]
    );
    return res.json({ notifications: rows, unread_count: Number(counts.unread_count) });
  } catch (err) {
    console.error('listNotifications error:', err);
    return res.status(500).json({ error: 'Failed to load notifications' });
  }
}

// PUT /api/notifications/read-all
async function markAllRead(req, res) {
  try {
    const [result] = await db.query(
      'UPDATE notifications SET is_read = 1 WHERE account_id = ? AND is_read = 0',
      [req.user.account_id]
    );
    return res.json({ updated: result.affectedRows });
  } catch (err) {
    console.error('markAllRead error:', err);
    return res.status(500).json({ error: 'Failed to update notifications' });
  }
}

// PUT /api/notifications/:notification_id/read
// 400 for a non-numeric id; 404 if it doesn't exist OR isn't the caller's
// (same 404 for both, so ownership isn't revealed).
async function markOneRead(req, res) {
  const raw = req.params.notification_id;
  if (!/^\d+$/.test(String(raw))) {
    return res.status(400).json({ error: 'notification_id must be a positive integer' });
  }
  const id = Number(raw);
  try {
    // Check ownership first so an already-read own row doesn't look like a 404
    // (MySQL's affectedRows counts changed rows, not matched ones).
    const [owned] = await db.query(
      'SELECT notification_id FROM notifications WHERE notification_id = ? AND account_id = ?',
      [id, req.user.account_id]
    );
    if (owned.length === 0) {
      return res.status(404).json({ error: 'Notification not found' });
    }
    await db.query('UPDATE notifications SET is_read = 1 WHERE notification_id = ?', [id]);
    return res.json({ notification_id: id, is_read: 1 });
  } catch (err) {
    console.error('markOneRead error:', err);
    return res.status(500).json({ error: 'Failed to update notification' });
  }
}

module.exports = { listNotifications, markAllRead, markOneRead };
