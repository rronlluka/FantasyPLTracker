/**
 * adminRoutes.js  —  /admin/* endpoints for inspecting the DB and cache.
 *
 * These are for development/debugging only.
 * Do NOT expose these in production without authentication.
 */

const express = require('express');
const router = express.Router();
const { getDbInfo, db, listActiveCache, deleteCacheKey, deleteAllCache, getLeagueSnapshot } = require('../storage');
const { getMetrics } = require('../metrics');
const { sendApiSuccess, sendApiError } = require('../response');

// GET /admin/db-info
// Returns row counts and sample rows for every table, plus the DB file path.
router.get('/db-info', (req, res) => {
  try {
    sendApiSuccess(res, getDbInfo());
  } catch (err) {
    sendApiError(res, 500, 'db_info_failed', err.message);
  }
});

// GET /admin/cache
// Lists all non-expired cache entries (key + expiry)
router.get('/cache', (req, res) => {
  try {
    const rows = listActiveCache();
    sendApiSuccess(res, { count: rows.length, entries: rows });
  } catch (err) {
    sendApiError(res, 500, 'cache_list_failed', err.message);
  }
});

// DELETE /admin/cache/:key  — remove a specific cache entry
router.delete('/cache/:key', (req, res) => {
  try {
    deleteCacheKey(req.params.key);
    sendApiSuccess(res, { ok: true });
  } catch (err) {
    sendApiError(res, 500, 'cache_delete_failed', err.message);
  }
});

// DELETE /admin/cache  — flush entire cache
router.delete('/cache', (req, res) => {
  try {
    const result = deleteAllCache();
    sendApiSuccess(res, { ok: true, deleted: result.changes });
  } catch (err) {
    sendApiError(res, 500, 'cache_flush_failed', err.message);
  }
});

// GET /admin/league-picks/:leagueId/:gameweek
// Shows all stored picks rows for a league+GW
router.get('/league-picks/:leagueId/:gameweek', (req, res) => {
  try {
    const rows = db.prepare(
      'SELECT manager_id, entry_name, rank, fetched_at FROM league_picks WHERE league_id = ? AND gameweek = ? ORDER BY rank'
    ).all(req.params.leagueId, req.params.gameweek);
    sendApiSuccess(res, {
      count: rows.length,
      snapshot: getLeagueSnapshot(req.params.leagueId, req.params.gameweek),
      managers: rows,
    });
  } catch (err) {
    sendApiError(res, 500, 'league_picks_failed', err.message);
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
    sendApiSuccess(res, { count: rows.length, players: rows });
  } catch (err) {
    sendApiError(res, 500, 'player_stats_list_failed', err.message);
  }
});

// GET /admin/stats  — overall server stats
router.get('/stats', (req, res) => {
  try {
    const cacheCount  = db.prepare('SELECT COUNT(*) as c FROM cache WHERE expires_at > ?').get(Math.floor(Date.now() / 1000)).c;
    const picksCount  = db.prepare('SELECT COUNT(*) as c FROM league_picks').get().c;
    const statsCount  = db.prepare('SELECT COUNT(*) as c FROM player_stats').get().c;
    const snapshotCount = db.prepare('SELECT COUNT(*) as c FROM league_snapshots').get().c;
    const leaguesSeen = db.prepare('SELECT COUNT(DISTINCT league_id) as c FROM league_picks').get().c;
    sendApiSuccess(res, {
      activeCacheEntries: cacheCount,
      storedManagerPicks: picksCount,
      storedSnapshots: snapshotCount,
      computedPlayerStats: statsCount,
      leaguesSeen,
      metrics: getMetrics(),
    });
  } catch (err) {
    sendApiError(res, 500, 'admin_stats_failed', err.message);
  }
});

module.exports = router;
