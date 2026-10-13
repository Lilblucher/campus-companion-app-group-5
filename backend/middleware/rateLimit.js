const rateLimit = require('express-rate-limit');

const num = (v, d) => { const n = Number(v); return Number.isFinite(n) && n > 0 ? n : d; };

const make = (windowMin, max) =>
  rateLimit({
    windowMs: windowMin * 60 * 1000,
    max,
    standardHeaders: true,
    legacyHeaders: false,
    message: { error: 'Too many requests. Try again later.' },
  });

// Defaults unchanged; env-overridable for load tests / per-environment tuning.
exports.forgotLimiter = make(15, num(process.env.FORGOT_RATELIMIT_MAX, 5));  // code requests / 15 min
exports.resetLimiter  = make(15, num(process.env.RESET_RATELIMIT_MAX, 10)); // verify+reset attempts / 15 min