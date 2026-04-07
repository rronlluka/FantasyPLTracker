// ── Bootstrap ─────────────────────────────────────────────────────────────────
export interface BootstrapData {
  events: Event[];
  teams: Team[];
  elements: Player[];
  element_types: ElementType[];
}

export interface Event {
  id: number;
  name: string;
  deadline_time: string;
  average_entry_score?: number;
  finished: boolean;
  data_checked: boolean;
  highest_scoring_entry?: number;
  highest_score?: number;
  is_previous: boolean;
  is_current: boolean;
  is_next: boolean;
}

export interface Team {
  id: number;
  name: string;
  short_name: string;
  code: number;
  strength: number;
}

export interface Player {
  id: number;
  web_name: string;
  first_name: string;
  second_name: string;
  team: number;
  element_type: number;
  now_cost: number;
  total_points: number;
  event_points: number;
  points_per_game: string;
  form: string;
  selected_by_percent: string;
  status: string;
  news?: string;
  photo: string;
  goals_scored?: number;
  assists?: number;
  clean_sheets?: number;
  cost_change_start?: number;
}

export interface ElementType {
  id: number;
  plural_name: string;
  plural_name_short: string;
  singular_name: string;
  singular_name_short: string;
}

// ── Manager ───────────────────────────────────────────────────────────────────
export interface ManagerData {
  id: number;
  player_first_name: string;
  player_last_name: string;
  player_region_name: string;
  summary_overall_points: number;
  summary_overall_rank: number;
  summary_event_points: number;
  summary_event_rank?: number;
  current_event: number;
  name: string;
  favourite_team?: number;
  started_event: number;
  leagues: Leagues;
}

export interface Leagues {
  classic: LeagueInfo[];
  h2h?: LeagueInfo[];
}

export interface LeagueInfo {
  id: number;
  name: string;
  short_name?: string;
  created: string;
  entry_rank?: number;
  entry_last_rank?: number;
  entry_can_leave: boolean;
  entry_can_admin: boolean;
  entry_can_invite: boolean;
}

// ── Manager History ───────────────────────────────────────────────────────────
export interface ManagerHistory {
  current: GameweekHistory[];
  past: PastSeason[];
  chips: ChipPlay[];
}

export interface GameweekHistory {
  event: number;
  points: number;
  total_points: number;
  rank?: number;
  rank_sort?: number;
  overall_rank: number;
  bank: number;
  value: number;
  event_transfers: number;
  event_transfers_cost: number;
  points_on_bench: number;
}

export interface PastSeason {
  season_name: string;
  total_points: number;
  rank: number;
}

export interface ChipPlay {
  name: string;
  time: string;
  event: number;
}

// ── Manager Picks ─────────────────────────────────────────────────────────────
export interface ManagerPicks {
  active_chip?: string;
  automatic_subs?: AutomaticSub[];
  entry_history: EntryHistory;
  picks: Pick[];
}

export interface AutomaticSub {
  entry: number;
  element_in: number;
  element_out: number;
  event: number;
}

export interface EntryHistory {
  event: number;
  points: number;
  total_points: number;
  rank?: number;
  rank_sort?: number;
  overall_rank: number;
  bank: number;
  value: number;
  event_transfers: number;
  event_transfers_cost: number;
  points_on_bench: number;
}

export interface Pick {
  element: number;
  position: number;
  multiplier: number;
  is_captain: boolean;
  is_vice_captain: boolean;
}

// ── League Standings ──────────────────────────────────────────────────────────
export interface LeagueStandings {
  league: LeagueDetails;
  standings: StandingsData;
}

export interface LeagueDetails {
  id: number;
  name: string;
  created: string;
  start_event: number;
  code_privacy: string;
}

export interface StandingsData {
  has_next: boolean;
  page: number;
  results: StandingEntry[];
}

export interface StandingEntry {
  id: number;
  event_total: number;
  player_name: string;
  rank: number;
  last_rank: number;
  rank_sort: number;
  total: number;
  entry: number;
  entry_name: string;
}

// ── Live Gameweek ─────────────────────────────────────────────────────────────
export interface LiveGameweek {
  elements: LiveElement[];
}

export interface LiveElement {
  id: number;
  stats: LiveStats;
  explain: ExplainEntry[];
}

export interface LiveStats {
  minutes: number;
  goals_scored: number;
  assists: number;
  clean_sheets: number;
  goals_conceded: number;
  own_goals: number;
  penalties_saved: number;
  penalties_missed: number;
  yellow_cards: number;
  red_cards: number;
  saves: number;
  bonus: number;
  bps: number;
  influence: string;
  creativity: string;
  threat: string;
  ict_index: string;
  starts: number;
  expected_goals: string;
  expected_assists: string;
  expected_goal_involvements: string;
  expected_goals_conceded: string;
  total_points: number;
  in_dreamteam: boolean;
}

export interface ExplainEntry {
  fixture: number;
  stats: FixtureStat[];
}

export interface FixtureStat {
  identifier: string;
  points: number;
  value: number;
}

// ── Fixtures ──────────────────────────────────────────────────────────────────
export interface Fixture {
  id: number;
  code: number;
  event?: number;
  finished: boolean;
  finished_provisional: boolean;
  kickoff_time?: string;
  minutes: number;
  provisional_start_time: boolean;
  started?: boolean;
  team_a: number;
  team_a_score?: number;
  team_h: number;
  team_h_score?: number;
  stats: FixtureStatGroup[];
  team_h_difficulty: number;
  team_a_difficulty: number;
  pulse_id: number;
}

export interface FixtureStatGroup {
  identifier: string;
  a: FixtureStatValue[];
  h: FixtureStatValue[];
}

export interface FixtureStatValue {
  value: number;
  element: number;
}

// ── Transfers ─────────────────────────────────────────────────────────────────
export interface ManagerTransfer {
  element_in: number;
  element_in_cost: number;
  element_out: number;
  element_out_cost: number;
  entry: number;
  event: number;
  time: string;
}

// ── Backend ───────────────────────────────────────────────────────────────────
export interface BackendHealthResponse {
  ok: boolean;
  ts?: string;
  uptime_seconds?: number;
}

export interface LeagueManagerRef {
  entryName: string;
  rank: number;
}

export interface BackendLeaguePlayerStats {
  playerId: number;
  startsCount: number;
  benchCount: number;
  captainCount: number;
  viceCaptainCount: number;
  startsPercentage: number;
  ownedPercentage: number;
  startedBy: LeagueManagerRef[];
  benchedBy: LeagueManagerRef[];
  captainedBy: LeagueManagerRef[];
  league_id?: number;
  gameweek?: number;
  player_id?: number;
  starts?: number;
  bench?: number;
  captain?: number;
  vice_captain?: number;
  managers_count?: number;
}

export interface StatsOverviewResponse {
  meta: StatsOverviewMeta;
  sections: {
    mostPoints: StatsLeaderboardSection;
    gameweekPoints: StatsLeaderboardSection & { isLive?: boolean };
    goals: StatsLeaderboardSection;
    assists: StatsLeaderboardSection;
    marketRisers: StatsLeaderboardSection;
    marketFallers: StatsLeaderboardSection;
    form?: StatsLeaderboardSection;
    ownership?: StatsLeaderboardSection;
  };
  defCon: {
    DEF: StatsLeaderboardSection;
    MID: StatsLeaderboardSection;
    FWD: StatsLeaderboardSection;
  };
}

export interface StatsOverviewMeta {
  event: number;
  eventName: string;
  isLive: boolean;
  currentEvent: number;
}

export interface StatsLeaderboardSection {
  title: string;
  statLabel: string;
  rows: StatsLeaderboardPlayer[];
}

export interface StatsLeaderboardPlayer {
  playerId: number;
  name: string;
  teamId: number;
  teamShortName: string;
  position: string;
  primaryValue: number;
  primaryDisplay: string;
  secondaryDisplay?: string;
  currentPrice?: number;
  liveDelta?: number;
  priceDelta?: number;
  bestActions?: number;
}

// ── Player Detail ─────────────────────────────────────────────────────────────
export interface PlayerDetailResponse {
  fixtures: PlayerFixture[];
  history: PlayerHistory[];
  history_past: PlayerHistoryPast[];
}

export interface PlayerFixture {
  id: number;
  code: number;
  team_h: number;
  team_h_score?: number;
  team_a: number;
  team_a_score?: number;
  event: number;
  finished: boolean;
  minutes: number;
  provisional_start_time: boolean;
  kickoff_time: string;
  event_name: string;
  is_home: boolean;
  difficulty: number;
}

export interface PlayerHistory {
  element: number;
  fixture: number;
  opponent_team: number;
  total_points: number;
  was_home: boolean;
  kickoff_time: string;
  team_h_score?: number;
  team_a_score?: number;
  round: number;
  minutes: number;
  goals_scored: number;
  assists: number;
  clean_sheets: number;
  goals_conceded: number;
  own_goals: number;
  penalties_saved: number;
  penalties_missed: number;
  yellow_cards: number;
  red_cards: number;
  saves: number;
  bonus: number;
  bps: number;
  value: number;
  defensive_contribution?: number;
  transfers_balance: number;
  selected: number;
  transfers_in: number;
  transfers_out: number;
  expected_goals?: string;
  expected_assists?: string;
  expected_goals_conceded?: string;
}

export interface PlayerHistoryPast {
  season_name: string;
  element_code: number;
  start_cost: number;
  end_cost: number;
  total_points: number;
  minutes: number;
  goals_scored: number;
  assists: number;
  clean_sheets: number;
  goals_conceded: number;
  own_goals: number;
  penalties_saved: number;
  penalties_missed: number;
  yellow_cards: number;
  red_cards: number;
  saves: number;
  bonus: number;
  bps: number;
  influence: string;
  creativity: string;
  threat: string;
  ict_index: string;
}
