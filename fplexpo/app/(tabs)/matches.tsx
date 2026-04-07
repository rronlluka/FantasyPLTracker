/**
 * Matches Tab (MatchesScreen.kt port)
 * Shows fixtures for the selected gameweek with live score indicators.
 */
import React, { useEffect, useState, useCallback } from 'react';
import {
  View, Text, FlatList, TouchableOpacity, ActivityIndicator,
  StyleSheet, RefreshControl, ScrollView,
} from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { Api } from '@/services/api';
import {
  Fixture, Event, BootstrapData, Team, LiveGameweek,
  LiveElement, Player, FixtureStatValue, PlayerDetailResponse, BackendLeaguePlayerStats, LeagueManagerRef,
} from '@/types/fpl';
import { Colors, Radius, getDifficultyTheme } from '@/constants/theme';
import { AppBottomSheet } from '@/components/ui/app-bottom-sheet';
import { Storage } from '@/utils/storage';

function formatKickoff(iso?: string): string {
  if (!iso) return 'TBC';
  const d = new Date(iso);
  return d.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }) + ' ' +
    d.toLocaleDateString([], { weekday: 'short', day: 'numeric', month: 'short' });
}

type GoalEntry = {
  element: number;
  playerName: string;
  count: number;
  isOwnGoal: boolean;
  isPenalty: boolean;
};

type MatchDialogPlayer = {
  player: Player;
  teamInfo?: Team;
  teamShortName: string;
  fixture?: Fixture;
  liveStats?: LiveElement;
};

function buildGoalEntries({
  scoringEntries,
  ownGoalEntries,
  penaltyEntries,
  players,
}: {
  scoringEntries: FixtureStatValue[];
  ownGoalEntries: FixtureStatValue[];
  penaltyEntries: FixtureStatValue[];
  players: Player[];
}): GoalEntry[] {
  const entryMap = new Map<string, GoalEntry>();
  const playerName = (id: number) => players.find((player) => player.id === id)?.web_name ?? `#${id}`;

  scoringEntries.forEach((entry) => {
    const name = playerName(entry.element);
    const penalties = penaltyEntries.find((penalty) => penalty.element === entry.element)?.value ?? 0;
    entryMap.set(`goal-${entry.element}`, {
      element: entry.element,
      playerName: name,
      count: entry.value,
      isOwnGoal: false,
      isPenalty: penalties > 0,
    });
  });

  ownGoalEntries.forEach((entry) => {
    const name = playerName(entry.element);
    entryMap.set(`own-${entry.element}`, {
      element: entry.element,
      playerName: name,
      count: entry.value,
      isOwnGoal: true,
      isPenalty: false,
    });
  });

  return [...entryMap.values()];
}

export default function MatchesScreen() {
  const insets = useSafeAreaInsets();

  const [events, setEvents] = useState<Event[]>([]);
  const [currentEventId, setCurrentEventId] = useState<number>(1);
  const [fixtures, setFixtures] = useState<Fixture[]>([]);
  const [bootstrapData, setBootstrapData] = useState<BootstrapData | null>(null);
  const [liveData, setLiveData] = useState<LiveGameweek | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [isRefreshing, setIsRefreshing] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [gwDropdownOpen, setGwDropdownOpen] = useState(false);
  const [selectedFixture, setSelectedFixture] = useState<Fixture | null>(null);
  const [favoriteLeagueId, setFavoriteLeagueId] = useState<number | null>(null);
  const [favoriteLeagueName, setFavoriteLeagueName] = useState<string | null>(null);
  const [selectedPlayer, setSelectedPlayer] = useState<MatchDialogPlayer | null>(null);
  const [playerDetail, setPlayerDetail] = useState<PlayerDetailResponse | null>(null);
  const [leagueStats, setLeagueStats] = useState<BackendLeaguePlayerStats | null>(null);
  const [leagueStatsError, setLeagueStatsError] = useState<string | null>(null);
  const [isLoadingPlayerData, setIsLoadingPlayerData] = useState(false);

  const loadBootstrap = useCallback(async () => {
    const [bootstrap, storedFavoriteLeagueId, storedFavoriteLeagueName] = await Promise.all([
      Api.getBootstrapData(),
      Storage.getFavoriteLeagueId(),
      Storage.getFavoriteLeagueName(),
    ]);
    setBootstrapData(bootstrap);
    setFavoriteLeagueId(storedFavoriteLeagueId);
    setFavoriteLeagueName(storedFavoriteLeagueName);
    const currentEvent = bootstrap.events.find((e) => e.is_current)
      ?? bootstrap.events.find((e) => e.is_next)
      ?? bootstrap.events[0];
    setEvents(bootstrap.events);
    return { bootstrap, currentEvent };
  }, []);

  const loadGameweek = useCallback(async (eventId: number) => {
    const [fixturesList, live] = await Promise.all([
      Api.getFixturesByEvent(eventId),
      Api.getLiveGameweek(eventId).catch(() => null),
    ]);
    setFixtures(fixturesList);
    setLiveData(live);
  }, []);

  const loadAll = useCallback(async () => {
    try {
      const { currentEvent } = await loadBootstrap();
      const gwId = currentEvent?.id ?? 1;
      setCurrentEventId(gwId);
      await loadGameweek(gwId);
      setError(null);
    } catch (e: any) {
      setError(e.message ?? 'Failed to load');
    }
  }, []);

  useEffect(() => { loadAll().finally(() => setIsLoading(false)); }, []);

  const handleSelectGw = async (id: number) => {
    setGwDropdownOpen(false);
    setCurrentEventId(id);
    setIsLoading(true);
    try {
      await loadGameweek(id);
    } catch (e: any) {
      setError(e.message);
    } finally {
      setIsLoading(false);
    }
  };

  const handleRefresh = async () => {
    setIsRefreshing(true);
    try {
      await loadGameweek(currentEventId);
    } finally {
      setIsRefreshing(false);
    }
  };

  const handlePlayerPress = async (player: Player, fixture: Fixture) => {
    const teamInfo = teams.find((team) => team.id === player.team);
    const liveStats = liveMap.get(player.id);
    setSelectedPlayer({
      player,
      teamInfo,
      teamShortName: teamInfo?.short_name ?? '?',
      fixture,
      liveStats,
    });
    setPlayerDetail(null);
    setLeagueStats(null);
    setLeagueStatsError(null);
    setIsLoadingPlayerData(true);

    try {
      const [detail, statsResult] = await Promise.all([
        Api.getPlayerDetail(player.id).catch(() => null),
        favoriteLeagueId != null
          ? Api.getLeaguePlayerStats(favoriteLeagueId, currentEventId, player.id)
            .then((stats) => ({ stats, error: null as string | null }))
            .catch((err: Error) => ({ stats: null, error: err.message ?? 'League stats unavailable' }))
          : Promise.resolve({ stats: null, error: 'Choose a favorite league to compare league stats.' }),
      ]);

      setPlayerDetail(detail);
      setLeagueStats(statsResult.stats);
      setLeagueStatsError(statsResult.error);
    } finally {
      setIsLoadingPlayerData(false);
    }
  };

  const teams = bootstrapData?.teams ?? [];
  const players = bootstrapData?.elements ?? [];
  const liveMap = new Map<number, LiveElement>(
    liveData?.elements.map((e) => [e.id, e]) ?? [],
  );

  const liveCount = fixtures.filter(
    (f) => f.started && !f.finished && !f.finished_provisional,
  ).length;

  const selectedEvent = events.find((e) => e.id === currentEventId);

  if (isLoading && fixtures.length === 0) {
    return (
      <View style={styles.center}>
        <ActivityIndicator color={Colors.primary} size="large" />
      </View>
    );
  }

  return (
    <View style={[styles.root, { paddingTop: insets.top }]}>
      {/* ── Header ───────────────────────────────────────────────── */}
      <View style={styles.headerSection}>
        <Text style={styles.headerLabel}>MATCHDAY CENTRAL</Text>
        <View style={styles.headerRow}>
          <TouchableOpacity
            style={styles.gwSelector}
            onPress={() => setGwDropdownOpen(true)}
          >
            <Text style={styles.gwTitle}>
              {selectedEvent?.name ?? `GW ${currentEventId}`}
            </Text>
            <Text style={styles.gwChevron}>⌄</Text>
          </TouchableOpacity>

          <View style={styles.headerRight}>
            {liveCount > 0 && (
              <View style={styles.liveBadge}>
                <View style={styles.liveDot} />
                <Text style={styles.liveBadgeText}>{liveCount} LIVE</Text>
              </View>
            )}
            <TouchableOpacity onPress={handleRefresh}>
              <Text style={styles.refreshIcon}>↺</Text>
            </TouchableOpacity>
          </View>
        </View>
      </View>

      {/* ── Fixtures list ─────────────────────────────────────────── */}
      <FlatList
        data={fixtures}
        keyExtractor={(item) => String(item.id)}
        refreshControl={
          <RefreshControl
            refreshing={isRefreshing}
            onRefresh={handleRefresh}
            tintColor={Colors.primary}
          />
        }
        renderItem={({ item }) => (
          <FixtureCard
            fixture={item}
            teams={teams}
            players={players}
            liveMap={liveMap}
            onPress={() => setSelectedFixture(item)}
            onPlayerPress={(player) => handlePlayerPress(player, item)}
          />
        )}
        contentContainerStyle={{ paddingVertical: 8 }}
        showsVerticalScrollIndicator={false}
        ItemSeparatorComponent={() => <View style={{ height: 8 }} />}
      />

      {/* ── GW Picker modal ──────────────────────────────────────── */}
      <AppBottomSheet
        visible={gwDropdownOpen}
        onClose={() => setGwDropdownOpen(false)}
        snapPoints={['58%']}
        backgroundStyle={{ backgroundColor: Colors.surfaceHigh }}
      >
        <View style={styles.gwModalContent}>
          <View style={styles.gwModal}>
            <Text style={styles.gwModalTitle}>Select Gameweek</Text>
            <FlatList
              data={events}
              keyExtractor={(e) => String(e.id)}
              renderItem={({ item }) => (
                <TouchableOpacity
                  style={[styles.gwModalItem, item.id === currentEventId && styles.gwModalItemActive]}
                  onPress={() => handleSelectGw(item.id)}
                >
                  <Text style={[styles.gwModalItemText, item.id === currentEventId && { color: Colors.primary }]}>
                    {item.name}
                  </Text>
                  {(item.is_current || item.is_next) && (
                    <Text style={styles.gwModalItemBadge}>
                      {item.is_current ? 'CURRENT' : 'NEXT'}
                    </Text>
                  )}
                </TouchableOpacity>
              )}
            />
          </View>
        </View>
      </AppBottomSheet>

      {/* ── Fixture stats modal ───────────────────────────────────── */}
      {selectedFixture && (
        <FixtureStatsModal
          fixture={selectedFixture}
          teams={teams}
          players={players}
          liveMap={liveMap}
          onPlayerPress={handlePlayerPress}
          onClose={() => setSelectedFixture(null)}
        />
      )}

      {selectedPlayer && bootstrapData && (
        <PlayerLeagueModal
          player={selectedPlayer}
          detail={playerDetail}
          bootstrapData={bootstrapData}
          leagueStats={leagueStats}
          leagueStatsError={leagueStatsError}
          isLoadingLeagueStats={isLoadingPlayerData}
          currentEvent={currentEventId}
          selectedLeagueName={favoriteLeagueName}
          onClose={() => setSelectedPlayer(null)}
        />
      )}
    </View>
  );
}

// ── FixtureCard ────────────────────────────────────────────────────────────────
function FixtureCard({
  fixture, teams, players, liveMap, onPress, onPlayerPress,
}: {
  fixture: Fixture;
  teams: Team[];
  players: Player[];
  liveMap: Map<number, LiveElement>;
  onPress: () => void;
  onPlayerPress: (player: Player) => void;
}) {
  const homeTeam = teams.find((t) => t.id === fixture.team_h);
  const awayTeam = teams.find((t) => t.id === fixture.team_a);
  const isLive = fixture.started && !fixture.finished && !fixture.finished_provisional;
  const notStarted = !fixture.started;
  const statusColor = isLive ? Colors.primary : fixture.finished ? Colors.onSurfaceVariant : Colors.outline;
  const goalsStat = fixture.stats.find((stat) => stat.identifier === 'goals_scored');
  const ownGoalsStat = fixture.stats.find((stat) => stat.identifier === 'own_goals');
  const penaltiesScoredStat = fixture.stats.find((stat) => stat.identifier === 'penalties_scored');
  const homeGoalEntries = (isLive || fixture.finished)
    ? buildGoalEntries({
      scoringEntries: goalsStat?.h ?? [],
      ownGoalEntries: ownGoalsStat?.a ?? [],
      penaltyEntries: penaltiesScoredStat?.h ?? [],
      players,
    })
    : [];
  const awayGoalEntries = (isLive || fixture.finished)
    ? buildGoalEntries({
      scoringEntries: goalsStat?.a ?? [],
      ownGoalEntries: ownGoalsStat?.h ?? [],
      penaltyEntries: penaltiesScoredStat?.a ?? [],
      players,
    })
    : [];
  const hasGoals = homeGoalEntries.length > 0 || awayGoalEntries.length > 0;

  return (
    <TouchableOpacity style={fixtureStyles.card} onPress={onPress} activeOpacity={0.8}>
      {isLive && <View style={fixtureStyles.liveLine} />}
      <View style={fixtureStyles.body}>
        {/* Home team */}
        <View style={fixtureStyles.teamBlock}>
          <Text style={fixtureStyles.teamName}>{homeTeam?.short_name ?? '???'}</Text>
        </View>

        {/* Score / Time */}
        <View style={fixtureStyles.scoreBlock}>
          {notStarted ? (
            <>
              <Text style={fixtureStyles.kickoff}>{formatKickoff(fixture.kickoff_time)}</Text>
              <View style={fixtureStyles.vsBox}>
                <Text style={fixtureStyles.vsText}>VS</Text>
              </View>
            </>
          ) : (
            <View style={fixtureStyles.scoreRow}>
              <Text style={fixtureStyles.score}>{fixture.team_h_score ?? 0}</Text>
              <View style={[fixtureStyles.scoreDiv, { backgroundColor: statusColor }]} />
              <Text style={fixtureStyles.score}>{fixture.team_a_score ?? 0}</Text>
            </View>
          )}
          {isLive && (
            <View style={fixtureStyles.livePill}>
              <View style={fixtureStyles.livePillDot} />
              <Text style={fixtureStyles.livePillText}>LIVE</Text>
            </View>
          )}
          {fixture.finished && <Text style={fixtureStyles.finishedText}>FT</Text>}
        </View>

        {/* Away team */}
        <View style={[fixtureStyles.teamBlock, { alignItems: 'flex-end' }]}>
          <Text style={fixtureStyles.teamName}>{awayTeam?.short_name ?? '???'}</Text>
        </View>
      </View>

      {/* Full names */}
      <View style={fixtureStyles.fullNameRow}>
        <Text style={[fixtureStyles.fullName, fixtureStyles.fullNameLeft]} numberOfLines={1}>
          {homeTeam?.name}
        </Text>
        <Text style={[fixtureStyles.fullName, fixtureStyles.fullNameRight]} numberOfLines={1}>
          {awayTeam?.name}
        </Text>
      </View>

      {hasGoals && (
        <View style={fixtureStyles.scorerWrap}>
          <View style={fixtureStyles.scorerDivider} />
          <View style={fixtureStyles.scorerRow}>
            <View style={fixtureStyles.scorerCol}>
              {homeGoalEntries.map((entry, index) => (
                <View key={`home-${index}`} style={fixtureStyles.scorerItem}>
                  <Text style={fixtureStyles.scorerBall}>⚽</Text>
                  <TouchableOpacity onPress={() => {
                    const player = players.find((item) => item.id === entry.element);
                    if (player) onPlayerPress(player);
                  }}>
                    <Text style={fixtureStyles.scorerText}>
                      {entry.playerName}
                      {entry.count > 1 ? ` ×${entry.count}` : ''}
                      {entry.isOwnGoal ? ' (og)' : ''}
                      {entry.isPenalty ? ' (P)' : ''}
                    </Text>
                  </TouchableOpacity>
                </View>
              ))}
            </View>
            <View style={[fixtureStyles.scorerCol, fixtureStyles.scorerColRight]}>
              {awayGoalEntries.map((entry, index) => (
                <View key={`away-${index}`} style={[fixtureStyles.scorerItem, fixtureStyles.scorerItemRight]}>
                  <TouchableOpacity onPress={() => {
                    const player = players.find((item) => item.id === entry.element);
                    if (player) onPlayerPress(player);
                  }}>
                    <Text style={[fixtureStyles.scorerText, fixtureStyles.scorerTextRight]}>
                      {entry.playerName}
                      {entry.count > 1 ? ` ×${entry.count}` : ''}
                      {entry.isOwnGoal ? ' (og)' : ''}
                      {entry.isPenalty ? ' (P)' : ''}
                    </Text>
                  </TouchableOpacity>
                  <Text style={fixtureStyles.scorerBall}>⚽</Text>
                </View>
              ))}
            </View>
          </View>
        </View>
      )}
    </TouchableOpacity>
  );
}

// ── FixtureStatsModal ─────────────────────────────────────────────────────────
function FixtureStatsModal({
  fixture,
  teams,
  players,
  liveMap,
  onPlayerPress,
  onClose,
}: {
  fixture: Fixture;
  teams: Team[];
  players: Player[];
  liveMap: Map<number, LiveElement>;
  onPlayerPress: (player: Player, fixture: Fixture) => void;
  onClose: () => void;
}) {
  const homeTeam = teams.find((t) => t.id === fixture.team_h);
  const awayTeam = teams.find((t) => t.id === fixture.team_a);
  const getPlayerName = (id: number) =>
    players.find((p) => p.id === id)?.web_name ?? `#${id}`;

  const statLabels: Record<string, string> = {
    goals_scored: 'Goals',
    assists: 'Assists',
    yellow_cards: 'Yellow Cards',
    red_cards: 'Red Cards',
    saves: 'Saves',
    penalties_saved: 'Pen Saved',
    penalties_missed: 'Pen Missed',
    own_goals: 'Own Goals',
    bonus: 'Bonus',
  };

  const activeStats = fixture.stats.filter((s) => s.h.length > 0 || s.a.length > 0);

  return (
    <AppBottomSheet
      visible
      onClose={onClose}
      snapPoints={['82%']}
      backgroundStyle={{ backgroundColor: Colors.surface }}
    >
      <View style={modalStyles.sheet}>
        <View style={modalStyles.header}>
          <Text style={modalStyles.teamH}>{homeTeam?.short_name ?? '?'}</Text>
          <Text style={modalStyles.score}>
            {fixture.team_h_score ?? 0} – {fixture.team_a_score ?? 0}
          </Text>
          <Text style={modalStyles.teamA}>{awayTeam?.short_name ?? '?'}</Text>
        </View>

        <View style={modalStyles.difficultyRow}>
          <View style={modalStyles.difficultySide}>
            <Text style={modalStyles.difficultyLabel}>{homeTeam?.short_name ?? 'HOME'} Difficulty</Text>
            <View
              style={[
                modalStyles.difficultyPill,
                { backgroundColor: getDifficultyTheme(fixture.team_h_difficulty).backgroundColor },
              ]}
            >
              <Text
                style={[
                  modalStyles.difficultyValue,
                  { color: getDifficultyTheme(fixture.team_h_difficulty).color },
                ]}
              >
                {fixture.team_h_difficulty}
              </Text>
            </View>
          </View>
          <View style={[modalStyles.difficultySide, modalStyles.difficultySideRight]}>
            <Text style={[modalStyles.difficultyLabel, { textAlign: 'right' }]}>
              {awayTeam?.short_name ?? 'AWAY'} Difficulty
            </Text>
            <View
              style={[
                modalStyles.difficultyPill,
                modalStyles.difficultyPillRight,
                { backgroundColor: getDifficultyTheme(fixture.team_a_difficulty).backgroundColor },
              ]}
            >
              <Text
                style={[
                  modalStyles.difficultyValue,
                  { color: getDifficultyTheme(fixture.team_a_difficulty).color },
                ]}
              >
                {fixture.team_a_difficulty}
              </Text>
            </View>
          </View>
        </View>

        <TouchableOpacity style={modalStyles.closeBtn} onPress={onClose}>
          <Text style={modalStyles.closeBtnText}>✕</Text>
        </TouchableOpacity>

        <ScrollView showsVerticalScrollIndicator={false}>
          {activeStats.length === 0 ? (
            <Text style={modalStyles.noStats}>No stats available yet</Text>
          ) : (
            activeStats.map((stat) => (
              <View key={stat.identifier} style={modalStyles.statSection}>
                <Text style={modalStyles.statLabel}>
                  {statLabels[stat.identifier] ?? stat.identifier}
                </Text>
                <View style={modalStyles.statColumns}>
                  <View style={modalStyles.statCol}>
                    {stat.h.map((sv, i) => (
                      <TouchableOpacity
                        key={i}
                        onPress={() => {
                          const player = players.find((item) => item.id === sv.element);
                          if (player) onPlayerPress(player, fixture);
                        }}
                      >
                        <Text style={modalStyles.statPlayer}>
                          {getPlayerName(sv.element)} {sv.value > 1 ? `×${sv.value}` : ''}
                        </Text>
                      </TouchableOpacity>
                    ))}
                  </View>
                  <View style={modalStyles.statCol}>
                    {stat.a.map((sv, i) => (
                      <TouchableOpacity
                        key={i}
                        onPress={() => {
                          const player = players.find((item) => item.id === sv.element);
                          if (player) onPlayerPress(player, fixture);
                        }}
                      >
                        <Text key={i} style={[modalStyles.statPlayer, { textAlign: 'right' }]}>
                          {sv.value > 1 ? `×${sv.value} ` : ''}{getPlayerName(sv.element)}
                        </Text>
                      </TouchableOpacity>
                    ))}
                  </View>
                </View>
              </View>
            ))
          )}
          <View style={{ height: 24 }} />
        </ScrollView>
      </View>
    </AppBottomSheet>
  );
}

function getPositionName(elementType: number): string {
  switch (elementType) {
    case 1: return 'Goalkeeper';
    case 2: return 'Defender';
    case 3: return 'Midfielder';
    case 4: return 'Forward';
    default: return 'Player';
  }
}

function getTeamBadgeColor(teamId?: number): string {
  switch (teamId) {
    case 1: return '#EF0107';
    case 2: return '#95BFE5';
    case 3: return '#E62333';
    case 4: return '#E30613';
    case 5: return '#0057B8';
    case 6: return '#034694';
    case 7: return '#1B458F';
    case 8: return '#003399';
    case 9: return '#000000';
    case 10: return '#0057B8';
    case 11: return '#003090';
    case 12: return '#C8102E';
    case 13: return '#6CABDD';
    case 14: return '#DA291C';
    case 15: return '#241F20';
    case 16: return '#E53233';
    case 17: return '#D71920';
    case 18: return '#132257';
    case 19: return '#7A263A';
    case 20: return '#FDB913';
    default: return Colors.primaryContainer;
  }
}

function getTeamBadgeTextColor(teamId?: number): string {
  if (teamId === 9 || teamId === 15) return '#FFFFFF';
  if (teamId === 13 || teamId === 20) return '#131313';
  return '#FFFFFF';
}

function formatPercent(value: number | string | undefined): string {
  const numeric = typeof value === 'string' ? parseFloat(value) : value;
  if (numeric == null || Number.isNaN(numeric)) return '0.0%';
  return `${numeric.toFixed(1)}%`;
}

function sortManagers(managers: LeagueManagerRef[] = []): LeagueManagerRef[] {
  return [...managers].sort((a, b) => a.rank - b.rank || a.entryName.localeCompare(b.entryName));
}

function PlayerLeagueModal({
  player,
  detail,
  bootstrapData,
  leagueStats,
  leagueStatsError,
  isLoadingLeagueStats,
  currentEvent,
  selectedLeagueName,
  onClose,
}: {
  player: MatchDialogPlayer;
  detail: PlayerDetailResponse | null;
  bootstrapData: BootstrapData;
  leagueStats: BackendLeaguePlayerStats | null;
  leagueStatsError: string | null;
  isLoadingLeagueStats: boolean;
  currentEvent: number;
  selectedLeagueName: string | null;
  onClose: () => void;
}) {
  const [selectedTab, setSelectedTab] = useState<'summary' | 'starts' | 'bench'>('summary');
  const [showAllPrevious, setShowAllPrevious] = useState(false);
  const [showAllUpcoming, setShowAllUpcoming] = useState(false);

  useEffect(() => {
    setSelectedTab('summary');
    setShowAllPrevious(false);
    setShowAllUpcoming(false);
  }, [player.player.id]);

  const team = player.teamInfo;
  const teamShortName = team?.short_name ?? player.teamShortName;
  const selectedHistory =
    detail?.history.find((entry) => entry.round === currentEvent) ??
    detail?.history.slice(-1)[0] ??
    null;
  const allPreviousFixtures = detail?.history.filter((entry) => entry.round <= currentEvent) ?? [];
  const allUpcomingFixtures = detail?.fixtures.filter((fixture) => !fixture.finished && fixture.event != null && fixture.event > currentEvent) ?? [];
  const previousFixtures = showAllPrevious ? allPreviousFixtures : allPreviousFixtures.slice(-5);
  const upcomingFixtures = showAllUpcoming ? allUpcomingFixtures : allUpcomingFixtures.slice(0, 5);
  const explainStats = player.liveStats?.explain.flatMap((entry) => entry.stats) ?? [];
  const defensiveContribution = explainStats.find((stat) => stat.identifier.toLowerCase().includes('def'));
  const defensiveThreshold = player.player.element_type === 2 ? 10 : 12;
  const defensiveActions = defensiveContribution?.value ?? 0;
  const defensiveBonusPoints = defensiveActions >= defensiveThreshold ? 2 : 0;
  const captainedBy = sortManagers(leagueStats?.captainedBy);
  const startedBy = sortManagers(
    leagueStats?.startedBy.filter((manager) => !captainedBy.some((captained) => captained.rank === manager.rank && captained.entryName === manager.entryName)) ?? [],
  );
  const benchedBy = sortManagers(leagueStats?.benchedBy);
  const leagueLabel = selectedLeagueName ?? 'your favorite league';
  const displayMinutes = player.fixture && player.fixture.started === true && !player.fixture.finished
    ? player.fixture.minutes
    : (selectedHistory?.minutes ?? 0);

  return (
    <AppBottomSheet
      visible
      onClose={onClose}
      snapPoints={['92%']}
      stackBehavior="push"
    >
      <View style={playerModalStyles.sheet}>
        <View style={playerModalStyles.header}>
          <View style={[playerModalStyles.teamBadge, { backgroundColor: getTeamBadgeColor(team?.id) }]}>
            <Text style={[playerModalStyles.teamBadgeText, { color: getTeamBadgeTextColor(team?.id) }]}>
              {teamShortName.toUpperCase()}
            </Text>
          </View>
          <View style={playerModalStyles.headerCopy}>
            <Text style={playerModalStyles.name}>{player.player.web_name}</Text>
            <Text style={playerModalStyles.meta}>{teamShortName} · {getPositionName(player.player.element_type)}</Text>
            <Text style={playerModalStyles.caption}>Favorite league · GW{currentEvent}</Text>
          </View>
          <TouchableOpacity onPress={onClose} style={playerModalStyles.closeBtn}>
            <Text style={playerModalStyles.closeBtnText}>×</Text>
          </TouchableOpacity>
        </View>

        <View style={playerModalStyles.tabRow}>
          {(['summary', 'starts', 'bench'] as const).map((tab) => (
            <TouchableOpacity key={tab} style={playerModalStyles.tabButton} onPress={() => setSelectedTab(tab)}>
              <Text style={[playerModalStyles.tabLabel, selectedTab === tab && playerModalStyles.tabLabelActive]}>
                {tab === 'summary' ? 'Summary' : tab === 'starts' ? `Starts${leagueStats ? ` (${leagueStats.startsCount})` : ''}` : `Bench${leagueStats ? ` (${leagueStats.benchCount})` : ''}`}
              </Text>
              <View style={[playerModalStyles.tabIndicator, selectedTab === tab && playerModalStyles.tabIndicatorActive]} />
            </TouchableOpacity>
          ))}
        </View>

        <ScrollView showsVerticalScrollIndicator={false} contentContainerStyle={playerModalStyles.scrollContent}>
          {selectedTab === 'summary' && (
            <>
              <View style={playerModalStyles.infoCard}>
                <Text style={playerModalStyles.infoText}>
                  League stats on this dialog are for {leagueLabel} and GW{currentEvent} only.
                </Text>
              </View>
              {selectedHistory ? (
                <>
                  <View style={playerModalStyles.sectionCard}>
                    <Text style={playerModalStyles.sectionTitle}>LATEST MATCH</Text>
                    <PlayerRowLine label={`Defensive contributions (${defensiveActions}):`} value={defensiveBonusPoints > 0 ? `+${defensiveBonusPoints}` : '0'} />
                    {selectedHistory.goals_scored > 0 && <PlayerRowLine label={`${selectedHistory.goals_scored} goals:`} value={String(selectedHistory.goals_scored * (player.player.element_type === 4 ? 4 : player.player.element_type === 3 ? 5 : 6))} />}
                    {selectedHistory.assists > 0 && <PlayerRowLine label={`${selectedHistory.assists} assists:`} value={String(selectedHistory.assists * 3)} />}
                    {selectedHistory.goals_conceded >= 2 && (player.player.element_type === 1 || player.player.element_type === 2) && <PlayerRowLine label={`Goals conceded (${selectedHistory.goals_conceded}):`} value={String(-Math.floor(selectedHistory.goals_conceded / 2))} />}
                    <PlayerRowLine label={`Played ${displayMinutes} min:`} value={String(displayMinutes === 0 ? 0 : displayMinutes >= 60 ? 2 : 1)} />
                    <PlayerRowLine label={`Bonus (${selectedHistory.bps} bps):`} value={String(selectedHistory.bonus)} />
                    <View style={playerModalStyles.divider} />
                    <PlayerRowLine label="Total Points:" value={String(selectedHistory.total_points)} emphasize />
                  </View>
                  <View style={playerModalStyles.sectionCard}>
                    <Text style={playerModalStyles.sectionTitle}>OWNERSHIP</Text>
                    <PlayerRowLine label="Starts league:" value={formatPercent(leagueStats?.startsPercentage)} />
                    <PlayerRowLine label="Owned league:" value={formatPercent(leagueStats?.ownedPercentage)} />
                    <PlayerRowLine label="Owned overall:" value={formatPercent(player.player.selected_by_percent)} />
                    <PlayerRowLine label="Captain count:" value={String(leagueStats?.captainCount ?? 0)} />
                  </View>
                </>
              ) : (
                <Text style={playerModalStyles.emptyText}>{isLoadingLeagueStats ? 'Loading player data…' : 'No player details available.'}</Text>
              )}
              {previousFixtures.length > 0 && (
                <FixtureTableCard
                  title="PREVIOUS FIXTURES"
                  fixtures={previousFixtures.map((match) => ({
                    key: `${match.fixture}-${match.round}`,
                    gw: String(match.round),
                    opp: `${bootstrapData.teams.find((entry) => entry.id === match.opponent_team)?.short_name ?? 'OPP'} ${match.was_home ? '(H)' : '(A)'}`,
                    third: String(match.minutes),
                    fourth: String(match.total_points),
                    fourthTheme: { color: Colors.primary },
                  }))}
                  headers={['GW', 'Opp', 'Min', 'Pts']}
                  showAll={allPreviousFixtures.length > 5 ? () => setShowAllPrevious((v) => !v) : undefined}
                  expanded={showAllPrevious}
                  total={allPreviousFixtures.length}
                />
              )}
              {upcomingFixtures.length > 0 && (
                <FixtureTableCard
                  title="UPCOMING FIXTURES"
                  fixtures={upcomingFixtures.map((fixture) => {
                    const opponentTeamId = fixture.is_home ? fixture.team_a : fixture.team_h;
                    const opponent = bootstrapData.teams.find((entry) => entry.id === opponentTeamId)?.short_name ?? 'TBD';
                    return {
                      key: `${fixture.id}-${fixture.event}`,
                      gw: String(fixture.event),
                      opp: `${opponent} ${fixture.is_home ? '(H)' : '(A)'}`,
                      third: '',
                      fourth: String(fixture.difficulty),
                      fourthTheme: getDifficultyTheme(fixture.difficulty),
                    };
                  })}
                  headers={['GW', 'Opp', '', 'Diff']}
                  showAll={allUpcomingFixtures.length > 5 ? () => setShowAllUpcoming((v) => !v) : undefined}
                  expanded={showAllUpcoming}
                  total={allUpcomingFixtures.length}
                  difficulty
                />
              )}
            </>
          )}
          {selectedTab === 'starts' && (
            <ManagerListTab
              title="TEAMS WHO STARTED THIS PLAYER"
              managers={startedBy}
              captainedBy={captainedBy}
              emptyText={leagueStatsError ?? 'No teams in this league started this player.'}
              isLoading={isLoadingLeagueStats}
            />
          )}
          {selectedTab === 'bench' && (
            <BenchListTab
              managers={benchedBy}
              benchCount={leagueStats?.benchCount ?? 0}
              emptyText={leagueStatsError ?? 'No teams in this league benched this player.'}
              isLoading={isLoadingLeagueStats}
            />
          )}
        </ScrollView>

        <TouchableOpacity style={playerModalStyles.closeAction} onPress={onClose}>
          <Text style={playerModalStyles.closeActionText}>CLOSE</Text>
        </TouchableOpacity>
      </View>
    </AppBottomSheet>
  );
}

function PlayerRowLine({ label, value, emphasize = false }: { label: string; value: string; emphasize?: boolean }) {
  return (
    <View style={playerModalStyles.dataRow}>
      <Text style={[playerModalStyles.dataLabel, emphasize && playerModalStyles.dataLabelStrong]}>{label}</Text>
      <Text style={[playerModalStyles.dataValue, emphasize && playerModalStyles.dataValueStrong]}>{value}</Text>
    </View>
  );
}

function FixtureTableCard({
  title,
  fixtures,
  headers,
  showAll,
  expanded,
  total,
  difficulty = false,
}: {
  title: string;
  fixtures: { key: string; gw: string; opp: string; third: string; fourth: string; fourthTheme?: { backgroundColor: string; color: string } | { color: string } }[];
  headers: [string, string, string, string] | string[];
  showAll?: () => void;
  expanded: boolean;
  total: number;
  difficulty?: boolean;
}) {
  return (
    <View style={playerModalStyles.sectionCard}>
      <Text style={playerModalStyles.sectionTitle}>{title}</Text>
      <View style={playerModalStyles.tableHeader}>
        <Text style={[playerModalStyles.tableHeaderCell, playerModalStyles.colGw]}>{headers[0]}</Text>
        <Text style={[playerModalStyles.tableHeaderCell, playerModalStyles.colOpp]}>{headers[1]}</Text>
        <Text style={[playerModalStyles.tableHeaderCell, playerModalStyles.colThird]}>{headers[2]}</Text>
        <Text style={[playerModalStyles.tableHeaderCell, playerModalStyles.colFourth]}>{headers[3]}</Text>
      </View>
      <View style={playerModalStyles.tableDivider} />
      {fixtures.map((row) => (
        <View key={row.key} style={playerModalStyles.tableRow}>
          <Text style={[playerModalStyles.tableCell, playerModalStyles.colGwValue]}>{row.gw}</Text>
          <Text style={[playerModalStyles.tableCell, playerModalStyles.colOppValue]}>{row.opp}</Text>
          <Text style={[playerModalStyles.tableCell, playerModalStyles.colThirdValue]}>{row.third}</Text>
          <View style={playerModalStyles.colFourthValue}>
            {difficulty && row.fourthTheme && 'backgroundColor' in row.fourthTheme ? (
              <View style={[playerModalStyles.difficultyPill, { backgroundColor: row.fourthTheme.backgroundColor }]}>
                <Text style={[playerModalStyles.difficultyText, { color: row.fourthTheme.color }]}>{row.fourth}</Text>
              </View>
            ) : (
              <Text style={[playerModalStyles.tableCell, playerModalStyles.colFourthText, row.fourthTheme ? { color: row.fourthTheme.color } : null]}>{row.fourth}</Text>
            )}
          </View>
        </View>
      ))}
      {showAll && (
        <TouchableOpacity style={playerModalStyles.expandButton} onPress={showAll}>
          <Text style={playerModalStyles.expandButtonText}>{expanded ? 'Show less' : `Show all ${total}`}</Text>
        </TouchableOpacity>
      )}
    </View>
  );
}

function ManagerListTab({
  title,
  managers,
  captainedBy,
  emptyText,
  isLoading,
}: {
  title: string;
  managers: LeagueManagerRef[];
  captainedBy: LeagueManagerRef[];
  emptyText: string;
  isLoading: boolean;
}) {
  if (isLoading && managers.length === 0 && captainedBy.length === 0) {
    return <ActivityIndicator color={Colors.primary} style={{ marginTop: 20 }} />;
  }
  return (
    <>
      <Text style={playerModalStyles.tabSectionTitle}>{title}</Text>
      {captainedBy.map((manager) => (
        <ManagerBadge key={`captain-${manager.rank}-${manager.entryName}`} manager={manager} icon="★" captain />
      ))}
      {managers.map((manager) => (
        <ManagerBadge key={`start-${manager.rank}-${manager.entryName}`} manager={manager} icon="✓" />
      ))}
      {captainedBy.length === 0 && managers.length === 0 && <Text style={playerModalStyles.emptyText}>{emptyText}</Text>}
    </>
  );
}

function BenchListTab({
  managers,
  benchCount,
  emptyText,
  isLoading,
}: {
  managers: LeagueManagerRef[];
  benchCount: number;
  emptyText: string;
  isLoading: boolean;
}) {
  if (isLoading && managers.length === 0) {
    return <ActivityIndicator color={Colors.primary} style={{ marginTop: 20 }} />;
  }
  return (
    <>
      <Text style={[playerModalStyles.tabSectionTitle, { color: Colors.tertiary }]}>TEAMS WHO BENCHED THIS PLAYER</Text>
      {benchCount > 0 && (
        <View style={playerModalStyles.benchSummaryCard}>
          <Text style={playerModalStyles.benchSummaryTitle}>Benched Count: {benchCount}</Text>
          <Text style={playerModalStyles.benchSummaryText}>These managers own this player but have them on the bench.</Text>
        </View>
      )}
      {managers.map((manager) => (
        <ManagerBadge key={`bench-${manager.rank}-${manager.entryName}`} manager={manager} icon="B" bench />
      ))}
      {managers.length === 0 && <Text style={playerModalStyles.emptyText}>{emptyText}</Text>}
    </>
  );
}

function ManagerBadge({
  manager,
  icon,
  captain = false,
  bench = false,
}: {
  manager: LeagueManagerRef;
  icon: string;
  captain?: boolean;
  bench?: boolean;
}) {
  return (
    <View style={[
      playerModalStyles.managerCard,
      captain && playerModalStyles.managerCardCaptain,
      bench && playerModalStyles.managerCardBench,
    ]}>
      <Text style={playerModalStyles.managerIcon}>{icon}</Text>
      <Text style={playerModalStyles.managerText}>#{manager.rank} {manager.entryName}</Text>
    </View>
  );
}

// ── Styles ─────────────────────────────────────────────────────────────────────
const fixtureStyles = StyleSheet.create({
  card: {
    backgroundColor: Colors.surface,
    borderRadius: Radius.lg,
    marginHorizontal: 12,
    overflow: 'hidden',
  },
  liveLine: {
    height: 2,
    backgroundColor: Colors.primary,
  },
  body: {
    flexDirection: 'row',
    alignItems: 'center',
    padding: 16,
    gap: 8,
  },
  teamBlock: {
    flex: 1,
    alignItems: 'flex-start',
  },
  teamName: {
    fontSize: 18, fontWeight: '800',
    color: Colors.onSurface,
    letterSpacing: -0.5,
  },
  scoreBlock: {
    alignItems: 'center',
    minWidth: 80,
    gap: 4,
  },
  kickoff: {
    fontSize: 10,
    color: Colors.outline,
    textAlign: 'center',
    marginBottom: 2,
  },
  vsBox: {
    backgroundColor: Colors.surfaceHigh,
    borderRadius: Radius.sm,
    paddingHorizontal: 10,
    paddingVertical: 4,
  },
  vsText: {
    fontSize: 12, fontWeight: '700',
    color: Colors.outline,
  },
  scoreRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 6,
  },
  score: {
    fontSize: 28, fontWeight: '900',
    color: Colors.onSurface,
    letterSpacing: -1,
  },
  scoreDiv: {
    width: 3, height: 3,
    borderRadius: 1.5,
  },
  livePill: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: Colors.primaryContainer + '66',
    borderRadius: Radius.full,
    paddingHorizontal: 8,
    paddingVertical: 3,
    gap: 4,
  },
  livePillDot: {
    width: 5, height: 5,
    borderRadius: 2.5,
    backgroundColor: Colors.primary,
  },
  livePillText: {
    fontSize: 9, fontWeight: '800',
    color: Colors.primary,
    letterSpacing: 1,
  },
  finishedText: {
    fontSize: 9, fontWeight: '700',
    color: Colors.outline,
    letterSpacing: 1,
  },
  fullNameRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    paddingHorizontal: 16,
    paddingBottom: 10,
  },
  fullName: {
    fontSize: 11,
    color: Colors.outline,
    flex: 1,
  },
  fullNameLeft: {
    textAlign: 'left',
    paddingRight: 12,
  },
  fullNameRight: {
    textAlign: 'right',
    paddingLeft: 12,
  },
  scorerWrap: {
    paddingBottom: 12,
  },
  scorerDivider: {
    height: 1,
    backgroundColor: Colors.outlineVariant + '33',
    marginHorizontal: 16,
    marginBottom: 10,
  },
  scorerRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    paddingHorizontal: 16,
    gap: 10,
  },
  scorerCol: {
    flex: 1,
    gap: 6,
  },
  scorerColRight: {
    alignItems: 'flex-end',
  },
  scorerItem: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 4,
  },
  scorerItemRight: {
    justifyContent: 'flex-end',
  },
  scorerBall: {
    fontSize: 11,
  },
  scorerText: {
    fontSize: 11,
    fontWeight: '500',
    color: Colors.onSurfaceVariant,
  },
  scorerTextRight: {
    textAlign: 'right',
  },
});

const modalStyles = StyleSheet.create({
  sheet: {
    flex: 1,
    backgroundColor: Colors.surface,
    paddingHorizontal: 20,
    paddingBottom: 24,
  },
  header: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    gap: 16,
    marginBottom: 20,
  },
  teamH: {
    fontSize: 24, fontWeight: '800',
    color: Colors.onSurface,
  },
  score: {
    fontSize: 20, fontWeight: '900',
    color: Colors.primary,
  },
  teamA: {
    fontSize: 24, fontWeight: '800',
    color: Colors.onSurface,
  },
  closeBtn: {
    position: 'absolute',
    right: 16, top: 10,
    padding: 8,
  },
  closeBtnText: {
    fontSize: 16,
    color: Colors.outline,
  },
  difficultyRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    gap: 12,
    marginBottom: 20,
  },
  difficultySide: {
    flex: 1,
    gap: 6,
  },
  difficultySideRight: {
    alignItems: 'flex-end',
  },
  difficultyLabel: {
    fontSize: 11,
    fontWeight: '700',
    color: Colors.outline,
  },
  difficultyPill: {
    alignSelf: 'flex-start',
    minWidth: 38,
    borderRadius: 8,
    paddingHorizontal: 12,
    paddingVertical: 7,
    alignItems: 'center',
  },
  difficultyPillRight: {
    alignSelf: 'flex-end',
  },
  difficultyValue: {
    fontSize: 13,
    fontWeight: '900',
  },
  noStats: {
    fontSize: 13,
    color: Colors.outline,
    textAlign: 'center',
    padding: 24,
  },
  statSection: {
    marginBottom: 12,
  },
  statLabel: {
    fontSize: 10, fontWeight: '800',
    color: Colors.primary,
    letterSpacing: 2,
    marginBottom: 6,
  },
  statColumns: {
    flexDirection: 'row',
    gap: 8,
  },
  statCol: {
    flex: 1,
    gap: 3,
  },
  statPlayer: {
    fontSize: 12, fontWeight: '600',
    color: Colors.onSurface,
  },
});

const playerModalStyles = StyleSheet.create({
  sheet: {
    flex: 1,
    backgroundColor: Colors.surfaceContainer,
    overflow: 'hidden',
  },
  header: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: 16,
    paddingBottom: 14,
    gap: 12,
    backgroundColor: Colors.surfaceHigh,
  },
  teamBadge: {
    width: 44,
    height: 44,
    borderRadius: 10,
    alignItems: 'center',
    justifyContent: 'center',
  },
  teamBadgeText: {
    fontSize: 10,
    fontWeight: '900',
  },
  headerCopy: {
    flex: 1,
  },
  name: {
    fontSize: 20,
    fontWeight: '900',
    color: Colors.onSurface,
  },
  meta: {
    fontSize: 12,
    color: Colors.onSurfaceVariant,
    marginTop: 2,
  },
  caption: {
    fontSize: 11,
    color: Colors.outline,
    marginTop: 2,
  },
  closeBtn: {
    width: 36,
    height: 36,
    borderRadius: 18,
    backgroundColor: Colors.surfaceHighest,
    alignItems: 'center',
    justifyContent: 'center',
  },
  closeBtnText: {
    fontSize: 20,
    color: Colors.onSurfaceVariant,
    lineHeight: 20,
  },
  tabRow: {
    flexDirection: 'row',
    paddingHorizontal: 12,
    borderBottomWidth: 1,
    borderBottomColor: Colors.outlineVariant + '33',
  },
  tabButton: {
    flex: 1,
    alignItems: 'center',
    paddingTop: 12,
    paddingBottom: 8,
  },
  tabLabel: {
    fontSize: 12,
    fontWeight: '600',
    color: Colors.outline,
  },
  tabLabelActive: {
    color: Colors.primary,
  },
  tabIndicator: {
    height: 3,
    width: '100%',
    borderRadius: 999,
    marginTop: 10,
    backgroundColor: 'transparent',
  },
  tabIndicatorActive: {
    backgroundColor: Colors.primary,
  },
  scrollContent: {
    padding: 16,
    gap: 12,
    paddingBottom: 24,
  },
  infoCard: {
    backgroundColor: Colors.primaryContainer + '55',
    borderRadius: 12,
    padding: 14,
  },
  infoText: {
    fontSize: 12,
    lineHeight: 18,
    color: Colors.primary,
    fontWeight: '600',
  },
  sectionCard: {
    backgroundColor: Colors.surface,
    borderRadius: 14,
    padding: 16,
  },
  sectionTitle: {
    fontSize: 12,
    fontWeight: '800',
    color: Colors.onSurfaceVariant,
    letterSpacing: 1.5,
    marginBottom: 10,
  },
  dataRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    gap: 12,
    paddingVertical: 4,
  },
  dataLabel: {
    flex: 1,
    fontSize: 13,
    color: Colors.onSurfaceVariant,
  },
  dataLabelStrong: {
    color: Colors.onSurface,
    fontWeight: '700',
  },
  dataValue: {
    fontSize: 14,
    fontWeight: '600',
    color: Colors.onSurface,
  },
  dataValueStrong: {
    color: Colors.primary,
    fontWeight: '800',
  },
  divider: {
    height: 1,
    backgroundColor: Colors.outlineVariant + '55',
    marginVertical: 10,
  },
  tableHeader: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  tableHeaderCell: {
    fontSize: 11,
    fontWeight: '700',
    color: Colors.outline,
  },
  tableDivider: {
    height: 1,
    backgroundColor: Colors.outlineVariant + '55',
    marginVertical: 8,
  },
  tableRow: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingVertical: 6,
  },
  tableCell: {
    fontSize: 13,
    color: Colors.onSurface,
  },
  colGw: { width: 34 },
  colOpp: { flex: 1 },
  colThird: { width: 44, textAlign: 'center' },
  colFourth: { width: 50, textAlign: 'right' },
  colGwValue: { width: 34 },
  colOppValue: { flex: 1, color: Colors.onSurfaceVariant },
  colThirdValue: { width: 44, textAlign: 'center', color: Colors.onSurfaceVariant },
  colFourthValue: { width: 50, alignItems: 'center' },
  colFourthText: { fontWeight: '800' },
  difficultyPill: {
    minWidth: 34,
    borderRadius: 6,
    paddingHorizontal: 10,
    paddingVertical: 6,
    alignItems: 'center',
  },
  difficultyText: {
    fontSize: 12,
    fontWeight: '800',
  },
  expandButton: {
    marginTop: 10,
    alignSelf: 'flex-start',
    paddingVertical: 6,
    paddingHorizontal: 10,
    borderRadius: 999,
    backgroundColor: Colors.surfaceHigh,
  },
  expandButtonText: {
    fontSize: 12,
    fontWeight: '700',
    color: Colors.primary,
  },
  tabSectionTitle: {
    fontSize: 13,
    fontWeight: '800',
    letterSpacing: 1,
    color: Colors.primary,
    marginBottom: 8,
  },
  managerCard: {
    backgroundColor: Colors.surface,
    borderRadius: 12,
    padding: 12,
    marginBottom: 8,
    flexDirection: 'row',
    alignItems: 'center',
    gap: 10,
  },
  managerCardCaptain: {
    backgroundColor: '#3C2F00AA',
  },
  managerCardBench: {
    backgroundColor: '#A402172E',
  },
  managerIcon: {
    width: 18,
    textAlign: 'center',
    fontSize: 14,
    fontWeight: '800',
    color: Colors.onSurface,
  },
  managerText: {
    flex: 1,
    fontSize: 13,
    color: Colors.onSurface,
    fontWeight: '600',
  },
  benchSummaryCard: {
    backgroundColor: Colors.tertiaryContainer + '33',
    borderRadius: 12,
    padding: 16,
    marginBottom: 10,
  },
  benchSummaryTitle: {
    fontSize: 15,
    fontWeight: '800',
    color: Colors.tertiary,
  },
  benchSummaryText: {
    fontSize: 12,
    color: Colors.outline,
    marginTop: 6,
    lineHeight: 18,
  },
  emptyText: {
    fontSize: 13,
    color: Colors.outline,
    textAlign: 'center',
    marginTop: 12,
  },
  closeAction: {
    marginHorizontal: 16,
    marginBottom: 16,
    backgroundColor: Colors.primaryContainer,
    borderRadius: 12,
    paddingVertical: 14,
    alignItems: 'center',
  },
  closeActionText: {
    fontSize: 14,
    fontWeight: '800',
    color: Colors.primary,
    letterSpacing: 1,
  },
});

const styles = StyleSheet.create({
  root: {
    flex: 1,
    backgroundColor: Colors.background,
  },
  center: {
    flex: 1,
    backgroundColor: Colors.background,
    justifyContent: 'center',
    alignItems: 'center',
  },
  headerSection: {
    paddingHorizontal: 16,
    paddingTop: 4,
    paddingBottom: 8,
  },
  headerLabel: {
    fontSize: 10, fontWeight: '800',
    color: Colors.primary,
    letterSpacing: 3,
    marginBottom: 4,
  },
  headerRow: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
  },
  gwSelector: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 6,
  },
  gwTitle: {
    fontSize: 32, fontWeight: '900',
    color: Colors.onSurface,
    letterSpacing: -1,
  },
  gwChevron: {
    fontSize: 22,
    color: Colors.onSurfaceVariant,
  },
  headerRight: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 10,
  },
  liveBadge: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: Colors.surface,
    borderRadius: Radius.md,
    paddingHorizontal: 10,
    paddingVertical: 6,
    gap: 6,
  },
  liveDot: {
    width: 6, height: 6,
    borderRadius: 3,
    backgroundColor: Colors.primary,
  },
  liveBadgeText: {
    fontSize: 10, fontWeight: '800',
    color: Colors.primary,
    letterSpacing: 1,
  },
  refreshIcon: {
    fontSize: 20,
    color: Colors.outline,
  },
  gwModalContent: {
    flex: 1,
    paddingHorizontal: 18,
    paddingBottom: 18,
  },
  gwModal: {
    flex: 1,
    backgroundColor: Colors.surfaceHigh,
    borderRadius: 20,
    padding: 16,
  },
  gwModalTitle: {
    fontSize: 14, fontWeight: '700',
    color: Colors.onSurface,
    marginBottom: 12,
    textAlign: 'center',
  },
  gwModalItem: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingVertical: 12,
    paddingHorizontal: 8,
    borderBottomWidth: 1,
    borderBottomColor: Colors.outlineVariant + '33',
  },
  gwModalItemActive: {
    backgroundColor: Colors.primaryContainer + '33',
    borderRadius: Radius.sm,
  },
  gwModalItemText: {
    fontSize: 13, fontWeight: '600',
    color: Colors.onSurface,
  },
  gwModalItemBadge: {
    fontSize: 9, fontWeight: '800',
    color: Colors.outline,
    letterSpacing: 1,
  },
});
