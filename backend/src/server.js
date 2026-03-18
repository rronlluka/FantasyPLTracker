/**
 * server.js  —  Entry point for the FPL Tracker backend.
 *
 * Start with:  node src/server.js
 * Dev mode:    npm run dev   (uses nodemon for auto-restart)
 */

const express = require('express');
const cors    = require('cors');
const cron    = require('node-cron');
const { cachePrune } = require('./db');

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
    const cached = res.getHeader('X-From-Cache') === 'true' ? ' [CACHE]' : '';
    console.log(`${req.method} ${req.originalUrl} → ${res.statusCode} (${Date.now() - start}ms)${cached}`);
  });
  next();
});

// ── Routes ────────────────────────────────────────────────────────────────────
app.use('/api', fplRoutes);
app.use('/admin', adminRoutes);

// Health check
app.get('/health', (req, res) => res.json({ ok: true, ts: new Date().toISOString() }));

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
  console.log('║  GET  /admin/stats                            ║');
  console.log('║  GET  /admin/db-info                          ║');
  console.log('╚═══════════════════════════════════════════════╝');
  console.log('');
});
