const rateLimit = require('express-rate-limit');

const make = (windowMin, max) =>
  rateLimit({
    windowMs: windowMin * 60 * 1000,
    max,
    standardHeaders: true,
    legacyHeaders: false,
    message: { error: 'Too many requests. Try again later.' },
  });

exports.forgotLimiter = make(15, 5);  // 5 code requests / 15 min
exports.resetLimiter  = make(15, 10); // 10 reset attempts / 15 min 