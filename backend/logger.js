// logger.js
// Structured logger with daily rotating log files.

const fs   = require('fs');
const path = require('path');

// ── Config ─────────────────────────────────────────────────────
const LOG_DIR          = path.join(__dirname, 'logs');
const ROTATE_INTERVAL  = 24 * 60 * 60 * 1000; // 24 hours in ms

// ── Ensure logs/ directory exists ─────────────────────────────
if (!fs.existsSync(LOG_DIR)) {
  fs.mkdirSync(LOG_DIR, { recursive: true });
}


function timestamp() {
  return new Date().toISOString();
}

/** Returns the filename for today's log, e.g. logs/2026-09-22.log */
function todayLogFile() {
  const date = new Date().toISOString().slice(0, 10); // "YYYY-MM-DD"
  return path.join(LOG_DIR, `${date}.log`);
}

/** Append one line to today's rotating log file */
function writeToFile(line) {
  try {
    fs.appendFileSync(todayLogFile(), line + '\n', 'utf8');
  } catch (err) {
    // Never let a log write crash the server
    console.error('[logger] Failed to write log:', err.message);
  }
}

// ── ANSI colour helpers (console only, not written to file) ───
const C = {
  reset:  '\x1b[0m',
  bold:   '\x1b[1m',
  dim:    '\x1b[2m',
  green:  '\x1b[32m',
  yellow: '\x1b[33m',
  red:    '\x1b[31m',
  cyan:   '\x1b[36m',
  blue:   '\x1b[34m',
  magenta:'\x1b[35m',
};

/** Colour a string — stripped automatically when not a TTY */
function c(colour, str) {
  if (!process.stdout.isTTY) return str;
  return `${C[colour]}${str}${C.reset}`;
}

// ── Core write function ────────────────────────────────────────
/**
 * @param {'INFO'|'WARN'|'ERROR'|'AUTH'|'REQUEST'} level
 * @param {string} message
 * @param {object} [meta]  extra key/value pairs printed alongside the message
 */
function log(level, message, meta = {}) {
  const ts     = timestamp();
  const metaStr = Object.keys(meta).length
    ? '  ' + Object.entries(meta).map(([k, v]) => `${k}=${v}`).join('  ')
    : '';

  // ── File line (plain text, always written) ─────────────────
  const fileLine = `[${ts}] [${level.padEnd(7)}] ${message}${metaStr}`;
  writeToFile(fileLine);

  // ── Console line (coloured) ────────────────────────────────
  const levelColours = {
    INFO:    'cyan',
    WARN:    'yellow',
    ERROR:   'red',
    AUTH:    'green',
    REQUEST: 'dim',
  };
  const col       = levelColours[level] || 'reset';
  const tsStr     = c('dim',  `[${ts}]`);
  const lvlStr    = c(col,    `[${level.padEnd(7)}]`);
  const msgStr    = level === 'AUTH'  ? c('bold',  message)
                  : level === 'ERROR' ? c('red',   message)
                  : message;

  const consoleLine = `${tsStr} ${lvlStr} ${msgStr}${metaStr}`;

  if (level === 'ERROR') {
    console.error(consoleLine);
  } else {
    console.log(consoleLine);
  }
}

// ── Public API ─────────────────────────────────────────────────

/** General informational message */
function info(message, meta)  { log('INFO',  message, meta); }

/** Warning — something unexpected but non-fatal */
function warn(message, meta)  { log('WARN',  message, meta); }

/** Error — something failed */
function error(message, meta) { log('ERROR', message, meta); }

/**
 * Auth event — call this from authController.js for login/register/logout.
 *
 * event examples:
 *   'LOGIN_SUCCESS'   { student_id, name, role }
 *   'LOGIN_FAILED'    { email, reason }
 *   'REGISTER_SUCCESS'{ student_id, name, email }
 *   'REGISTER_FAILED' { email, reason }
 *   'LOGOUT'          { student_id }
 *
 * @param {string} event
 * @param {object} [meta]
 */
function auth(event, meta = {}) {
  // Build a readable label, e.g. "AUTH  LOGIN_SUCCESS"
  log('AUTH', event, meta);
}

/**
 * HTTP request logger — drop this in as Express middleware.
 * Skips noisy bot probes (/.env, /wp-admin, etc.) from the console
 * but still writes them to the log file so you have a record.
 */
const SKIP_CONSOLE_PATHS = /\.(env|php|xml|txt)|wp-|adminer|cpanel|roundcube|webmail|trpc/i;

function requestMiddleware(req, _res, next) {
  const ts     = timestamp();
  const line   = `[${ts}] [REQUEST] ${req.method} ${req.path}`;
  writeToFile(line); // always write to file

  if (!SKIP_CONSOLE_PATHS.test(req.path)) {
    const tsStr = c('dim', `[${ts}]`);
    const lvl   = c('dim', '[REQUEST]');
    const method = c('blue', req.method.padEnd(6));
    console.log(`${tsStr} ${lvl} ${method} ${req.path}`);
  }

  next();
}

// ── Daily rotation reminder ────────────────────────────────────
// Logs the filename at startup and every 24 h so you always know
// which file is active.
function logRotationNotice() {
  info(`Log file → ${todayLogFile()}`);
}
logRotationNotice();
setInterval(logRotationNotice, ROTATE_INTERVAL);

// ── Exports ────────────────────────────────────────────────────
module.exports = { info, warn, error, auth, requestMiddleware };