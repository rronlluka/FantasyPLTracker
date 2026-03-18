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

const storage = require('./storage');
const fpl = require('./fpl');
const { recordSnapshotRefresh, recordSnapshotLockWait } = require('./metrics');

const inflightRefreshes = new Map();

function snapshotKey(leagueId, gameweek) {
  return `${leagueId}:${gameweek}`;
}

/**
 * Ensures the league's picks for a GW are in the DB.
 * Snapshots are immutable per GW unless forceRefresh is requested.
 */
async function ensureLeaguePicks(leagueId, gameweek, forceRefresh = false) {
  const existingSnapshot = storage.getLeagueSnapshot(leagueId, gameweek);
  const existingCount = storage.countLeaguePicks(leagueId, gameweek);

  if (!forceRefresh && existingSnapshot && existingCount > 0) {
    console.log(`[picks] Snapshot hit — league ${leagueId} GW${gameweek} (${existingCount}/${existingSnapshot.managerCountExpected})`);
    return {
      refreshed: false,
      snapshot: existingSnapshot,
    };
  }

  const key = snapshotKey(leagueId, gameweek);
  if (inflightRefreshes.has(key)) {
    recordSnapshotLockWait();
    return inflightRefreshes.get(key);
  }

  const refreshPromise = refreshLeagueSnapshot(leagueId, gameweek, forceRefresh)
    .finally(() => inflightRefreshes.delete(key));
  inflightRefreshes.set(key, refreshPromise);
  return refreshPromise;
}

async function refreshLeagueSnapshot(leagueId, gameweek, forceRefresh) {
  console.log(`[picks] ${forceRefresh ? 'Force refresh' : 'Missing snapshot'} — fetching league ${leagueId} GW${gameweek}…`);
  try {
    const snapshotData = await fpl.fetchAllManagerPicks(leagueId, gameweek);
    const fetchedAt = Math.floor(Date.now() / 1000);
    const snapshot = {
      leagueName: snapshotData.league?.name,
      managerCountExpected: snapshotData.managerCountExpected,
      managerCountFetched: snapshotData.managerCountFetched,
      failedCount: snapshotData.failedManagers.length,
      failedManagers: snapshotData.failedManagers,
      status: snapshotData.partial ? 'partial' : 'ready',
      fetchedAt,
      refreshDurationMs: snapshotData.fetchDurationMs,
      lastError: null,
    };
    storage.saveLeaguePicks(leagueId, gameweek, snapshot, snapshotData.managers);
    recordSnapshotRefresh(true);
    return {
      refreshed: true,
      snapshot,
    };
  } catch (err) {
    const failedSnapshot = {
      status: 'failed',
      fetchedAt: Math.floor(Date.now() / 1000),
      lastError: err.message,
      managerCountExpected: 0,
      managerCountFetched: 0,
      failedCount: 0,
      failedManagers: [],
      refreshDurationMs: null,
    };
    storage.saveLeagueSnapshot(leagueId, gameweek, failedSnapshot);
    recordSnapshotRefresh(false);
    throw err;
  }
}

/**
 * Computes and returns league-wide stats for a specific player.
 * Reads entirely from DB — no FPL calls here.
 */
function computePlayerStats(leagueId, gameweek, playerId) {
  // Check if we already have pre-computed stats
  const cached = storage.getPlayerStats(leagueId, gameweek, playerId);
  if (cached) return cached;

  // Compute from raw picks
  const rows = storage.getLeaguePicks(leagueId, gameweek);
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
  storage.savePlayerStats(leagueId, gameweek, stats);
  return stats;
}

module.exports = { ensureLeaguePicks, computePlayerStats };
