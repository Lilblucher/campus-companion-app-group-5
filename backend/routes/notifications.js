// routes/notifications.js — Phase 3 (notifications)
const express = require('express');
const auth = require('../middleware/auth');
const {
  listNotifications, markAllRead, markOneRead,
} = require('../controllers/notificationController');

const router = express.Router();

// Any logged-in user (student or lecturer) — no role restriction.
router.use(auth);

router.get('/', listNotifications);
router.put('/read-all', markAllRead);
router.put('/:notification_id/read', markOneRead);

module.exports = router;
