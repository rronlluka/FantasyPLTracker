/**
 * cache.js  —  TTL constants and a helper that wraps "check cache → fetch → store".
 */

const { cacheGet, cacheSet } = require('./storage');
const { recordCacheHit } = require('./metrics');

// ── TTL values (seconds) ──────────────────────────────────────────────────────

const TTL = {
  BOOTSTRAP:        2  * 60,   // 2 min  — event_points on players updates live
  FIXTURES:         90,        // 90 sec — game status (started/finished) must be fresh
  LIVE_ACTIVE:      45,        // 45 sec — live stats during active GW
  LIVE_IDLE:        5  * 60,   // 5 min  — between gameweeks
  STATS_ACTIVE:     2  * 60 * 60,   // 2 hr   — while the selected/current GW is live
  STATS_IDLE:       24 * 60 * 60,   // 24 hr  — for finished/non-live GWs
  LEAGUE_STANDINGS: 2  * 60,   // 2 min
  PLAYER_DETAIL:    3  * 60,   // 3 min  — bps/bonus update during live games
  MANAGER_DATA:     5  * 60,
  MANAGER_HISTORY:  5  * 60,
  MANAGER_PICKS:    3  * 60,   // 3 min  — entry_history.points updates as games settle
  MANAGER_TRANSFERS:5  * 60,
};

/**
 * Generic cache-aside helper.
 * @param {string}   key       Cache key
 * @param {number}   ttl       TTL in seconds
 * @param {Function} fetchFn   Async function to call on cache miss
 */
async function withCache(key, ttl, fetchFn) {
  const cached = cacheGet(key);
  if (cached !== null) {
    recordCacheHit(true);
    return { data: cached, fromCache: true };
  }
  recordCacheHit(false);
  const data = await fetchFn();
  cacheSet(key, data, ttl);
  return { data, fromCache: false };
}

module.exports = { TTL, withCache };
