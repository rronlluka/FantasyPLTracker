/**
 * adminRoutes.js  —  /admin/* endpoints for inspecting the DB and cache.
 *
 * These are for development/debugging only.
 * Do NOT expose these in production without authentication.
 */

const express = require('express');
const router = express.Router();
const { getDbInfo, db } = require('../db');

// GET /admin/db-info
// Returns row counts and sample rows for every table, plus the DB file path.
router.get('/db-info', (req, res) => {
  try {
    res.json(getDbInfo());
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// GET /admin/cache
// Lists all non-expired cache entries (key + expiry)
router.get('/cache', (req, res) => {
  try {
    const now = Math.floor(Date.now() / 1000);
    const rows = db.prepare(
      'SELECT key, expires_at, length(value) as size_bytes FROM cache WHERE expires_at > ? ORDER BY key'
    ).all(now);
    res.json({ count: rows.length, entries: rows });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// DELETE /admin/cache/:key  — remove a specific cache entry
router.delete('/cache/:key', (req, res) => {
  try {
    db.prepare('DELETE FROM cache WHERE key = ?').run(req.params.key);
    res.json({ ok: true });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// DELETE /admin/cache  — flush entire cache
router.delete('/cache', (req, res) => {
  try {
    const result = db.prepare('DELETE FROM cache').run();
    res.json({ ok: true, deleted: result.changes });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// GET /admin/league-picks/:leagueId/:gameweek
// Shows all stored picks rows for a league+GW
router.get('/league-picks/:leagueId/:gameweek', (req, res) => {
  try {
    const rows = db.prepare(
      'SELECT manager_id, entry_name, rank, fetched_at FROM league_picks WHERE league_id = ? AND gameweek = ? ORDER BY rank'
    ).all(req.params.leagueId, req.params.gameweek);
    res.json({ count: rows.length, managers: rows });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// GET /admin/player-stats/:leagueId/:gameweek
// Lists all computed player stats for a league+GW
router.get('/player-stats/:leagueId/:gameweek', (req, res) => {
  try {
    const rows = db.prepare(
      `SELECT player_id, starts_count, bench_count, captain_count, starts_pct, owned_pct, computed_at
       FROM player_stats WHERE league_id = ? AND gameweek = ? ORDER BY owned_pct DESC`
    ).all(req.params.leagueId, req.params.gameweek);
    res.json({ count: rows.length, players: rows });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// GET /admin/stats  — overall server stats
router.get('/stats', (req, res) => {
  try {
    const cacheCount  = db.prepare('SELECT COUNT(*) as c FROM cache WHERE expires_at > ?').get(Math.floor(Date.now() / 1000)).c;
    const picksCount  = db.prepare('SELECT COUNT(*) as c FROM league_picks').get().c;
    const statsCount  = db.prepare('SELECT COUNT(*) as c FROM player_stats').get().c;
    const leaguesSeen = db.prepare('SELECT COUNT(DISTINCT league_id) as c FROM league_picks').get().c;
    res.json({
      activeCacheEntries: cacheCount,
      storedManagerPicks: picksCount,
      computedPlayerStats: statsCount,
      leaguesSeen,
      uptime: process.uptime(),
      memoryMB: Math.round(process.memoryUsage().heapUsed / 1024 / 1024),
    });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

module.exports = router;
