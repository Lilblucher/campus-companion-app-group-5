// routes/reminders.js — read: any authenticated user; write: leader or lecturer.
const express = require('express');
const auth = require('../middleware/auth');
const { requireLeaderOrLecturer } = require('../middleware/roles');
const c = require('../controllers/reminderController');

const router = express.Router();

router.get('/', auth, c.listReminders);
router.post('/', auth, requireLeaderOrLecturer, c.createReminder);
router.patch('/:id', auth, requireLeaderOrLecturer, c.updateReminder);
router.delete('/:id', auth, requireLeaderOrLecturer, c.deleteReminder);

module.exports = router;
