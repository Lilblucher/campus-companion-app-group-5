// routes/group.js — group-leader actions (lecturer may override via ?group_id=).
const express = require('express');
const auth = require('../middleware/auth');
const { requireLeaderOrLecturer } = require('../middleware/roles');
const c = require('../controllers/groupLeaderController');

const router = express.Router();
router.use(auth, requireLeaderOrLecturer);

router.get('/members', c.getMembers);
router.post('/members', c.addMember);
router.delete('/members/:student_id', c.removeMember);
router.get('/export.xlsx', c.exportXlsx);
router.post('/export/send-to-lecturer', c.sendToLecturer);

module.exports = router;
