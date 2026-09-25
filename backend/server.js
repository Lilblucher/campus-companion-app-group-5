// server.js

require("dotenv").config(); // Load .env variables FIRST, before anything else

const logger  = require("./logger"); // structured logger + daily rotating log files
const express = require("express");
const cors = require("cors");
const helmet = require("helmet");
const db = require("./config/db"); // MySQL connection pool
const authRoutes = require("./routes/auth");
/*
const studentRoutes = require("./routes/students");
const groupRoutes = require("./routes/groups");
const syncRoutes = require("./routes/sync");
*/
// ── App setup ──────────────────────────────────────────────────
const app = express();
const PORT = process.env.PORT || 3000;

// ── Global middleware ──────────────────────────────────────────

// helmet() sets secure HTTP headers (e.g. hides that this is Express)
app.use(helmet());

// cors() allows the Android app (and any other client) to call this API.
// In production, replace "*" with your actual server URL.
app.use(
  cors({
    origin: process.env.CORS_ORIGIN || "*",
    methods: ["GET", "POST", "PUT", "PATCH", "DELETE"],
    allowedHeaders: ["Content-Type", "Authorization"],
  })
);

// Parse incoming JSON request bodies
app.use(express.json());

// Parse URL-encoded bodies (form submissions, if any)
app.use(express.urlencoded({ extended: true }));

// ── Request logger ─────────────────────────────────────────────
// Writes every request to the daily log file.
// On the console it suppresses noisy bot probes (/.env, /wp-admin, etc.)
// so real traffic stays readable. Works in both dev and production.
app.use(logger.requestMiddleware);

// ── Health check ───────────────────────────────────────────────
// GET /health — the Android app (or anyone) can ping this to check the server is up.
// No auth required.
app.get("/health", (_req, res) => {
  res.json({ status: "ok", timestamp: new Date().toISOString() });
});

// ── API routes ─────────────────────────────────────────────────
//
// All routes are prefixed with /api so there is never a clash with
// any static files or the health check above.
//
//  /api/auth      → register, login (public)
//  /api/students  → CRUD + search/filter (protected — lecturer or own record)
//  /api/groups    → list groups, assign/transfer students (protected)
//  /api/sync      → offline sync endpoint (protected)

app.use("/api/auth", authRoutes);
/*
app.use("/api/students", studentRoutes);
app.use("/api/groups", groupRoutes);
app.use("/api/sync", syncRoutes);
*/

// Catches any request that didn't match a route above.
app.use((_req, res) => {
  res.status(404).json({ error: "Route not found" });
});

// ── Global error handler ───────────────────────────────────────
// Express calls this whenever next(err) is called inside a route.
// Keeps error responses consistent across the whole API.
// eslint-disable-next-line no-unused-vars
app.use((err, _req, res, _next) => {
  logger.error(err.message, { stack: (err.stack || '').split('\n')[1]?.trim() });

  // Don't leak stack traces to clients in production
  const message =
    process.env.NODE_ENV === "production"
      ? "An unexpected error occurred"
      : err.message;

  res.status(err.status || 500).json({ error: message });
});

// ── Start ──────────────────────────────────────────────────────
// Test the database connection before accepting requests.
// This catches a wrong password or unreachable host early.
db.getConnection()
  .then((connection) => {
    connection.release(); // release back to pool immediately
    logger.info('MySQL connection pool is ready');

    app.listen(PORT, () => {
      logger.info(`Server running on port ${PORT}`);
      logger.info(`Health check  -> http://localhost:${PORT}/health`);
      logger.info(`Auth          -> http://localhost:${PORT}/api/auth`);
      logger.info(`Students      -> http://localhost:${PORT}/api/students`);
      logger.info(`Groups        -> http://localhost:${PORT}/api/groups`);
      logger.info(`Sync          -> http://localhost:${PORT}/api/sync`);
    });
  })
  .catch((err) => {
    logger.error('Failed to connect to MySQL: ' + err.message);
    logger.error('Check your .env file: DB_HOST, DB_USER, DB_PASSWORD, DB_NAME');
    process.exit(1);
  });

module.exports = app; // exported so the Testing team can import it in tests