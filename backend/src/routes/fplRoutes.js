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
const { getLeagueSnapshot, countLeaguePicks } = require('../storage');
const { getStatsOverview } = require('../statsOverview');
const { sendApiSuccess, sendApiError } = require('../response');

// ── Bootstrap ─────────────────────────────────────────────────────────────────
// GET /api/bootstrap-static/
router.get('/bootstrap-static/', async (req, res) => {
  try {
    const { data, fromCache } = await withCache('bootstrap', TTL.BOOTSTRAP, fpl.getBootstrap);
    sendApiSuccess(res, data, { fromCache });
  } catch (err) {
    console.error('[bootstrap]', err.message);
    sendApiError(res, 502, 'bootstrap_failed', err.message);
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
    sendApiSuccess(res, data, { fromCache });
  } catch (err) {
    console.error('[fixtures]', err.message);
    sendApiError(res, 502, 'fixtures_failed', err.message);
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
    sendApiSuccess(res, data, { fromCache });
  } catch (err) {
    console.error('[live]', err.message);
    sendApiError(res, 502, 'live_failed', err.message);
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
    sendApiSuccess(res, data, { fromCache });
  } catch (err) {
    console.error('[league standings]', err.message);
    sendApiError(res, 502, 'league_standings_failed', err.message);
  }
});

// ── Manager data ──────────────────────────────────────────────────────────────
// GET /api/entry/:managerId/
router.get('/entry/:managerId/', async (req, res) => {
  const managerId = parseInt(req.params.managerId);
  const key = `manager:${managerId}`;
  try {
    const { data, fromCache } = await withCache(key, TTL.MANAGER_DATA, () => fpl.getManagerData(managerId));
    sendApiSuccess(res, data, { fromCache });
  } catch (err) {
    console.error('[manager data]', err.message);
    sendApiError(res, 502, 'manager_data_failed', err.message);
  }
});

// ── Manager history ───────────────────────────────────────────────────────────
// GET /api/entry/:managerId/history/
router.get('/entry/:managerId/history/', async (req, res) => {
  const managerId = parseInt(req.params.managerId);
  const key = `manager:${managerId}:history`;
  try {
    const { data, fromCache } = await withCache(key, TTL.MANAGER_HISTORY, () => fpl.getManagerHistory(managerId));
    sendApiSuccess(res, data, { fromCache });
  } catch (err) {
    console.error('[manager history]', err.message);
    sendApiError(res, 502, 'manager_history_failed', err.message);
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
    sendApiSuccess(res, data, { fromCache });
  } catch (err) {
    console.error('[manager picks]', err.message);
    sendApiError(res, 502, 'manager_picks_failed', err.message);
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
    sendApiSuccess(res, data, { fromCache });
  } catch (err) {
    console.error('[manager transfers]', err.message);
    sendApiError(res, 502, 'manager_transfers_failed', err.message);
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
    sendApiSuccess(res, data, { fromCache });
  } catch (err) {
    console.error('[player detail]', err.message);
    sendApiError(res, 502, 'player_detail_failed', err.message);
  }
});

// ── Stats overview ────────────────────────────────────────────────────────────
// GET /api/stats/overview?event=31
router.get('/stats/overview', async (req, res) => {
  const requestedEvent = req.query.event ? parseInt(req.query.event) : null;
  if (req.query.event && Number.isNaN(requestedEvent)) {
    return sendApiError(res, 400, 'invalid_params', 'Invalid event query parameter');
  }

  const cacheKey = `stats:overview:${requestedEvent ?? 'current'}`;
  try {
    const { data: bootstrap } = await withCache('bootstrap', TTL.BOOTSTRAP, fpl.getBootstrap);
    const resolvedEvent = requestedEvent == null
      ? (
        bootstrap.events.find((event) => event.is_current)
        ?? bootstrap.events.find((event) => event.is_next)
        ?? bootstrap.events[bootstrap.events.length - 1]
      )
      : (
        bootstrap.events.find((event) => event.id === requestedEvent)
        ?? bootstrap.events.find((event) => event.is_current)
        ?? bootstrap.events.find((event) => event.is_next)
        ?? bootstrap.events[bootstrap.events.length - 1]
      );
    const statsTtl = resolvedEvent?.is_current && !resolvedEvent?.finished
      ? TTL.STATS_ACTIVE
      : TTL.STATS_IDLE;

    const { data, fromCache } = await withCache(
      cacheKey,
      statsTtl,
      () => getStatsOverview(requestedEvent),
    );
    sendApiSuccess(res, data, { fromCache });
  } catch (err) {
    console.error('[stats overview]', err.message);
    sendApiError(res, 502, 'stats_overview_failed', err.message);
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
    return sendApiError(res, 400, 'invalid_params', 'Invalid leagueId, gameweek, or playerId');
  }

  try {
    const refreshResult = await ensureLeaguePicks(leagueId, gameweek, forceRefresh);
    const snapshot = refreshResult.snapshot ?? getLeagueSnapshot(leagueId, gameweek);
    const stats = computePlayerStats(leagueId, gameweek, playerId);

    if (!stats) {
      return sendApiError(res, 404, 'snapshot_missing', 'No picks data found for this league/gameweek');
    }

    sendApiSuccess(res, {
      ...stats,
      meta: {
        leagueId,
        gameweek,
        picksRefreshed: refreshResult.refreshed,
        snapshotStatus: snapshot?.status ?? 'missing',
        managerCountExpected: snapshot?.managerCountExpected ?? 0,
        managerCountFetched: snapshot?.managerCountFetched ?? countLeaguePicks(leagueId, gameweek),
        failedCount: snapshot?.failedCount ?? 0,
        fetchedAt: snapshot?.fetchedAt ?? null,
      },
    }, {
      snapshotStatus: snapshot?.status ?? 'missing',
    });
  } catch (err) {
    console.error('[player stats]', err.message);
    sendApiError(res, 502, 'player_stats_failed', err.message);
  }
});

// ── League picks refresh (admin/manual trigger) ───────────────────────────────
// POST /api/league/:leagueId/gw/:gameweek/refresh-picks
router.post('/league/:leagueId/gw/:gameweek/refresh-picks', async (req, res) => {
  const leagueId = parseInt(req.params.leagueId);
  const gameweek = parseInt(req.params.gameweek);
  try {
    const refreshResult = await ensureLeaguePicks(leagueId, gameweek, true);
    const snapshot = refreshResult.snapshot ?? getLeagueSnapshot(leagueId, gameweek);
    sendApiSuccess(res, {
      ok: true,
      leagueId,
      gameweek,
      snapshot,
      managersStored: countLeaguePicks(leagueId, gameweek),
    });
  } catch (err) {
    console.error('[refresh picks]', err.message);
    sendApiError(res, 502, 'refresh_picks_failed', err.message);
  }
});

module.exports = router;
