// backend/config/db.js
// Connects to MySQL using the .env settings.
// Exposes a shared connection pool plus helpers for queries,
// transactions (needed for the group-capacity rule) and shutdown.

require('dotenv').config();
const mysql = require('mysql2/promise');

// ---- 1. Fail fast if required environment variables are missing ----
const REQUIRED = ['DB_HOST', 'DB_PORT', 'DB_USER', 'DB_PASSWORD', 'DB_NAME'];
const missing = REQUIRED.filter((key) => !process.env[key]);

if (missing.length > 0) {
  console.error(
    `[db] Missing required environment variable(s): ${missing.join(', ')}\n` +
    '[db] Copy .env.example to .env and fill in your MySQL details.'
  );
  process.exit(1);
}

// ---- 2. Build the connection pool ----
const pool = mysql.createPool({
  host: process.env.DB_HOST,
  port: Number(process.env.DB_PORT),
  user: process.env.DB_USER,
  password: process.env.DB_PASSWORD,
  database: process.env.DB_NAME,

  // Pool behaviour
  waitForConnections: true,
  connectionLimit: Number(process.env.DB_POOL_SIZE) || 10,
  queueLimit: 0,

  // Character / time handling (supports accented names)
  charset: 'utf8mb4_general_ci',
  timezone: 'Z',

  // Safety: never allow stacked statements from one call
  multipleStatements: false,

  // Return DECIMAL/BIGINT safely
  decimalNumbers: true,
});

// ---- 3. Simple query helper (parameterised only) ----
/**
 * Run a parameterised SQL statement.
 * Always pass user input through `params` — never string-concatenate SQL.
 * Returns the standard mysql2/promise [rows, fields] tuple — destructure
 * with `const [rows] = await query(...)` at call sites.
 */
async function query(sql, params = []) {
  return pool.execute(sql, params);
}

// ---- 4. Transaction helper ----
/**
 * Run `work(connection)` inside a transaction with commit/rollback handling.
 * Use this for group assignment / transfer / deletion so the 15-member
 * limit holds even under simultaneous requests:
 *
 *   await withTransaction(async (conn) => {
 *     const [[group]] = await conn.execute(
 *       'SELECT group_id FROM groups WHERE group_id = ? FOR UPDATE',
 *       [groupId]
 *     );
 *     // ... count members, then INSERT ...
 *   });
 */
async function withTransaction(work) {
  const conn = await pool.getConnection();
  try {
    await conn.beginTransaction();
    const result = await work(conn);
    await conn.commit();
    return result;
  } catch (err) {
    try {
      await conn.rollback();
    } catch (rollbackErr) {
      console.error('[db] Rollback failed:', rollbackErr.message);
    }
    throw err;
  } finally {
    conn.release();
  }
}

// ---- 5. Startup connectivity check ----
async function checkConnection()  {
  const conn = await pool.getConnection();
  try {
    await conn.query('SELECT 1');
    const [[{ version, engine }]] = await conn.query(
      `SELECT VERSION() AS version,
              (SELECT ENGINE FROM information_schema.TABLES
                WHERE TABLE_SCHEMA = ? AND TABLE_NAME = 'students'
                LIMIT 1) AS engine`,
      [process.env.DB_NAME]
    );
    console.log(`[db] Connected to MySQL ${version} (db: ${process.env.DB_NAME})`);
    if (engine) console.log(`[db] students table engine: ${engine}`);
  } finally {
    conn.release();
  }
}
// Hand out a raw connection (caller must release it)
function getConnection() {
  return pool.getConnection();
}

// ---- 6. Graceful shutdown ----
async function closePool() {
  await pool.end();
  console.log('[db] Connection pool closed.');
}

module.exports = { pool, query, withTransaction, getConnection, checkConnection, closePool };