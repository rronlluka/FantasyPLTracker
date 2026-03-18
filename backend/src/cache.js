/**
 * cache.js  —  TTL constants and a helper that wraps "check cache → fetch → store".
 */

const { cacheGet, cacheSet } = require('./db');

// ── TTL values (seconds) ──────────────────────────────────────────────────────

const TTL = {
  BOOTSTRAP:        5  * 60,   // 5 min  — teams/players don't change often
  FIXTURES:         5  * 60,   // 5 min
  LIVE_ACTIVE:      60,        // 1 min  — during an active gameweek with live games
  LIVE_IDLE:        5  * 60,   // 5 min  — between gameweeks
  LEAGUE_STANDINGS: 2  * 60,   // 2 min
  PLAYER_DETAIL:    10 * 60,   // 10 min — historical stats, rarely changes
  MANAGER_DATA:     5  * 60,
  MANAGER_HISTORY:  5  * 60,
  MANAGER_PICKS:    60 * 60,   // 1 hr   — picks lock at deadline, won't change mid-GW
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
    return { data: cached, fromCache: true };
  }
  const data = await fetchFn();
  cacheSet(key, data, ttl);
  return { data, fromCache: false };
}

module.exports = { TTL, withCache };
