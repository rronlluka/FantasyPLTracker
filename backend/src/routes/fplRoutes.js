/**
 * fplRoutes.js  —  All /api/* endpoints exposed to the Android app.
 *
 * Proxy endpoints (bootstrap, fixtures, live, manager, player detail, transfers)
 * just cache the FPL response and return it.
 *
 * League/player stats endpoints use the pre-fetched picks cache.
 */

const express = require('express');
const router = express.Router();

const fpl = require('../fpl');
const { TTL, withCache } = require('../cache');
const { ensureLeaguePicks, computePlayerStats } = require('../leaguePicks');

// ── Bootstrap ─────────────────────────────────────────────────────────────────
// GET /api/bootstrap-static/
router.get('/bootstrap-static/', async (req, res) => {
  try {
    const { data, fromCache } = await withCache('bootstrap', TTL.BOOTSTRAP, fpl.getBootstrap);
    res.set('X-From-Cache', String(fromCache));
    res.json(data);
  } catch (err) {
    console.error('[bootstrap]', err.message);
    res.status(502).json({ error: err.message });
  }
});

// ── Fixtures ─────────────────────────────────────────────────────────────────
// GET /api/fixtures/          — all fixtures
// GET /api/fixtures/?event=28 — fixtures for a specific GW
router.get('/fixtures/', async (req, res) => {
  const eventId = req.query.event ? parseInt(req.query.event) : null;
  const key = eventId != null ? `fixtures:gw${eventId}` : 'fixtures:all';
  try {
    const { data, fromCache } = await withCache(key, TTL.FIXTURES, () => fpl.getFixtures(eventId));
    res.set('X-From-Cache', String(fromCache));
    res.json(data);
  } catch (err) {
    console.error('[fixtures]', err.message);
    res.status(502).json({ error: err.message });
  }
});

// ── Live gameweek ─────────────────────────────────────────────────────────────
// GET /api/event/:eventId/live/
router.get('/event/:eventId/live/', async (req, res) => {
  const eventId = parseInt(req.params.eventId);
  const key = `live:gw${eventId}`;
  try {
    // Use shorter TTL if this is likely the current active GW
    const { data, fromCache } = await withCache(key, TTL.LIVE_ACTIVE, () => fpl.getLiveGameweek(eventId));
    res.set('X-From-Cache', String(fromCache));
    res.json(data);
  } catch (err) {
    console.error('[live]', err.message);
    res.status(502).json({ error: err.message });
  }
});

// ── League standings ──────────────────────────────────────────────────────────
// GET /api/leagues-classic/:leagueId/standings/
// Query params: page_standings, event (optional GW for historical view)
router.get('/leagues-classic/:leagueId/standings/', async (req, res) => {
  const leagueId = parseInt(req.params.leagueId);
  const page = parseInt(req.query.page_standings ?? '1');
  const eventId = req.query.event ? parseInt(req.query.event) : null;
  const key = `league:${leagueId}:standings:p${page}${eventId != null ? `:gw${eventId}` : ''}`;
  try {
    const { data, fromCache } = await withCache(
      key,
      TTL.LEAGUE_STANDINGS,
      () => fpl.getLeagueStandings(leagueId, page, eventId)
    );
    res.set('X-From-Cache', String(fromCache));
    res.json(data);
  } catch (err) {
    console.error('[league standings]', err.message);
    res.status(502).json({ error: err.message });
  }
});

// ── Manager data ──────────────────────────────────────────────────────────────
// GET /api/entry/:managerId/
router.get('/entry/:managerId/', async (req, res) => {
  const managerId = parseInt(req.params.managerId);
  const key = `manager:${managerId}`;
  try {
    const { data, fromCache } = await withCache(key, TTL.MANAGER_DATA, () => fpl.getManagerData(managerId));
    res.set('X-From-Cache', String(fromCache));
    res.json(data);
  } catch (err) {
    console.error('[manager data]', err.message);
    res.status(502).json({ error: err.message });
  }
});

// ── Manager history ───────────────────────────────────────────────────────────
// GET /api/entry/:managerId/history/
router.get('/entry/:managerId/history/', async (req, res) => {
  const managerId = parseInt(req.params.managerId);
  const key = `manager:${managerId}:history`;
  try {
    const { data, fromCache } = await withCache(key, TTL.MANAGER_HISTORY, () => fpl.getManagerHistory(managerId));
    res.set('X-From-Cache', String(fromCache));
    res.json(data);
  } catch (err) {
    console.error('[manager history]', err.message);
    res.status(502).json({ error: err.message });
  }
});

// ── Manager picks ─────────────────────────────────────────────────────────────
// GET /api/entry/:managerId/event/:eventId/picks/
router.get('/entry/:managerId/event/:eventId/picks/', async (req, res) => {
  const managerId = parseInt(req.params.managerId);
  const eventId   = parseInt(req.params.eventId);
  const key = `manager:${managerId}:picks:gw${eventId}`;
  try {
    const { data, fromCache } = await withCache(
      key,
      TTL.MANAGER_PICKS,
      () => fpl.getManagerPicks(managerId, eventId)
    );
    res.set('X-From-Cache', String(fromCache));
    res.json(data);
  } catch (err) {
    console.error('[manager picks]', err.message);
    res.status(502).json({ error: err.message });
  }
});

// ── Manager transfers ─────────────────────────────────────────────────────────
// GET /api/entry/:managerId/transfers/
router.get('/entry/:managerId/transfers/', async (req, res) => {
  const managerId = parseInt(req.params.managerId);
  const key = `manager:${managerId}:transfers`;
  try {
    const { data, fromCache } = await withCache(
      key,
      TTL.MANAGER_TRANSFERS,
      () => fpl.getManagerTransfers(managerId)
    );
    res.set('X-From-Cache', String(fromCache));
    res.json(data);
  } catch (err) {
    console.error('[manager transfers]', err.message);
    res.status(502).json({ error: err.message });
  }
});

// ── Player detail ─────────────────────────────────────────────────────────────
// GET /api/element-summary/:elementId/
router.get('/element-summary/:elementId/', async (req, res) => {
  const elementId = parseInt(req.params.elementId);
  const key = `player:${elementId}:detail`;
  try {
    const { data, fromCache } = await withCache(
      key,
      TTL.PLAYER_DETAIL,
      () => fpl.getPlayerDetail(elementId)
    );
    res.set('X-From-Cache', String(fromCache));
    res.json(data);
  } catch (err) {
    console.error('[player detail]', err.message);
    res.status(502).json({ error: err.message });
  }
});

// ── League player stats (THE KEY ENDPOINT) ────────────────────────────────────
//
// GET /api/league/:leagueId/gw/:gameweek/player/:playerId/stats
//
// This is the endpoint that replaces the 25 individual picks calls.
// The first call for a league+GW fetches all managers' picks once and caches them.
// Every subsequent call (any player, any user) is served from the DB instantly.
//
// Query param: ?refresh=true  — force a re-fetch of all picks (admin use)
//
router.get('/league/:leagueId/gw/:gameweek/player/:playerId/stats', async (req, res) => {
  const leagueId = parseInt(req.params.leagueId);
  const gameweek = parseInt(req.params.gameweek);
  const playerId = parseInt(req.params.playerId);
  const forceRefresh = req.query.refresh === 'true';

  if (isNaN(leagueId) || isNaN(gameweek) || isNaN(playerId)) {
    return res.status(400).json({ error: 'Invalid leagueId, gameweek, or playerId' });
  }

  try {
    // Step 1: Make sure all picks for this league+GW are in the DB
    const refreshed = await ensureLeaguePicks(leagueId, gameweek, forceRefresh);

    // Step 2: Compute stats from DB (no FPL calls)
    const stats = computePlayerStats(leagueId, gameweek, playerId);

    if (!stats) {
      return res.status(404).json({ error: 'No picks data found for this league/gameweek' });
    }

    res.json({
      ...stats,
      meta: {
        leagueId,
        gameweek,
        picksRefreshed: refreshed,
      },
    });
  } catch (err) {
    console.error('[player stats]', err.message);
    res.status(502).json({ error: err.message });
  }
});

// ── League picks refresh (admin/manual trigger) ───────────────────────────────
// POST /api/league/:leagueId/gw/:gameweek/refresh-picks
router.post('/league/:leagueId/gw/:gameweek/refresh-picks', async (req, res) => {
  const leagueId = parseInt(req.params.leagueId);
  const gameweek = parseInt(req.params.gameweek);
  try {
    await ensureLeaguePicks(leagueId, gameweek, true);
    const count = require('../db').countLeaguePicks(leagueId, gameweek);
    res.json({ ok: true, managersStored: count, leagueId, gameweek });
  } catch (err) {
    console.error('[refresh picks]', err.message);
    res.status(502).json({ error: err.message });
  }
});

module.exports = router;
