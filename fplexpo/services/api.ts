/**
 * FPL API Service
 * Mirrors BackendApiService + FPLApiService from the Android Kotlin app.
 * All calls go through your Node.js backend proxy (caching + live stats).
 */
import {
  BootstrapData,
  Fixture,
  ManagerData,
  ManagerHistory,
  ManagerPicks,
  LiveGameweek,
  LeagueStandings,
  PlayerDetailResponse,
  ManagerTransfer,
  BackendHealthResponse,
  BackendLeaguePlayerStats,
  StatsOverviewResponse,
} from '../types/fpl';

// ── Backend URL (updated at runtime via Settings screen) ──────────────────────
let _backendBase = 'http://127.0.0.1:3000/api/';

export function getBackendUrl(): string {
  return _backendBase;
}

export function setBackendUrl(url: string): void {
  let normalized = url.trim();
  if (!normalized.endsWith('/')) normalized += '/';
  if (!normalized.endsWith('api/')) {
    normalized = normalized.replace(/\/?$/, '') + '/api/';
  }
  _backendBase = normalized;
}

// ── Request helper ────────────────────────────────────────────────────────────
async function request<T>(path: string): Promise<T> {
  const url = _backendBase + path;
  const response = await fetch(url, {
    headers: { 'Content-Type': 'application/json' },
  });
  if (!response.ok) {
    throw new Error(`HTTP ${response.status} for ${url}`);
  }
  return response.json() as Promise<T>;
}

// ── Endpoints ─────────────────────────────────────────────────────────────────
export const Api = {
  // Health check
  getHealth: (): Promise<BackendHealthResponse> =>
    request<BackendHealthResponse>('health'),

  // Bootstrap (events, teams, players)
  getBootstrapData: (): Promise<BootstrapData> =>
    request<BootstrapData>('bootstrap-static/'),

  // Fixtures
  getAllFixtures: (): Promise<Fixture[]> =>
    request<Fixture[]>('fixtures/'),

  getFixturesByEvent: (eventId: number): Promise<Fixture[]> =>
    request<Fixture[]>(`fixtures/?event=${eventId}`),

  // Manager
  getManagerData: (managerId: number): Promise<ManagerData> =>
    request<ManagerData>(`entry/${managerId}/`),

  getManagerHistory: (managerId: number): Promise<ManagerHistory> =>
    request<ManagerHistory>(`entry/${managerId}/history/`),

  getManagerPicks: (managerId: number, eventId: number): Promise<ManagerPicks> =>
    request<ManagerPicks>(`entry/${managerId}/event/${eventId}/picks/`),

  getManagerTransfers: (managerId: number): Promise<ManagerTransfer[]> =>
    request<ManagerTransfer[]>(`entry/${managerId}/transfers/`),

  // Live gameweek
  getLiveGameweek: (eventId: number): Promise<LiveGameweek> =>
    request<LiveGameweek>(`event/${eventId}/live/`),

  // League standings
  getLeagueStandings: (
    leagueId: number,
    page: number = 1,
    eventId?: number,
  ): Promise<LeagueStandings> => {
    let path = `leagues-classic/${leagueId}/standings/?page_standings=${page}`;
    if (eventId != null) path += `&event=${eventId}`;
    return request<LeagueStandings>(path);
  },

  // Player detail
  getPlayerDetail: (elementId: number): Promise<PlayerDetailResponse> =>
    request<PlayerDetailResponse>(`element-summary/${elementId}/`),

  // Stats overview
  getStatsOverview: (eventId?: number): Promise<StatsOverviewResponse> =>
    request<StatsOverviewResponse>(`stats/overview${eventId != null ? `?event=${eventId}` : ''}`),

  // Backend-only: pre-computed league player stats
  getLeaguePlayerStats: (
    leagueId: number,
    gameweek: number,
    playerId: number,
  ): Promise<BackendLeaguePlayerStats> =>
    request<BackendLeaguePlayerStats>(
      `league/${leagueId}/gw/${gameweek}/player/${playerId}/stats`,
    ),
};
