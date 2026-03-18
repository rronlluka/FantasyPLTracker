/**
 * server.js  —  Entry point for the FPL Tracker backend.
 *
 * Start with:  node src/server.js
 * Dev mode:    npm run dev   (uses nodemon for auto-restart)
 */

const express = require('express');
const cors    = require('cors');
const cron    = require('node-cron');
const { cachePrune } = require('./storage');
const { recordRequest, getMetrics } = require('./metrics');
const { adminGuard, adminEnabled } = require('./adminGuard');
const { sendApiSuccess } = require('./response');

const fplRoutes   = require('./routes/fplRoutes');
const adminRoutes = require('./routes/adminRoutes');

const PORT = process.env.PORT || 3000;

const app = express();
app.use(cors());
app.use(express.json());

// ── Request logging ───────────────────────────────────────────────────────────
app.use((req, res, next) => {
  const start = Date.now();
  res.on('finish', () => {
    const durationMs = Date.now() - start;
    const cached = res.getHeader('X-From-Cache') === 'true';
    const logEntry = {
      method: req.method,
      path: req.originalUrl,
      status: res.statusCode,
      durationMs,
      cached,
    };
    recordRequest(req.route?.path || req.path, res.statusCode, durationMs);
    console.log(JSON.stringify(logEntry));
  });
  next();
});

// ── Routes ────────────────────────────────────────────────────────────────────
app.use('/api', fplRoutes);
app.use('/admin', adminGuard, adminRoutes);

// Health check
app.get('/health', (req, res) => sendApiSuccess(res, {
  ok: true,
  ts: new Date().toISOString(),
  uptimeSeconds: Math.round(process.uptime()),
  adminEnabled: adminEnabled(),
  metrics: getMetrics(),
}));

// ── Scheduled jobs ────────────────────────────────────────────────────────────
// Prune expired cache entries every 30 minutes
cron.schedule('*/30 * * * *', () => {
  console.log('[cron] Pruning expired cache entries…');
  cachePrune();
});

// ── Start ─────────────────────────────────────────────────────────────────────
app.listen(PORT, () => {
  console.log('');
  console.log('╔═══════════════════════════════════════════════╗');
  console.log('║       FPL Tracker Backend  —  RUNNING         ║');
  console.log(`║   http://localhost:${PORT}                       ║`);
  console.log('╠═══════════════════════════════════════════════╣');
  console.log('║  Key endpoints:                               ║');
  console.log('║  GET  /api/bootstrap-static/                  ║');
  console.log('║  GET  /api/league/:id/gw/:gw/player/:pid/stats║');
  console.log(`║  Admin enabled: ${String(adminEnabled()).padEnd(29, ' ')}║`);
  console.log('╚═══════════════════════════════════════════════╝');
  console.log('');
});
