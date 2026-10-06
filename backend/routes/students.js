// routes/students.js — Phase 1 (student dashboard)
const express = require('express');
const auth = require('../middleware/auth');
const {
  studentOnly, getMe, updateMe, getMyGroup, requestGroupChange,
} = require('../controllers/studentController');

const router = express.Router();

// Every route: valid login token first, then student role (403 for lecturers)
router.use(auth, studentOnly);

router.get('/me', getMe);
router.put('/me', updateMe);
router.get('/me/group', getMyGroup);
router.post('/me/group-change-request', requestGroupChange);

// Existing placeholder, not part of Phase 1 — left as-is
router.post('/me/student-number-correction', (req, res) => {
  res.json({ message: 'Student number correction route is working' });
});

module.exports = router;