// routes/sync.js — Phase 4 (offline sync)
const express = require('express');
const auth = require('../middleware/auth');
const { studentOnly } = require('../controllers/studentController');
const { push } = require('../controllers/syncController');

const router = express.Router();

// Every route: valid login token, student role only
router.use(auth, studentOnly);

// Push one offline operation from Android.
// POST /api/sync is the documented path; /push is kept because the old stub used it.
router.post(['/', '/push'], push);

// Pull changes from the server to Android — existing placeholder, not part of this task
router.get('/pull', (req, res) => {
  res.json({ message: 'Sync pull route is working' });
});

module.exports = router;