/**
 * leaguePicks.js  —  The core service that powers Starts/Bench tabs.
 *
 * Strategy:
 *   1. When a league+GW combo is requested, check if we already have all
 *      managers' picks cached in the DB.
 *   2. If not (or if stale), fetch all managers' picks from FPL in one go
 *      and store them.
 *   3. Compute player stats from the stored picks (no extra FPL calls).
 *   4. Cache computed player stats so repeated requests for the same player
 *      are instant.
 *
 * This means no matter how many players are viewed, FPL only sees the initial
 * burst of N API calls (one per manager) — once per GW per league.
 */

const db = require('./db');
const fpl = require('./fpl');

// How old (in seconds) picks data can be before we re-fetch.
// Picks lock at deadline so this can be generous. We use 1 hour during GW,
// but you could set it to the entire GW length.
const PICKS_MAX_AGE_SECONDS = 60 * 60; // 1 hour

/**
 * Ensures the league's picks for a GW are in the DB and fresh.
 * Returns true if picks were refreshed, false if they were already fresh.
 */
async function ensureLeaguePicks(leagueId, gameweek, forceRefresh = false) {
  const now = Math.floor(Date.now() / 1000);
  const count = db.countLeaguePicks(leagueId, gameweek);
  const age = db.getLeaguePicksAge(leagueId, gameweek);
  const isStale = !age || (now - age) > PICKS_MAX_AGE_SECONDS;

  if (!forceRefresh && count > 0 && !isStale) {
    console.log(`[picks] Cache hit — league ${leagueId} GW${gameweek} (${count} managers, age ${Math.round((now - age) / 60)}min)`);
    return false;
  }

  console.log(`[picks] ${forceRefresh ? 'Force refresh' : 'Cache miss/stale'} — fetching league ${leagueId} GW${gameweek}…`);
  const managers = await fpl.fetchAllManagerPicks(leagueId, gameweek);
  db.saveLeaguePicks(leagueId, gameweek, managers);
  return true;
}

/**
 * Computes and returns league-wide stats for a specific player.
 * Reads entirely from DB — no FPL calls here.
 */
function computePlayerStats(leagueId, gameweek, playerId) {
  // Check if we already have pre-computed stats
  const cached = db.getPlayerStats(leagueId, gameweek, playerId);
  if (cached) return cached;

  // Compute from raw picks
  const rows = db.getLeaguePicks(leagueId, gameweek);
  if (rows.length === 0) return null;

  let startsCount = 0;
  let benchCount = 0;
  let captainCount = 0;
  let viceCaptainCount = 0;
  const startedBy = [];
  const benchedBy = [];
  const captainedBy = [];

  for (const row of rows) {
    const picks = JSON.parse(row.picks_json);
    const managerRef = { entryName: row.entry_name, rank: row.rank };

    for (const pick of picks) {
      if (pick.element === playerId) {
        if (pick.position <= 11) {
          startsCount++;
          startedBy.push(managerRef);
          if (pick.is_captain) {
            captainCount++;
            captainedBy.push(managerRef);
          }
          if (pick.is_vice_captain) {
            viceCaptainCount++;
          }
        } else {
          benchCount++;
          benchedBy.push(managerRef);
        }
        break; // each manager has the player at most once
      }
    }
  }

  const totalManagers = rows.length;
  const stats = {
    playerId,
    startsCount,
    benchCount,
    captainCount,
    viceCaptainCount,
    startsPercentage: totalManagers > 0 ? (startsCount / totalManagers) * 100 : 0,
    ownedPercentage:  totalManagers > 0 ? ((startsCount + benchCount) / totalManagers) * 100 : 0,
    startedBy,
    benchedBy,
    captainedBy,
  };

  // Persist for next request
  db.savePlayerStats(leagueId, gameweek, stats);
  return stats;
}

module.exports = { ensureLeaguePicks, computePlayerStats };
