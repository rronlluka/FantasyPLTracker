/**
 * db.js  —  SQLite database setup via Node's built-in node:sqlite module.
 *
 * Tables:
 *   cache          – generic key/value store with TTL (used for bootstrap, fixtures, live)
 *   league_picks   – all managers' picks for a league+GW, stored once, used for every player query
 *   player_stats   – computed starts/bench/captain stats per league+GW+player
 */

const { DatabaseSync } = require('node:sqlite');
const path = require('path');
const fs = require('fs');

const DB_DIR = path.join(__dirname, '..', 'data');
if (!fs.existsSync(DB_DIR)) fs.mkdirSync(DB_DIR, { recursive: true });

const DB_PATH = path.join(DB_DIR, 'fpl_tracker.db');

const db = new DatabaseSync(DB_PATH);

// Enable WAL mode for better read/write concurrency
db.exec('PRAGMA journal_mode = WAL');
db.exec('PRAGMA foreign_keys = ON');

// ── Schema ────────────────────────────────────────────────────────────────────

db.exec(`
  -- Generic cache table: stores JSON blobs with an expiry timestamp
  CREATE TABLE IF NOT EXISTS cache (
    key         TEXT PRIMARY KEY,
    value       TEXT NOT NULL,
    expires_at  INTEGER NOT NULL  -- Unix timestamp (seconds)
  );

  -- Stores the raw picks for every manager in a league for a specific GW.
  -- Fetched once per GW per league, reused for all player stat queries.
  CREATE TABLE IF NOT EXISTS league_picks (
    league_id   INTEGER NOT NULL,
    gameweek    INTEGER NOT NULL,
    manager_id  INTEGER NOT NULL,
    entry_name  TEXT    NOT NULL,
    rank        INTEGER NOT NULL DEFAULT 0,
    picks_json  TEXT    NOT NULL,   -- JSON array of picks from FPL API
    fetched_at  INTEGER NOT NULL,   -- Unix timestamp
    PRIMARY KEY (league_id, gameweek, manager_id)
  );

  -- Computed player stats per league/GW/player — derived from league_picks.
  -- Invalidated automatically when league_picks is refreshed.
  CREATE TABLE IF NOT EXISTS player_stats (
    league_id         INTEGER NOT NULL,
    gameweek          INTEGER NOT NULL,
    player_id         INTEGER NOT NULL,
    starts_count      INTEGER NOT NULL DEFAULT 0,
    bench_count       INTEGER NOT NULL DEFAULT 0,
    captain_count     INTEGER NOT NULL DEFAULT 0,
    vice_captain_count INTEGER NOT NULL DEFAULT 0,
    starts_pct        REAL    NOT NULL DEFAULT 0,
    owned_pct         REAL    NOT NULL DEFAULT 0,
    started_by_json   TEXT    NOT NULL DEFAULT '[]',  -- [{entryName, rank}]
    benched_by_json   TEXT    NOT NULL DEFAULT '[]',
    captained_by_json TEXT    NOT NULL DEFAULT '[]',
    computed_at       INTEGER NOT NULL,
    PRIMARY KEY (league_id, gameweek, player_id)
  );
`);

// ── Helpers ───────────────────────────────────────────────────────────────────

const stmts = {
  // cache
  getCache:   db.prepare('SELECT value, expires_at FROM cache WHERE key = ?'),
  setCache:   db.prepare('INSERT OR REPLACE INTO cache (key, value, expires_at) VALUES (?, ?, ?)'),
  delCache:   db.prepare('DELETE FROM cache WHERE key = ?'),
  pruneCache: db.prepare('DELETE FROM cache WHERE expires_at < ?'),

  // league_picks
  getLeaguePicks:      db.prepare('SELECT * FROM league_picks WHERE league_id = ? AND gameweek = ?'),
  getLeaguePicksAge:   db.prepare('SELECT MIN(fetched_at) as oldest FROM league_picks WHERE league_id = ? AND gameweek = ?'),
  upsertLeaguePick:    db.prepare(`
    INSERT OR REPLACE INTO league_picks (league_id, gameweek, manager_id, entry_name, rank, picks_json, fetched_at)
    VALUES (?, ?, ?, ?, ?, ?, ?)
  `),
  deleteLeaguePicks:   db.prepare('DELETE FROM league_picks WHERE league_id = ? AND gameweek = ?'),
  countLeaguePicks:    db.prepare('SELECT COUNT(*) as cnt FROM league_picks WHERE league_id = ? AND gameweek = ?'),

  // player_stats
  getPlayerStats:    db.prepare('SELECT * FROM player_stats WHERE league_id = ? AND gameweek = ? AND player_id = ?'),
  upsertPlayerStats: db.prepare(`
    INSERT OR REPLACE INTO player_stats
      (league_id, gameweek, player_id, starts_count, bench_count, captain_count, vice_captain_count,
       starts_pct, owned_pct, started_by_json, benched_by_json, captained_by_json, computed_at)
    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
  `),
  deletePlayerStats: db.prepare('DELETE FROM player_stats WHERE league_id = ? AND gameweek = ?'),
};

// ── Cache helpers ─────────────────────────────────────────────────────────────

/**
 * Get a cached value. Returns parsed JSON or null if missing/expired.
 */
function cacheGet(key) {
  const row = stmts.getCache.get(key);
  if (!row) return null;
  if (row.expires_at < Math.floor(Date.now() / 1000)) {
    stmts.delCache.run(key);
    return null;
  }
  return JSON.parse(row.value);
}

/**
 * Set a cached value with a TTL in seconds.
 */
function cacheSet(key, value, ttlSeconds) {
  const expiresAt = Math.floor(Date.now() / 1000) + ttlSeconds;
  stmts.setCache.run(key, JSON.stringify(value), expiresAt);
}

/**
 * Delete all expired cache rows (run periodically).
 */
function cachePrune() {
  const deleted = stmts.pruneCache.run(Math.floor(Date.now() / 1000));
  if (deleted.changes > 0) {
    console.log(`[cache] Pruned ${deleted.changes} expired entries`);
  }
}

// ── League picks helpers ──────────────────────────────────────────────────────

function getLeaguePicks(leagueId, gameweek) {
  return stmts.getLeaguePicks.all(leagueId, gameweek);
}

function getLeaguePicksAge(leagueId, gameweek) {
  const row = stmts.getLeaguePicksAge.get(leagueId, gameweek);
  return row?.oldest ?? null;
}

function saveLeaguePicks(leagueId, gameweek, managers) {
  const now = Math.floor(Date.now() / 1000);
  // Delete old data for this league+GW first (also cascades player_stats)
  db.exec('BEGIN');
  try {
    stmts.deleteLeaguePicks.run(leagueId, gameweek);
    stmts.deletePlayerStats.run(leagueId, gameweek);
    for (const m of managers) {
      stmts.upsertLeaguePick.run(
        leagueId, gameweek, m.managerId, m.entryName, m.rank,
        JSON.stringify(m.picks), now
      );
    }
    db.exec('COMMIT');
  } catch (err) {
    db.exec('ROLLBACK');
    throw err;
  }
  console.log(`[db] Saved picks for ${managers.length} managers — league ${leagueId} GW${gameweek}`);
}

function countLeaguePicks(leagueId, gameweek) {
  return stmts.countLeaguePicks.get(leagueId, gameweek)?.cnt ?? 0;
}

// ── Player stats helpers ──────────────────────────────────────────────────────

function getPlayerStats(leagueId, gameweek, playerId) {
  const row = stmts.getPlayerStats.get(leagueId, gameweek, playerId);
  if (!row) return null;
  return {
    playerId:         row.player_id,
    startsCount:      row.starts_count,
    benchCount:       row.bench_count,
    captainCount:     row.captain_count,
    viceCaptainCount: row.vice_captain_count,
    startsPercentage: row.starts_pct,
    ownedPercentage:  row.owned_pct,
    startedBy:        JSON.parse(row.started_by_json),
    benchedBy:        JSON.parse(row.benched_by_json),
    captainedBy:      JSON.parse(row.captained_by_json),
  };
}

function savePlayerStats(leagueId, gameweek, stats) {
  const now = Math.floor(Date.now() / 1000);
  stmts.upsertPlayerStats.run(
    leagueId, gameweek, stats.playerId,
    stats.startsCount, stats.benchCount,
    stats.captainCount, stats.viceCaptainCount,
    stats.startsPercentage, stats.ownedPercentage,
    JSON.stringify(stats.startedBy),
    JSON.stringify(stats.benchedBy),
    JSON.stringify(stats.captainedBy),
    now
  );
}

// ── DB info (for /admin/db-info endpoint) ─────────────────────────────────────

function getDbInfo() {
  const tables = db.prepare(`
    SELECT name FROM sqlite_master WHERE type='table' ORDER BY name
  `).all();

  const info = {};
  for (const { name } of tables) {
    const count = db.prepare(`SELECT COUNT(*) as cnt FROM "${name}"`).get();
    const sample = db.prepare(`SELECT * FROM "${name}" LIMIT 5`).all();
    info[name] = { rowCount: count.cnt, sample };
  }
  info._dbPath = DB_PATH;
  return info;
}

module.exports = {
  db,
  cacheGet,
  cacheSet,
  cachePrune,
  getLeaguePicks,
  getLeaguePicksAge,
  saveLeaguePicks,
  countLeaguePicks,
  getPlayerStats,
  savePlayerStats,
  getDbInfo,
};
