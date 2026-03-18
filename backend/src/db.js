/**
 * db.js  —  SQLite database setup via Node's built-in node:sqlite module.
 *
 * Tables:
 *   cache            – generic key/value store with TTL (used for bootstrap, fixtures, live)
 *   league_picks     – all managers' picks for a league+GW, stored once, used for every player query
 *   league_snapshots – metadata about a picks snapshot refresh for league+GW
 *   player_stats     – computed starts/bench/captain stats per league+GW+player
 */

const { DatabaseSync } = require('node:sqlite');
const path = require('path');
const fs = require('fs');

const DB_DIR = path.join(__dirname, '..', 'data');
if (!fs.existsSync(DB_DIR)) fs.mkdirSync(DB_DIR, { recursive: true });

const DB_PATH = path.join(DB_DIR, 'fpl_tracker.db');

const db = new DatabaseSync(DB_PATH);

db.exec('PRAGMA journal_mode = WAL');
db.exec('PRAGMA foreign_keys = ON');

db.exec(`
  CREATE TABLE IF NOT EXISTS cache (
    key         TEXT PRIMARY KEY,
    value       TEXT NOT NULL,
    expires_at  INTEGER NOT NULL
  );

  CREATE TABLE IF NOT EXISTS league_picks (
    league_id   INTEGER NOT NULL,
    gameweek    INTEGER NOT NULL,
    manager_id  INTEGER NOT NULL,
    entry_name  TEXT    NOT NULL,
    rank        INTEGER NOT NULL DEFAULT 0,
    picks_json  TEXT    NOT NULL,
    fetched_at  INTEGER NOT NULL,
    PRIMARY KEY (league_id, gameweek, manager_id)
  );

  CREATE TABLE IF NOT EXISTS league_snapshots (
    league_id               INTEGER NOT NULL,
    gameweek                INTEGER NOT NULL,
    league_name             TEXT,
    manager_count_expected  INTEGER NOT NULL DEFAULT 0,
    manager_count_fetched   INTEGER NOT NULL DEFAULT 0,
    failed_count            INTEGER NOT NULL DEFAULT 0,
    failed_managers_json    TEXT    NOT NULL DEFAULT '[]',
    status                  TEXT    NOT NULL DEFAULT 'missing',
    fetched_at              INTEGER,
    refresh_duration_ms     INTEGER,
    last_error              TEXT,
    PRIMARY KEY (league_id, gameweek)
  );

  CREATE TABLE IF NOT EXISTS player_stats (
    league_id          INTEGER NOT NULL,
    gameweek           INTEGER NOT NULL,
    player_id          INTEGER NOT NULL,
    starts_count       INTEGER NOT NULL DEFAULT 0,
    bench_count        INTEGER NOT NULL DEFAULT 0,
    captain_count      INTEGER NOT NULL DEFAULT 0,
    vice_captain_count INTEGER NOT NULL DEFAULT 0,
    starts_pct         REAL    NOT NULL DEFAULT 0,
    owned_pct          REAL    NOT NULL DEFAULT 0,
    started_by_json    TEXT    NOT NULL DEFAULT '[]',
    benched_by_json    TEXT    NOT NULL DEFAULT '[]',
    captained_by_json  TEXT    NOT NULL DEFAULT '[]',
    computed_at        INTEGER NOT NULL,
    PRIMARY KEY (league_id, gameweek, player_id)
  );
`);

const stmts = {
  getCache: db.prepare('SELECT value, expires_at FROM cache WHERE key = ?'),
  setCache: db.prepare('INSERT OR REPLACE INTO cache (key, value, expires_at) VALUES (?, ?, ?)'),
  delCache: db.prepare('DELETE FROM cache WHERE key = ?'),
  delAllCache: db.prepare('DELETE FROM cache'),
  pruneCache: db.prepare('DELETE FROM cache WHERE expires_at < ?'),
  listActiveCache: db.prepare(
    'SELECT key, expires_at, length(value) as size_bytes FROM cache WHERE expires_at > ? ORDER BY key'
  ),

  getLeaguePicks: db.prepare('SELECT * FROM league_picks WHERE league_id = ? AND gameweek = ?'),
  upsertLeaguePick: db.prepare(`
    INSERT OR REPLACE INTO league_picks (league_id, gameweek, manager_id, entry_name, rank, picks_json, fetched_at)
    VALUES (?, ?, ?, ?, ?, ?, ?)
  `),
  deleteLeaguePicks: db.prepare('DELETE FROM league_picks WHERE league_id = ? AND gameweek = ?'),
  countLeaguePicks: db.prepare('SELECT COUNT(*) as cnt FROM league_picks WHERE league_id = ? AND gameweek = ?'),

  getLeagueSnapshot: db.prepare('SELECT * FROM league_snapshots WHERE league_id = ? AND gameweek = ?'),
  upsertLeagueSnapshot: db.prepare(`
    INSERT OR REPLACE INTO league_snapshots
      (league_id, gameweek, league_name, manager_count_expected, manager_count_fetched,
       failed_count, failed_managers_json, status, fetched_at, refresh_duration_ms, last_error)
    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
  `),

  getPlayerStats: db.prepare('SELECT * FROM player_stats WHERE league_id = ? AND gameweek = ? AND player_id = ?'),
  upsertPlayerStats: db.prepare(`
    INSERT OR REPLACE INTO player_stats
      (league_id, gameweek, player_id, starts_count, bench_count, captain_count, vice_captain_count,
       starts_pct, owned_pct, started_by_json, benched_by_json, captained_by_json, computed_at)
    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
  `),
  deletePlayerStats: db.prepare('DELETE FROM player_stats WHERE league_id = ? AND gameweek = ?'),
};

function cacheGet(key) {
  const row = stmts.getCache.get(key);
  if (!row) return null;
  if (row.expires_at < Math.floor(Date.now() / 1000)) {
    stmts.delCache.run(key);
    return null;
  }
  return JSON.parse(row.value);
}

function cacheSet(key, value, ttlSeconds) {
  const expiresAt = Math.floor(Date.now() / 1000) + ttlSeconds;
  stmts.setCache.run(key, JSON.stringify(value), expiresAt);
}

function deleteCacheKey(key) {
  return stmts.delCache.run(key);
}

function deleteAllCache() {
  return stmts.delAllCache.run();
}

function listActiveCache() {
  return stmts.listActiveCache.all(Math.floor(Date.now() / 1000));
}

function cachePrune() {
  const deleted = stmts.pruneCache.run(Math.floor(Date.now() / 1000));
  if (deleted.changes > 0) {
    console.log(`[cache] Pruned ${deleted.changes} expired entries`);
  }
}

function getLeaguePicks(leagueId, gameweek) {
  return stmts.getLeaguePicks.all(leagueId, gameweek);
}

function countLeaguePicks(leagueId, gameweek) {
  return stmts.countLeaguePicks.get(leagueId, gameweek)?.cnt ?? 0;
}

function parseLeagueSnapshotRow(row) {
  if (!row) return null;
  return {
    leagueId: row.league_id,
    gameweek: row.gameweek,
    leagueName: row.league_name,
    managerCountExpected: row.manager_count_expected,
    managerCountFetched: row.manager_count_fetched,
    failedCount: row.failed_count,
    failedManagers: JSON.parse(row.failed_managers_json),
    status: row.status,
    fetchedAt: row.fetched_at,
    refreshDurationMs: row.refresh_duration_ms,
    lastError: row.last_error,
  };
}

function getLeagueSnapshot(leagueId, gameweek) {
  return parseLeagueSnapshotRow(stmts.getLeagueSnapshot.get(leagueId, gameweek));
}

function saveLeagueSnapshot(leagueId, gameweek, snapshot) {
  stmts.upsertLeagueSnapshot.run(
    leagueId,
    gameweek,
    snapshot.leagueName ?? null,
    snapshot.managerCountExpected ?? 0,
    snapshot.managerCountFetched ?? 0,
    snapshot.failedCount ?? 0,
    JSON.stringify(snapshot.failedManagers ?? []),
    snapshot.status ?? 'missing',
    snapshot.fetchedAt ?? null,
    snapshot.refreshDurationMs ?? null,
    snapshot.lastError ?? null
  );
}

function saveLeaguePicks(leagueId, gameweek, snapshot, managers) {
  const now = snapshot.fetchedAt ?? Math.floor(Date.now() / 1000);
  db.exec('BEGIN');
  try {
    stmts.deleteLeaguePicks.run(leagueId, gameweek);
    stmts.deletePlayerStats.run(leagueId, gameweek);

    for (const m of managers) {
      stmts.upsertLeaguePick.run(
        leagueId,
        gameweek,
        m.managerId,
        m.entryName,
        m.rank,
        JSON.stringify(m.picks),
        now
      );
    }

    saveLeagueSnapshot(leagueId, gameweek, {
      ...snapshot,
      fetchedAt: now,
    });

    db.exec('COMMIT');
  } catch (err) {
    db.exec('ROLLBACK');
    throw err;
  }
  console.log(
    `[db] Saved picks for ${managers.length}/${snapshot.managerCountExpected} managers — league ${leagueId} GW${gameweek} (${snapshot.status})`
  );
}

function getPlayerStats(leagueId, gameweek, playerId) {
  const row = stmts.getPlayerStats.get(leagueId, gameweek, playerId);
  if (!row) return null;
  return {
    playerId: row.player_id,
    startsCount: row.starts_count,
    benchCount: row.bench_count,
    captainCount: row.captain_count,
    viceCaptainCount: row.vice_captain_count,
    startsPercentage: row.starts_pct,
    ownedPercentage: row.owned_pct,
    startedBy: JSON.parse(row.started_by_json),
    benchedBy: JSON.parse(row.benched_by_json),
    captainedBy: JSON.parse(row.captained_by_json),
  };
}

function savePlayerStats(leagueId, gameweek, stats) {
  const now = Math.floor(Date.now() / 1000);
  stmts.upsertPlayerStats.run(
    leagueId,
    gameweek,
    stats.playerId,
    stats.startsCount,
    stats.benchCount,
    stats.captainCount,
    stats.viceCaptainCount,
    stats.startsPercentage,
    stats.ownedPercentage,
    JSON.stringify(stats.startedBy),
    JSON.stringify(stats.benchedBy),
    JSON.stringify(stats.captainedBy),
    now
  );
}

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
  deleteCacheKey,
  deleteAllCache,
  listActiveCache,
  cachePrune,
  getLeaguePicks,
  countLeaguePicks,
  getLeagueSnapshot,
  saveLeagueSnapshot,
  saveLeaguePicks,
  getPlayerStats,
  savePlayerStats,
  getDbInfo,
};
