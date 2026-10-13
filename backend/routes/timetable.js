// routes/timetable.js — read: any authenticated user; write: lecturer only.
const express = require('express');
const auth = require('../middleware/auth');
const { requireLecturer } = require('../middleware/roles');
const c = require('../controllers/timetableController');

const router = express.Router();

router.get('/', auth, c.listTimetable);
router.get('/next', auth, c.nextSession);
router.post('/', auth, requireLecturer, c.createSession);
router.patch('/:id', auth, requireLecturer, c.updateSession);
router.delete('/:id', auth, requireLecturer, c.deleteSession);

module.exports = router;
