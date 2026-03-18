/**
 * fpl.js  —  All calls to the real FPL API.
 * Uses Node's built-in https module — no external dependencies needed.
 */

const https = require('https');

const FPL_BASE_HOST = 'fantasy.premierleague.com';
const FPL_BASE_PATH = '/api';

const HEADERS = {
  'User-Agent':
    'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 ' +
    '(KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36',
  'Accept': 'application/json, text/plain, */*',
  'Accept-Language': 'en-GB,en;q=0.9',
  'Origin': 'https://fantasy.premierleague.com',
  'Referer': 'https://fantasy.premierleague.com/',
};

/**
 * Simple HTTPS GET using Node built-ins. Returns parsed JSON.
 */
function httpsGet(path) {
  return new Promise((resolve, reject) => {
    const options = {
      hostname: FPL_BASE_HOST,
      path: `${FPL_BASE_PATH}${path}`,
      method: 'GET',
      headers: HEADERS,
      timeout: 15000,
    };

    const req = https.request(options, (res) => {
      let body = '';
      res.on('data', chunk => body += chunk);
      res.on('end', () => {
        if (res.statusCode >= 200 && res.statusCode < 300) {
          try {
            resolve(JSON.parse(body));
          } catch (e) {
            reject(new Error(`JSON parse error for ${path}: ${e.message}`));
          }
        } else {
          reject(new Error(`FPL API ${path} returned HTTP ${res.statusCode}`));
        }
      });
    });

    req.on('timeout', () => {
      req.destroy();
      reject(new Error(`Timeout fetching ${path}`));
    });
    req.on('error', reject);
    req.end();
  });
}

/**
 * Retry wrapper — retries once on failure.
 */
async function fplGet(path) {
  for (let attempt = 1; attempt <= 2; attempt++) {
    try {
      return await httpsGet(path);
    } catch (err) {
      if (attempt === 2) throw err;
      console.warn(`[fpl] Retrying ${path} (attempt ${attempt}): ${err.message}`);
      await new Promise(r => setTimeout(r, 600 * attempt));
    }
  }
}

// ── Endpoints ─────────────────────────────────────────────────────────────────

function getBootstrap() {
  return fplGet('/bootstrap-static/');
}

function getFixtures(eventId = null) {
  const query = eventId != null ? `?event=${eventId}` : '';
  return fplGet(`/fixtures/${query}`);
}

function getManagerData(managerId) {
  return fplGet(`/entry/${managerId}/`);
}

function getManagerHistory(managerId) {
  return fplGet(`/entry/${managerId}/history/`);
}

function getManagerPicks(managerId, eventId) {
  return fplGet(`/entry/${managerId}/event/${eventId}/picks/`);
}

function getLiveGameweek(eventId) {
  return fplGet(`/event/${eventId}/live/`);
}

function getLeagueStandings(leagueId, page = 1, eventId = null) {
  let path = `/leagues-classic/${leagueId}/standings/?page_standings=${page}`;
  if (eventId != null) path += `&event=${eventId}`;
  return fplGet(path);
}

function getPlayerDetail(elementId) {
  return fplGet(`/element-summary/${elementId}/`);
}

function getManagerTransfers(managerId) {
  return fplGet(`/entry/${managerId}/transfers/`);
}

/**
 * Fetches ALL pages of a league's standings (FPL paginates at 50 per page).
 */
async function getAllLeagueStandings(leagueId) {
  let page = 1;
  let hasNext = true;
  const allResults = [];
  let leagueMeta = null;

  while (hasNext) {
    const data = await getLeagueStandings(leagueId, page);
    if (!leagueMeta) leagueMeta = data.league;
    allResults.push(...data.standings.results);
    hasNext = data.standings.has_next;
    page++;
    if (hasNext) await new Promise(r => setTimeout(r, 200));
  }

  return { league: leagueMeta, results: allResults };
}

/**
 * Fetches picks for all managers in a league for a given GW.
 * Batched to avoid overwhelming FPL API.
 */
async function fetchAllManagerPicks(leagueId, gameweek, batchSize = 8) {
  const { league, results } = await getAllLeagueStandings(leagueId);
  console.log(`[fpl] League "${league.name}" — ${results.length} managers, fetching GW${gameweek} picks…`);

  const managerPicks = [];

  for (let i = 0; i < results.length; i += batchSize) {
    const batch = results.slice(i, i + batchSize);
    const batchResults = await Promise.allSettled(
      batch.map(async (standing) => {
        const picks = await getManagerPicks(standing.entry, gameweek);
        return {
          managerId: standing.entry,
          entryName: standing.entry_name,
          rank:      standing.rank,
          picks:     picks.picks,
        };
      })
    );
    for (const result of batchResults) {
      if (result.status === 'fulfilled') {
        managerPicks.push(result.value);
      } else {
        console.warn(`[fpl] Failed picks fetch: ${result.reason?.message}`);
      }
    }
    if (i + batchSize < results.length) {
      await new Promise(r => setTimeout(r, 300));
    }
  }

  console.log(`[fpl] Got picks for ${managerPicks.length}/${results.length} managers`);
  return managerPicks;
}

module.exports = {
  getBootstrap,
  getFixtures,
  getManagerData,
  getManagerHistory,
  getManagerPicks,
  getLiveGameweek,
  getLeagueStandings,
  getPlayerDetail,
  getManagerTransfers,
  getAllLeagueStandings,
  fetchAllManagerPicks,
};
