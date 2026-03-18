# FPL Tracker Backend

This backend sits in front of the public FPL API and caches responses locally.

The important endpoint for the Android app is:

`GET /api/league/:leagueId/gw/:gameweek/player/:playerId/stats`

That endpoint fetches all managers' picks for a league/gameweek once, stores them in SQLite, and then serves player `starts / bench / captain` stats from the cached dataset.

## Requirements

- Node.js 20+ recommended
- npm

## Install

From the project root:

```bash
cd backend
npm install
```

This installs:

- `express`
- `cors`
- `node-cron`
- `nodemon` for development

SQLite now uses Node's built-in `node:sqlite`, so there is no native SQLite package to compile.

## Run

Development:

```bash
cd backend
npm run dev
```

Production/local plain run:

```bash
cd backend
npm start
```

The server starts on `http://localhost:3000`.

If you previously tried installing the old native SQLite dependency, clean the backend once before reinstalling:

```bash
cd backend
rm -rf node_modules package-lock.json
npm install
```

## Android config

The app now supports a runtime backend URL in debug builds.

Default:

`http://127.0.0.1:3000/api/`

This is intended for `adb reverse tcp:3000 tcp:3000` on a physical Android device.

You can change the backend URL from the debug panel on the login screen without editing code.

Examples:

- Emulator host access: `http://10.0.2.2:3000/api/`
- Physical device on same Wi-Fi: `http://192.168.1.25:3000/api/`
- ADB reverse on a physical device: `http://127.0.0.1:3000/api/`

## Useful endpoints

App-facing:

- `GET /health`
- `GET /api/bootstrap-static/`
- `GET /api/leagues-classic/:leagueId/standings/`
- `GET /api/entry/:managerId/event/:eventId/picks/`
- `GET /api/league/:leagueId/gw/:gameweek/player/:playerId/stats`

Admin/debug:

- `GET /admin/stats`
- `GET /admin/db-info`
- `GET /admin/cache`
- `GET /admin/league-picks/:leagueId/:gameweek`
- `GET /admin/player-stats/:leagueId/:gameweek`

Example:

```bash
curl "http://localhost:3000/api/league/123456/gw/28/player/355/stats"
```

Force a refresh of the league picks snapshot:

```bash
curl -X POST "http://localhost:3000/api/league/123456/gw/28/refresh-picks"
```

## Where data is stored

SQLite database file:

`backend/data/fpl_tracker.db`

Tables:

- `cache`
- `league_picks`
- `league_snapshots`
- `player_stats`

## How to inspect the DB

Option 1: use the admin endpoints.

```bash
curl "http://localhost:3000/admin/stats"
curl "http://localhost:3000/admin/db-info"
curl "http://localhost:3000/admin/league-picks/123456/28"
curl "http://localhost:3000/admin/player-stats/123456/28"
```

Option 2: open SQLite directly.

```bash
cd backend
sqlite3 data/fpl_tracker.db
```

Then for example:

```sql
.tables
select league_id, gameweek, manager_id, entry_name, rank from league_picks limit 20;
select league_id, gameweek, player_id, starts_count, bench_count, captain_count from player_stats limit 20;
```

## Cache behavior

- Generic FPL proxy responses use TTLs in [`cache.js`](/Users/rronlluka/AndroidStudioProjects/FantasyPLTracker/backend/src/cache.js)
- `league_picks` are snapshot-based per gameweek and are refreshed only when missing or manually force-refreshed
- `league_snapshots` store snapshot completeness, fetch duration, and failed-manager metadata
- `player_stats` are recomputed from cached `league_picks`
- concurrent refreshes for the same `league + gameweek` are deduplicated in-memory

## Admin protection

- Admin routes are enabled by default outside production
- In production, set `ENABLE_ADMIN=true` to expose them
- Set `ADMIN_SECRET=your-secret` to require the `x-admin-secret` header for `/admin/*`

## Current app integration

The Android app now calls the backend for league player stats from:

[`ManagerFormationScreen.kt`](/Users/rronlluka/AndroidStudioProjects/FantasyPLTracker/app/src/main/java/com/fpl/tracker/ui/screens/ManagerFormationScreen.kt)

through:

[`FPLRepository.kt`](/Users/rronlluka/AndroidStudioProjects/FantasyPLTracker/app/src/main/java/com/fpl/tracker/data/repository/FPLRepository.kt)

The Android app is backend-only now. If the backend is down or unreachable, app requests fail instead of calling the public FPL API directly.
