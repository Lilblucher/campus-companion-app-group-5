const express = require('express');

const router = express.Router();

const  {login, register, logout} = require("../controllers/authController");

const { forgotPassword, resetPassword } = require('../controllers/passwordController');
const { forgotLimiter, resetLimiter } = require('../middleware/rateLimit');

router.post('/forgot-password', forgotLimiter, forgotPassword);
router.post('/reset-password', resetLimiter, resetPassword);


// Student registration
router.post('/register', register);

// Student or lecturer login
router.post('/login',  login);


//logout
router.post("/logout", logout);

module.exports = router;