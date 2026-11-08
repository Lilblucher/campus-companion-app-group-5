const express = require('express');

const router = express.Router();

const auth = require('../middleware/auth');
const { requireLecturer } = require('../middleware/roles');
const c = require('../controllers/groupsController');

// Get all lab groups
router.get('/', (req, res) => {
    res.json({
        message: 'Get all groups route is working'
    });
});

router.get('/assignable-students', auth, requireLecturer, c.getAssignableStudents);
router.post('/:groupId/members', auth, requireLecturer, c.addMembers);


// Student requests to change lab group
router.post('/request', (req, res) => {
    res.json({
        message: 'Group change request route is working'
    });
});

// Lecturer assigns a student to a group
router.post('/assign', (req, res) => {
    res.json({
        message: 'Assign student to group route is working'
    });
});

// Lecturer transfers a student to another group
router.put('/transfer', (req, res) => {
    res.json({
        message: 'Transfer student between groups route is working'
    });
});

// Get one lab group by group ID
router.get('/:groupId', (req, res) => {
    res.json({
        message: 'Get group by ID route is working',
        groupId: req.params.groupId
    });
});

module.exports = router;