// routes/lecturer.js — lecturer panel (Phase 2). All lecturer-only.
const express = require('express');
const auth = require('../middleware/auth');
const { requireLecturer } = require('../middleware/roles');
const c = require('../controllers/lecturerController');

const router = express.Router();
router.use(auth, requireLecturer);

router.get('/students', c.listStudents);
router.post('/students', c.createStudent);
router.patch('/students/:id', c.editStudent);
router.delete('/students/:id', c.softDeleteStudent);
router.post('/students/:id/assign', c.assignStudent);

router.get('/groups', c.listGroups);
router.post('/groups', c.createGroup);
router.delete('/groups/:id', c.deleteGroup);
router.get('/groups/:id/members', c.listGroupMembers);
router.post('/groups/:id/leader', c.setGroupLeader);
router.get('/groups/:id/export.xlsx', c.exportGroupXlsx);

router.get('/requests', c.listRequests);
router.post('/requests/:id/approve', c.approveRequest);
router.post('/requests/:id/reject', c.rejectRequest);

router.post('/announcements', c.postAnnouncement);

module.exports = router;
