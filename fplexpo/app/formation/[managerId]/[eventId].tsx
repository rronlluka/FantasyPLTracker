/**
 * Manager Formation Screen (ManagerFormationScreen.kt port)
 * Shows team formation on a pitch with live points.
 */
import React, { useEffect, useState, useCallback, useRef } from 'react';
import {
  View, Text, TouchableOpacity, ActivityIndicator,
  StyleSheet, ScrollView,
} from 'react-native';
import { BottomSheetScrollView } from '@gorhom/bottom-sheet';
import { useLocalSearchParams, useRouter } from 'expo-router';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { Api } from '@/services/api';
import {
  ManagerPicks, BootstrapData, LiveGameweek, Fixture,
  Player, Team, ManagerTransfer, PlayerDetailResponse, BackendLeaguePlayerStats, LeagueManagerRef,
  ManagerHistory, ChipPlay,
} from '@/types/fpl';
import { FootballPitch, PlayerWithDetails } from '@/components/FootballPitch';
import { AppBottomSheet } from '@/components/ui/app-bottom-sheet';
import { Colors, Radius, getDifficultyTheme } from '@/constants/theme';
import { Storage } from '@/utils/storage';

function chipLabel(name: string): string {
  switch (name) {
    case 'bboost':   return 'Bench Boost';
    case 'wildcard': return 'Wildcard';
    case '3xc':      return 'Triple Captain';
    case 'freehit':  return 'Free Hit';
    default: return name;
  }
}

function chipShortLabel(name: string, index: number): string {
  const n = index + 1;
  switch (name) {
    case 'bboost': return n > 1 ? `BB${n}` : 'BB1';
    case 'wildcard': return `WC${n}`;
    case '3xc': return n > 1 ? `TC${n}` : 'TC1';
    case 'freehit': return n > 1 ? `FH${n}` : 'FH1';
    default: return '?';
  }
}

type ChipDefinition = {
  identifier: 'wildcard' | 'freehit' | 'bboost' | '3xc';
  label: string;
  color: string;
  textColor: string;
  instance: number;
};

function allChipDefinitions(): ChipDefinition[] {
  return [
    { identifier: 'freehit', label: 'FH1', color: '#FB923C22', textColor: '#FB923C', instance: 1 },
    { identifier: 'wildcard', label: 'WC1', color: '#60A5FA22', textColor: '#60A5FA', instance: 1 },
    { identifier: '3xc', label: 'TC1', color: '#FACC1522', textColor: '#FACC15', instance: 1 },
    { identifier: 'bboost', label: 'BB1', color: '#4ADE8022', textColor: '#4ADE80', instance: 1 },
    { identifier: 'bboost', label: 'BB2', color: '#4ADE8022', textColor: '#4ADE80', instance: 2 },
    { identifier: 'wildcard', label: 'WC2', color: '#60A5FA22', textColor: '#60A5FA', instance: 2 },
    { identifier: '3xc', label: 'TC2', color: '#FACC1522', textColor: '#FACC15', instance: 2 },
    { identifier: 'freehit', label: 'FH2', color: '#FB923C22', textColor: '#FB923C', instance: 2 },
  ];
}

function resolveChipUsage(definition: ChipDefinition, chips: ChipPlay[]): ChipPlay | null {
  const usages = chips.filter((chip) => chip.name === definition.identifier).sort((a, b) => a.event - b.event);
  return usages[definition.instance - 1] ?? null;
}

function orderedChipDefinitions(chips: ChipPlay[]): ChipDefinition[] {
  return allChipDefinitions().sort((a, b) => {
    const aUsage = resolveChipUsage(a, chips);
    const bUsage = resolveChipUsage(b, chips);
    if (aUsage == null && bUsage != null) return 1;
    if (aUsage != null && bUsage == null) return -1;
    if (aUsage != null && bUsage != null && aUsage.event !== bUsage.event) {
      return aUsage.event - bUsage.event;
    }
    return a.label.localeCompare(b.label);
  });
}

export default function FormationScreen() {
  const { managerId, eventId } = useLocalSearchParams<{ managerId: string; eventId: string }>();
  const router = useRouter();
  const insets = useSafeAreaInsets();

  const mId = parseInt(managerId, 10);
  const eId = parseInt(eventId, 10);

  const [picks, setPicks] = useState<ManagerPicks | null>(null);
  const [managerHistory, setManagerHistory] = useState<ManagerHistory | null>(null);
  const [bootstrapData, setBootstrapData] = useState<BootstrapData | null>(null);
  const [liveData, setLiveData] = useState<LiveGameweek | null>(null);
  const [fixtures, setFixtures] = useState<Fixture[]>([]);
  const [playerGwPoints, setPlayerGwPoints] = useState<Map<number, number>>(new Map());
  const [gwTransfers, setGwTransfers] = useState<ManagerTransfer[]>([]);
  const [allTransfers, setAllTransfers] = useState<ManagerTransfer[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [selectedPlayer, setSelectedPlayer] = useState<PlayerWithDetails | null>(null);
  const [playerDetail, setPlayerDetail] = useState<PlayerDetailResponse | null>(null);
  const [leagueStats, setLeagueStats] = useState<BackendLeaguePlayerStats | null>(null);
  const [leagueStatsError, setLeagueStatsError] = useState<string | null>(null);
  const [selectedLeagueId, setSelectedLeagueId] = useState<number | null>(null);
  const [selectedLeagueName, setSelectedLeagueName] = useState<string | null>(null);
  const [isLoadingPlayerData, setIsLoadingPlayerData] = useState(false);
  const [isPitchView, setIsPitchView] = useState(true);
  const [showAllHistory, setShowAllHistory] = useState(false);
  const [showAllTransfers, setShowAllTransfers] = useState(false);
  const [managerName, setManagerName] = useState('Team Formation');
  const playerRequestIdRef = useRef(0);

  const loadData = useCallback(async () => {
    try {
      const [
        picksData,
        bootstrap,
        live,
        fixturesData,
        transfers,
        history,
        manager,
        savedLeagueId,
        savedLeagueName,
        favoriteLeagueId,
        favoriteLeagueName,
      ] = await Promise.all([
        Api.getManagerPicks(mId, eId),
        Api.getBootstrapData(),
        Api.getLiveGameweek(eId).catch(() => null),
        Api.getFixturesByEvent(eId).catch(() => []),
        Api.getManagerTransfers(mId).catch(() => []),
        Api.getManagerHistory(mId).catch(() => null),
        Api.getManagerData(mId).catch(() => null),
        Storage.getSelectedLeagueId(),
        Storage.getSelectedLeagueName(),
        Storage.getFavoriteLeagueId(),
        Storage.getFavoriteLeagueName(),
      ]);
      setPicks(picksData);
      setBootstrapData(bootstrap);
      setLiveData(live);
      setFixtures(fixturesData as Fixture[]);
      setManagerHistory(history);
      setAllTransfers(transfers as ManagerTransfer[]);
      setGwTransfers((transfers as ManagerTransfer[]).filter((t) => t.event === eId));
      setSelectedLeagueId(savedLeagueId ?? favoriteLeagueId);
      setSelectedLeagueName(savedLeagueName ?? favoriteLeagueName);

      const uniquePlayerIds = [...new Set(picksData.picks.map((pick) => pick.element))];
      const pointEntries = await Promise.all(
        uniquePlayerIds.map(async (playerId) => {
          try {
            const detail = await Api.getPlayerDetail(playerId);
            const gwHistory = detail.history.find((entry) => entry.round === eId);
            if (gwHistory) {
              return [playerId, gwHistory.total_points] as const;
            }
            return null;
          } catch {
            return null;
          }
        }),
      );
      setPlayerGwPoints(new Map(pointEntries.filter((entry): entry is readonly [number, number] => entry != null)));

      if (manager) {
        setManagerName(manager.name || `${manager.player_first_name} ${manager.player_last_name}`);
      }
      setError(null);
    } catch (e: any) {
      setError(e.message ?? 'Failed to load formation');
    }
  }, [mId, eId]);

  useEffect(() => {
    loadData().finally(() => setIsLoading(false));
  }, [loadData]);

  const closePlayerModal = () => {
    playerRequestIdRef.current += 1;
    setSelectedPlayer(null);
    setPlayerDetail(null);
    setLeagueStats(null);
    setLeagueStatsError(null);
    setIsLoadingPlayerData(false);
  };

  const handlePlayerPress = async (player: PlayerWithDetails) => {
    const requestId = playerRequestIdRef.current + 1;
    playerRequestIdRef.current = requestId;
    setSelectedPlayer(player);
    setPlayerDetail(null);
    setLeagueStats(null);
    setLeagueStatsError(null);
    setIsLoadingPlayerData(true);

    try {
      const [detail, statsResult] = await Promise.all([
        Api.getPlayerDetail(player.player.id).catch(() => null),
        selectedLeagueId != null
          ? Api.getLeaguePlayerStats(selectedLeagueId, eId, player.player.id)
            .then((stats) => ({ stats, error: null as string | null }))
            .catch((err: Error) => ({ stats: null, error: err.message ?? 'League stats unavailable' }))
          : Promise.resolve({
            stats: null,
            error: selectedLeagueName == null ? 'Open a league to load league-only stats.' : 'League stats unavailable',
          }),
      ]);

      if (playerRequestIdRef.current !== requestId) return;

      setPlayerDetail(detail);
      setLeagueStats(statsResult.stats);
      setLeagueStatsError(statsResult.error);
    } finally {
      if (playerRequestIdRef.current === requestId) {
        setIsLoadingPlayerData(false);
      }
    }
  };

  if (isLoading) {
    return (
      <View style={styles.center}>
        <ActivityIndicator color={Colors.primary} size="large" />
      </View>
    );
  }

  if (error || !picks || !bootstrapData) {
    return (
      <View style={[styles.center, { padding: 24 }]}>
        <Text style={styles.errorText}>⚠️  {error ?? 'No data'}</Text>
        <TouchableOpacity style={styles.retryBtn} onPress={() => { setIsLoading(true); loadData().finally(() => setIsLoading(false)); }}>
          <Text style={styles.retryBtnText}>Retry</Text>
        </TouchableOpacity>
      </View>
    );
  }

  // ── Build PlayerWithDetails ────────────────────────────────────────────────
  const liveMap = new Map(liveData?.elements.map((e) => [e.id, e]) ?? []);
  const playerMap = new Map<number, Player>(bootstrapData.elements.map((p) => [p.id, p]));
  const teamMap = new Map<number, Team>(bootstrapData.teams.map((t) => [t.id, t]));
  const hasLiveFixtures = fixtures.some(
    (fixture) => fixture.started === true && fixture.finished === false && fixture.finished_provisional === false,
  );

  const makePlayerWithDetails = (pickItem: (typeof picks.picks)[0]): PlayerWithDetails | null => {
    const player = playerMap.get(pickItem.element);
    if (!player) return null;
    const liveEl = liveMap.get(pickItem.element);
    const team = teamMap.get(player.team);
    const fixture = fixtures.find((item) => item.team_h === player.team || item.team_a === player.team);
    const isLiveFixture =
      fixture?.started === true &&
      fixture.finished === false &&
      fixture.finished_provisional === false;
    const hasFinishedFixtureWithLiveStats =
      fixture?.finished === true &&
      liveEl != null;
    const hasSettledGwPoints = playerGwPoints.has(pickItem.element);
    const settledGwPoints = playerGwPoints.get(pickItem.element);
    const currentPoints = isLiveFixture && liveEl
      ? liveEl.stats.total_points
      : hasFinishedFixtureWithLiveStats
        ? liveEl.stats.total_points
        : hasSettledGwPoints
          ? (settledGwPoints ?? 0)
          : (liveEl?.stats.total_points ?? player.event_points);
    const liveDeltaPoints = isLiveFixture && liveEl ? liveEl.stats.total_points : 0;
    const displayPoints = currentPoints * (pickItem.position > 11 ? 1 : pickItem.multiplier);

    return {
      pick: pickItem,
      player,
      liveDeltaPoints,
      currentPoints,
      displayPoints,
      teamShortName: team?.short_name ?? '?',
      teamInfo: team,
      fixture,
      liveStats: liveEl,
    };
  };

  const startingXI: PlayerWithDetails[] = picks.picks
    .filter((p) => p.position <= 11)
    .map(makePlayerWithDetails)
    .filter((p): p is PlayerWithDetails => p !== null);

  const bench: PlayerWithDetails[] = picks.picks
    .filter((p) => p.position > 11)
    .sort((a, b) => a.position - b.position)
    .map(makePlayerWithDetails)
    .filter((p): p is PlayerWithDetails => p !== null);

  const totalLive = startingXI.reduce(
    (sum, p) => sum + p.liveDeltaPoints * p.pick.multiplier,
    0,
  );
  const benchPoints = picks.entry_history.points_on_bench;
  const transferHit = picks.entry_history.event_transfers_cost;
  const totalScore = picks.entry_history.total_points;
  const currentHistory = managerHistory?.current ?? [];
  const latestGw = currentHistory[currentHistory.length - 1] ?? null;
  const squadValue = latestGw ? ((latestGw.value - latestGw.bank) / 10).toFixed(1) : '0.0';
  const bankValue = latestGw ? (latestGw.bank / 10).toFixed(1) : '0.0';
  const historyRows = showAllHistory ? [...currentHistory].reverse() : [...currentHistory].reverse().slice(0, 8);
  const chips = managerHistory?.chips ?? [];
  const chipDefinitions = orderedChipDefinitions(chips);
  const groupedTransfers = allTransfers
    .slice()
    .sort((a, b) => b.event - a.event)
    .reduce((acc, transfer) => {
      if (!acc[transfer.event]) acc[transfer.event] = [];
      acc[transfer.event].push(transfer);
      return acc;
    }, {} as Record<number, ManagerTransfer[]>);
  const seasonTransferGroups = Object.entries(groupedTransfers)
    .sort((a, b) => Number(b[0]) - Number(a[0]))
    .slice(0, showAllTransfers ? Object.keys(groupedTransfers).length : 5);
  const transferMetaValue = (() => {
    if (picks.active_chip === 'freehit') return 'FH Active';
    if (picks.active_chip === 'wildcard') return 'WC Active';
    return String(picks.entry_history.event_transfers);
  })();

  const getPlayerName = (id: number) => playerMap.get(id)?.web_name ?? `#${id}`;

  return (
    <View style={[styles.root, { paddingTop: insets.top }]}>
      {/* ── Top bar ───────────────────────────────────────────────── */}
      <View style={styles.topBar}>
        <TouchableOpacity onPress={() => router.back()}>
          <Text style={styles.backBtn}>‹  Back</Text>
        </TouchableOpacity>
        <View style={styles.viewToggle}>
          <TouchableOpacity
            style={[styles.toggleBtn, isPitchView && styles.toggleBtnActive]}
            onPress={() => setIsPitchView(true)}
          >
            <Text style={[styles.toggleText, isPitchView && styles.toggleTextActive]}>
              ⚽ Pitch
            </Text>
          </TouchableOpacity>
          <TouchableOpacity
            style={[styles.toggleBtn, !isPitchView && styles.toggleBtnActive]}
            onPress={() => setIsPitchView(false)}
          >
            <Text style={[styles.toggleText, !isPitchView && styles.toggleTextActive]}>
              📋 List
            </Text>
          </TouchableOpacity>
        </View>
      </View>

      {/* ── Manager header ────────────────────────────────────────── */}
      <View style={styles.managerHeader}>
        <Text style={styles.managerName} numberOfLines={1}>{managerName}</Text>
        <Text style={styles.gwLabel}>GW {eId}</Text>
      </View>

      {/* Chip badge */}
      {picks.active_chip && (
        <View style={styles.chipBanner}>
          <Text style={styles.chipBannerText}>
            🎯 {chipLabel(picks.active_chip)} active
          </Text>
        </View>
      )}

      {/* Score summary */}
      <View style={styles.scoreRow}>
        <ScoreChip label="GW Pts" value={String(picks.entry_history.points)} />
        <ScoreChip label="Total Score" value={String(totalScore)} />
        <ScoreChip label="Live" value={String(hasLiveFixtures ? totalLive : 0)} highlight />
        <ScoreChip label="On Bench" value={String(benchPoints)} />
      </View>

      <View style={styles.metaRow}>
        <MetaChip label="Transfers" value={transferMetaValue} />
        <MetaChip
          label="Hit"
          value={transferHit > 0 ? `-${transferHit}` : '0'}
          danger={transferHit > 0}
        />
      </View>

      <View style={styles.profileGrid}>
        <MetaStatCard label="Team Value" value={`£${squadValue}m`} />
        <MetaStatCard label="In Bank" value={`£${bankValue}m`} />
      </View>

      <ScrollView showsVerticalScrollIndicator={false} contentContainerStyle={{ paddingBottom: 40 }}>
        {isPitchView ? (
          <View style={{ paddingHorizontal: 12 }}>
            <FootballPitch
              startingXI={startingXI}
              bench={bench}
              onPlayerClick={handlePlayerPress}
              tall
            />
          </View>
        ) : (
          <View style={styles.listView}>
            <ListSection title="GOALKEEPER" players={startingXI.filter((p) => p.player.element_type === 1)} onPress={handlePlayerPress} />
            <ListSection title="DEFENDERS" players={startingXI.filter((p) => p.player.element_type === 2)} onPress={handlePlayerPress} />
            <ListSection title="MIDFIELDERS" players={startingXI.filter((p) => p.player.element_type === 3)} onPress={handlePlayerPress} />
            <ListSection title="FORWARDS" players={startingXI.filter((p) => p.player.element_type === 4)} onPress={handlePlayerPress} />
            <ListSection title="BENCH" players={bench} onPress={handlePlayerPress} dim />
          </View>
        )}

        {/* GW Transfers */}
        {gwTransfers.length > 0 && (
          <View style={styles.transfersSection}>
            <Text style={styles.sectionLabel}>GW {eId} TRANSFERS</Text>
            {gwTransfers.map((t, i) => (
              <View key={i} style={styles.transferRow}>
                <Text style={styles.transferOut}>▼ {getPlayerName(t.element_out)}</Text>
                <Text style={styles.transferCost}>£{(t.element_out_cost / 10).toFixed(1)}m</Text>
                <Text style={styles.transferIn}>▲ {getPlayerName(t.element_in)}</Text>
                <Text style={styles.transferCost}>£{(t.element_in_cost / 10).toFixed(1)}m</Text>
              </View>
            ))}
          </View>
        )}

        <View style={styles.chipsSection}>
          <Text style={styles.sectionLabel}>CHIPS</Text>
          <View style={styles.chipGrid}>
            {chipDefinitions.map((chipDef) => {
              const usage = resolveChipUsage(chipDef, chips);
              return (
                <View
                  key={chipDef.label}
                  style={[
                    styles.chipCard,
                    usage == null && styles.chipCardLocked,
                  ]}
                >
                  <View
                    style={[
                      styles.chipCardBadge,
                      { backgroundColor: usage ? chipDef.color : Colors.surfaceHigh },
                    ]}
                  >
                    <Text style={[styles.chipCardBadgeText, { color: usage ? chipDef.textColor : Colors.outline }]}>
                      {usage ? chipDef.label.slice(0, 2) : 'L'}
                    </Text>
                  </View>
                  <Text style={[styles.chipCardTitle, usage == null && styles.chipCardTitleLocked]}>
                    {chipDef.label}
                  </Text>
                  <Text style={[styles.chipCardSub, usage == null && styles.chipCardSubLocked]}>
                    {usage ? `GW ${usage.event}` : 'UNUSED'}
                  </Text>
                </View>
              );
            })}
          </View>
        </View>

        {seasonTransferGroups.length > 0 && (
          <View style={styles.transfersSection}>
            <Text style={styles.sectionLabel}>SEASON TRANSFERS</Text>
            {seasonTransferGroups.map(([gw, txs]) => {
              const gwNumber = Number(gw);
              const gwHit = currentHistory.find((entry) => entry.event === gwNumber)?.event_transfers_cost ?? 0;
              return (
                <View key={gw} style={styles.seasonTransferGroup}>
                  <View style={styles.seasonTransferHeader}>
                    <Text style={styles.seasonTransferTitle}>GW {gw}</Text>
                    {gwHit > 0 ? (
                      <Text style={styles.seasonTransferHit}>-{gwHit} pts</Text>
                    ) : null}
                  </View>
                  {txs.map((transfer, index) => (
                    <View key={`${gw}-${index}`} style={styles.transferRow}>
                      <Text style={styles.transferOut}>▼ {getPlayerName(transfer.element_out)}</Text>
                      <Text style={styles.transferCost}>£{(transfer.element_out_cost / 10).toFixed(1)}m</Text>
                      <Text style={styles.transferIn}>▲ {getPlayerName(transfer.element_in)}</Text>
                      <Text style={styles.transferCost}>£{(transfer.element_in_cost / 10).toFixed(1)}m</Text>
                    </View>
                  ))}
                </View>
              );
            })}
            {Object.keys(groupedTransfers).length > 5 && (
              <TouchableOpacity style={styles.showMoreBtn} onPress={() => setShowAllTransfers((value) => !value)}>
                <Text style={styles.showMoreBtnText}>
                  {showAllTransfers ? 'Show Less Transfers ▲' : `Show All ${Object.keys(groupedTransfers).length} GWs ▼`}
                </Text>
              </TouchableOpacity>
            )}
          </View>
        )}

        {historyRows.length > 0 && (
          <View style={styles.historySection}>
            <Text style={styles.sectionLabel}>GW HISTORY</Text>
            <View style={styles.historyHeader}>
              <Text style={styles.historyCol}>GW</Text>
              <Text style={styles.historyCol}>Pts</Text>
              <Text style={styles.historyCol}>Total</Text>
              <Text style={styles.historyCol}>Rank</Text>
              <Text style={styles.historyCol}>Info</Text>
            </View>
            {historyRows.map((entry) => {
              const chip = chips.find((item) => item.event === entry.event);
              const chipIndex = chip
                ? chips.filter((item) => item.name === chip.name && item.event <= entry.event).length - 1
                : -1;
              return (
                <TouchableOpacity
                  key={entry.event}
                  style={styles.historyRow}
                  onPress={() => router.push(`/formation/${mId}/${entry.event}`)}
                  activeOpacity={0.85}
                >
                  <Text style={styles.historyCell}>{entry.event}</Text>
                  <Text style={[styles.historyCell, styles.historyCellAccent]}>{entry.points}</Text>
                  <Text style={styles.historyCell}>{entry.total_points}</Text>
                  <Text style={styles.historyCell}>{entry.rank != null ? entry.rank.toLocaleString() : '—'}</Text>
                  <View style={styles.historyInfoCell}>
                    {entry.event_transfers_cost > 0 ? (
                      <Text style={styles.historyInfoHit}>-{entry.event_transfers_cost} hit</Text>
                    ) : chip ? (
                      <Text style={styles.historyInfoChip}>{chipShortLabel(chip.name, chipIndex)}</Text>
                    ) : entry.event_transfers > 0 ? (
                      <Text style={styles.historyInfoTransfer}>{entry.event_transfers} xfer</Text>
                    ) : (
                      <Text style={styles.historyInfoTransfer}>—</Text>
                    )}
                  </View>
                </TouchableOpacity>
              );
            })}
            {currentHistory.length > 8 && (
              <TouchableOpacity style={styles.showMoreBtn} onPress={() => setShowAllHistory((value) => !value)}>
                <Text style={styles.showMoreBtnText}>
                  {showAllHistory ? 'Show Less History ▲' : `Show All ${currentHistory.length} GWs ▼`}
                </Text>
              </TouchableOpacity>
            )}
          </View>
        )}
      </ScrollView>

      {/* ── Player Detail Modal ───────────────────────────────────── */}
      {selectedPlayer && (
        <PlayerDetailModal
          player={selectedPlayer}
          detail={playerDetail}
          bootstrapData={bootstrapData}
          leagueStats={leagueStats}
          leagueStatsError={leagueStatsError}
          isLoadingLeagueStats={isLoadingPlayerData}
          currentEvent={eId}
          selectedLeagueName={selectedLeagueName}
          onClose={closePlayerModal}
        />
      )}
    </View>
  );
}

// ── Sub-components ─────────────────────────────────────────────────────────────

function ScoreChip({ label, value, highlight = false }: { label: string; value: string; highlight?: boolean }) {
  return (
    <View style={[scoreChipStyles.chip, highlight && scoreChipStyles.chipHighlight]}>
      <Text style={[scoreChipStyles.value, highlight && scoreChipStyles.valueHighlight]}>{value}</Text>
      <Text style={scoreChipStyles.label}>{label}</Text>
    </View>
  );
}

function MetaStatCard({ label, value }: { label: string; value: string }) {
  return (
    <View style={styles.profileStatCard}>
      <Text style={styles.profileStatValue}>{value}</Text>
      <Text style={styles.profileStatLabel}>{label}</Text>
    </View>
  );
}

function MetaChip({
  label,
  value,
  danger = false,
}: {
  label: string;
  value: string;
  danger?: boolean;
}) {
  return (
    <View style={styles.metaChip}>
      <Text style={styles.metaChipLabel}>{label}</Text>
      <Text
        style={[
          styles.metaChipValue,
          danger && styles.metaChipValueDanger,
          value.length > 8 && styles.metaChipValueCompact,
        ]}
        numberOfLines={1}
      >
        {value}
      </Text>
    </View>
  );
}

function ListSection({
  title, players, onPress, dim = false,
}: {
  title: string;
  players: PlayerWithDetails[];
  onPress: (p: PlayerWithDetails) => void;
  dim?: boolean;
}) {
  return (
    <View>
      <Text style={listStyles.sectionTitle}>{title}</Text>
      {players.map((p) => (
        <TouchableOpacity
          key={p.pick.element}
          style={[listStyles.row, dim && { opacity: 0.7 }]}
          onPress={() => onPress(p)}
        >
          <View style={listStyles.posTag}>
            <Text style={listStyles.posText}>
              {p.player.element_type === 1 ? 'GK' :
               p.player.element_type === 2 ? 'DEF' :
               p.player.element_type === 3 ? 'MID' : 'FWD'}
            </Text>
          </View>
          <Text style={listStyles.name} numberOfLines={1}>{p.player.web_name}</Text>
          {p.pick.is_captain && <Text style={listStyles.captainBadge}>C</Text>}
          {p.pick.is_vice_captain && <Text style={listStyles.vcBadge}>V</Text>}
          <Text style={listStyles.points}>{p.displayPoints}</Text>
        </TouchableOpacity>
      ))}
    </View>
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
  switch (teamId) {
    case 9:
    case 15:
      return '#FFFFFF';
    case 13:
    case 20:
      return '#131313';
    default:
      return '#FFFFFF';
  }
}

function formatPercent(value: number | string | undefined): string {
  const numeric = typeof value === 'string' ? parseFloat(value) : value;
  if (numeric == null || Number.isNaN(numeric)) return '0.0%';
  return `${numeric.toFixed(1)}%`;
}

function sortManagers(managers: LeagueManagerRef[] = []): LeagueManagerRef[] {
  return [...managers].sort((a, b) => a.rank - b.rank || a.entryName.localeCompare(b.entryName));
}

function ManagerRow({
  manager,
  icon,
  variant = 'neutral',
}: {
  manager: LeagueManagerRef;
  icon: string;
  variant?: 'neutral' | 'captain' | 'start' | 'bench';
}) {
  return (
    <View
      style={[
        detailStyles.managerCard,
        variant === 'captain' && detailStyles.managerCardCaptain,
        variant === 'start' && detailStyles.managerCardStart,
        variant === 'bench' && detailStyles.managerCardBench,
      ]}
    >
      <Text style={detailStyles.managerIcon}>{icon}</Text>
      <Text style={detailStyles.managerText}>#{manager.rank} {manager.entryName}</Text>
    </View>
  );
}

function DialogSection({
  title,
  children,
}: {
  title: string;
  children: React.ReactNode;
}) {
  return (
    <View style={detailStyles.sectionCard}>
      <Text style={detailStyles.sectionCardTitle}>{title}</Text>
      {children}
    </View>
  );
}

function SummaryPointRow({
  label,
  value,
  emphasize = false,
}: {
  label: string;
  value: string;
  emphasize?: boolean;
}) {
  return (
    <View style={detailStyles.dataRow}>
      <Text style={[detailStyles.dataLabel, emphasize && detailStyles.dataLabelStrong]}>{label}</Text>
      <Text style={[detailStyles.dataValue, emphasize && detailStyles.dataValueStrong]}>{value}</Text>
    </View>
  );
}

function OwnershipRow({ label, value }: { label: string; value: string }) {
  return (
    <View style={detailStyles.dataRow}>
      <Text style={detailStyles.ownershipLabel}>{label}</Text>
      <Text style={detailStyles.ownershipValue}>{value}</Text>
    </View>
  );
}

function DialogTabButton({
  label,
  selected,
  onPress,
}: {
  label: string;
  selected: boolean;
  onPress: () => void;
}) {
  return (
    <TouchableOpacity style={detailStyles.tabButton} onPress={onPress} activeOpacity={0.8}>
      <Text style={[detailStyles.tabLabel, selected && detailStyles.tabLabelActive]}>{label}</Text>
      <View style={[detailStyles.tabIndicator, selected && detailStyles.tabIndicatorActive]} />
    </TouchableOpacity>
  );
}

function PlayerDetailModal({
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
  player: PlayerWithDetails;
  detail: PlayerDetailResponse | null;
  bootstrapData: BootstrapData;
  leagueStats: BackendLeaguePlayerStats | null;
  leagueStatsError: string | null;
  isLoadingLeagueStats: boolean;
  currentEvent: number;
  selectedLeagueName: string | null;
  onClose: () => void;
}) {
  const insets = useSafeAreaInsets();
  const [selectedTab, setSelectedTab] = useState<'summary' | 'starts' | 'bench' | 'season_log'>('summary');
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
  const allPreviousFixtures = detail?.history
    .filter((entry) => entry.round <= currentEvent) ?? [];
  const allUpcomingFixtures = detail?.fixtures
    .filter((fixture) => !fixture.finished && fixture.event != null && fixture.event > currentEvent) ?? [];
  const previousFixtures = showAllPrevious ? allPreviousFixtures : allPreviousFixtures.slice(-5);
  const upcomingFixtures = showAllUpcoming ? allUpcomingFixtures : allUpcomingFixtures.slice(0, 5);
  const explainStats = player.liveStats?.explain.flatMap((entry) => entry.stats) ?? [];
  const defensiveContribution = explainStats.find((stat) => {
    const identifier = stat.identifier.toLowerCase();
    return identifier.includes('def');
  });
  const defensiveThreshold = player.player.element_type === 2 ? 10 : 12;
  const defensiveActions = defensiveContribution?.value ?? 0;
  const defensiveBonusPoints = defensiveActions >= defensiveThreshold ? 2 : 0;
  const captainedBy = sortManagers(leagueStats?.captainedBy);
  const startedBy = sortManagers(
    leagueStats?.startedBy.filter(
      (manager) => !captainedBy.some((captained) => captained.entryName === manager.entryName && captained.rank === manager.rank),
    ) ?? [],
  );
  const benchedBy = sortManagers(leagueStats?.benchedBy);
  const leagueLabel = selectedLeagueName ?? 'the selected league';
  const displayMinutes = player.fixture && player.fixture.started === true && !player.fixture.finished
    ? player.fixture.minutes
    : (selectedHistory?.minutes ?? 0);
  const seasonHistory = [...(detail?.history ?? [])].sort((a, b) => b.round - a.round);
  const seasonTotals = seasonHistory.reduce((acc, row) => ({
    totalPoints: acc.totalPoints + row.total_points,
    minutes: acc.minutes + row.minutes,
    goals: acc.goals + row.goals_scored,
    assists: acc.assists + row.assists,
    cleanSheets: acc.cleanSheets + row.clean_sheets,
    defCon: acc.defCon + Number(row.defensive_contribution ?? 0),
    xg: acc.xg + (Number.parseFloat(row.expected_goals ?? '0') || 0),
    xa: acc.xa + (Number.parseFloat(row.expected_assists ?? '0') || 0),
    xgc: acc.xgc + (Number.parseFloat(row.expected_goals_conceded ?? '0') || 0),
  }), {
    totalPoints: 0,
    minutes: 0,
    goals: 0,
    assists: 0,
    cleanSheets: 0,
    defCon: 0,
    xg: 0,
    xa: 0,
    xgc: 0,
  });

  const seasonLogContent = (
    <DialogSection title="SEASON MATCH LOG">
      <ScrollView horizontal showsHorizontalScrollIndicator={false}>
        <View style={detailStyles.seasonTable}>
          <View style={detailStyles.seasonHeaderRow}>
            <Text style={[detailStyles.seasonHeaderCell, detailStyles.seasonGw]}>GW</Text>
            <Text style={[detailStyles.seasonHeaderCell, detailStyles.seasonOpp]}>Opp</Text>
            <Text style={[detailStyles.seasonHeaderCell, detailStyles.seasonStat]}>Min</Text>
            <Text style={[detailStyles.seasonHeaderCell, detailStyles.seasonStat]}>Pts</Text>
            <Text style={[detailStyles.seasonHeaderCell, detailStyles.seasonMini]}>G</Text>
            <Text style={[detailStyles.seasonHeaderCell, detailStyles.seasonMini]}>A</Text>
            <Text style={[detailStyles.seasonHeaderCell, detailStyles.seasonMini]}>C</Text>
            <Text style={[detailStyles.seasonHeaderCell, detailStyles.seasonMini]}>DC</Text>
            <Text style={[detailStyles.seasonHeaderCell, detailStyles.seasonExpected]}>xG</Text>
            <Text style={[detailStyles.seasonHeaderCell, detailStyles.seasonExpected]}>xA</Text>
            <Text style={[detailStyles.seasonHeaderCell, detailStyles.seasonExpected]}>xC</Text>
          </View>
          <View style={detailStyles.tableDivider} />
          {seasonHistory.map((match) => {
            const opponentTeam = bootstrapData.teams.find((entry) => entry.id === match.opponent_team);
            return (
              <View key={`${match.fixture}-${match.round}`} style={detailStyles.seasonRow}>
                <Text style={[detailStyles.seasonCell, detailStyles.seasonGw]}>{match.round}</Text>
                <Text style={[detailStyles.seasonCell, detailStyles.seasonOpp]}>
                  {opponentTeam?.short_name ?? 'OPP'} {match.was_home ? '(H)' : '(A)'}
                </Text>
                <Text style={[detailStyles.seasonCell, detailStyles.seasonStat]}>{match.minutes > 0 ? match.minutes : '–'}</Text>
                <Text style={[detailStyles.seasonCell, detailStyles.seasonStat, detailStyles.seasonPoints]}>{match.total_points}</Text>
                <Text style={[detailStyles.seasonCell, detailStyles.seasonMini]}>{match.goals_scored}</Text>
                <Text style={[detailStyles.seasonCell, detailStyles.seasonMini]}>{match.assists}</Text>
                <Text style={[detailStyles.seasonCell, detailStyles.seasonMini]}>{match.clean_sheets}</Text>
                <Text style={[detailStyles.seasonCell, detailStyles.seasonMini]}>{match.defensive_contribution ?? 0}</Text>
                <Text style={[detailStyles.seasonCell, detailStyles.seasonExpected]}>{(Number.parseFloat(match.expected_goals ?? '0') || 0).toFixed(2)}</Text>
                <Text style={[detailStyles.seasonCell, detailStyles.seasonExpected]}>{(Number.parseFloat(match.expected_assists ?? '0') || 0).toFixed(2)}</Text>
                <Text style={[detailStyles.seasonCell, detailStyles.seasonExpected]}>{(Number.parseFloat(match.expected_goals_conceded ?? '0') || 0).toFixed(2)}</Text>
              </View>
            );
          })}
          <View style={detailStyles.tableDivider} />
          <View style={detailStyles.seasonRow}>
            <Text style={[detailStyles.seasonCell, detailStyles.seasonGw, detailStyles.seasonFooterLabel]}>Total</Text>
            <Text style={[detailStyles.seasonCell, detailStyles.seasonOpp]}>Season</Text>
            <Text style={[detailStyles.seasonCell, detailStyles.seasonStat]}>{seasonTotals.minutes}</Text>
            <Text style={[detailStyles.seasonCell, detailStyles.seasonStat, detailStyles.seasonPoints]}>{seasonTotals.totalPoints}</Text>
            <Text style={[detailStyles.seasonCell, detailStyles.seasonMini]}>{seasonTotals.goals}</Text>
            <Text style={[detailStyles.seasonCell, detailStyles.seasonMini]}>{seasonTotals.assists}</Text>
            <Text style={[detailStyles.seasonCell, detailStyles.seasonMini]}>{seasonTotals.cleanSheets}</Text>
            <Text style={[detailStyles.seasonCell, detailStyles.seasonMini]}>{seasonTotals.defCon}</Text>
            <Text style={[detailStyles.seasonCell, detailStyles.seasonExpected]}>{seasonTotals.xg.toFixed(2)}</Text>
            <Text style={[detailStyles.seasonCell, detailStyles.seasonExpected]}>{seasonTotals.xa.toFixed(2)}</Text>
            <Text style={[detailStyles.seasonCell, detailStyles.seasonExpected]}>{seasonTotals.xgc.toFixed(2)}</Text>
          </View>
        </View>
      </ScrollView>
    </DialogSection>
  );

  const summaryContent = (() => {
    if (detail == null && isLoadingLeagueStats) {
      return <ActivityIndicator color={Colors.primary} style={detailStyles.loadingBlock} />;
    }

    return (
      <>
        <View style={detailStyles.infoCard}>
          <Text style={detailStyles.infoText}>
            League stats on this dialog are for {leagueLabel} and GW{currentEvent} only.
          </Text>
        </View>

        {selectedHistory && (
          <DialogSection title="LATEST MATCH">
            <SummaryPointRow
              label={`Defensive contributions (${defensiveActions}):`}
              value={defensiveBonusPoints > 0 ? `+${defensiveBonusPoints}` : '0'}
            />
            {selectedHistory.yellow_cards > 0 && (
              <SummaryPointRow label="Yellow card:" value="-1" />
            )}
            {selectedHistory.red_cards > 0 && (
              <SummaryPointRow label="Red card:" value="-3" />
            )}
            {selectedHistory.saves > 0 && (
              <SummaryPointRow label={`${selectedHistory.saves} saves:`} value={String(Math.floor(selectedHistory.saves / 3))} />
            )}
            {selectedHistory.goals_scored > 0 && (
              <SummaryPointRow
                label={`${selectedHistory.goals_scored} goals:`}
                value={String(
                  selectedHistory.goals_scored * (
                    player.player.element_type === 4 ? 4 :
                    player.player.element_type === 3 ? 5 : 6
                  ),
                )}
              />
            )}
            {selectedHistory.assists > 0 && (
              <SummaryPointRow label={`${selectedHistory.assists} assists:`} value={String(selectedHistory.assists * 3)} />
            )}
            {selectedHistory.clean_sheets > 0 && (
              <SummaryPointRow
                label="Clean sheet:"
                value={String(
                  player.player.element_type === 1 || player.player.element_type === 2 ? 4 :
                  player.player.element_type === 3 ? 1 : 0,
                )}
              />
            )}
            {selectedHistory.goals_conceded >= 2 && (player.player.element_type === 1 || player.player.element_type === 2) && (
              <SummaryPointRow
                label={`Goals conceded (${selectedHistory.goals_conceded}):`}
                value={String(-Math.floor(selectedHistory.goals_conceded / 2))}
              />
            )}
            <SummaryPointRow
              label={`Played ${displayMinutes} min:`}
              value={String(displayMinutes === 0 ? 0 : displayMinutes >= 60 ? 2 : 1)}
            />
            <SummaryPointRow label={`Bonus (${selectedHistory.bps} bps):`} value={String(selectedHistory.bonus)} />
            <View style={detailStyles.divider} />
            <SummaryPointRow label="Total Points:" value={String(selectedHistory.total_points)} emphasize />
          </DialogSection>
        )}

        {(leagueStats != null || leagueStatsError != null) && (
          <DialogSection title="OWNERSHIP">
            {leagueStats ? (
              <>
                <OwnershipRow label="Starts league:" value={formatPercent(leagueStats.startsPercentage)} />
                <OwnershipRow label="Owned league:" value={formatPercent(leagueStats.ownedPercentage)} />
                <OwnershipRow label="Owned overall:" value={formatPercent(player.player.selected_by_percent)} />
                <OwnershipRow label="Captain count:" value={String(leagueStats.captainCount)} />
                <OwnershipRow label="Price:" value={`£${(player.player.now_cost / 10).toFixed(1)}M`} />
              </>
            ) : (
              <Text style={detailStyles.emptyStateText}>{leagueStatsError}</Text>
            )}
          </DialogSection>
        )}

        {previousFixtures.length > 0 && (
          <DialogSection title="PREVIOUS FIXTURES">
            <View style={detailStyles.tableHeader}>
              <Text style={[detailStyles.tableHeaderCell, detailStyles.colGw]}>GW</Text>
              <Text style={[detailStyles.tableHeaderCell, detailStyles.colOpp]}>Opp</Text>
              <Text style={[detailStyles.tableHeaderCell, detailStyles.colMin]}>Min</Text>
              <Text style={[detailStyles.tableHeaderCell, detailStyles.colPts]}>Pts</Text>
            </View>
            <View style={detailStyles.tableDivider} />
            {previousFixtures.map((match) => {
              const opponentTeam = bootstrapData.teams.find((entry) => entry.id === match.opponent_team);
              return (
                <View key={`${match.fixture}-${match.round}`} style={detailStyles.tableRow}>
                  <Text style={[detailStyles.tableCell, detailStyles.colGwValue]}>{match.round}</Text>
                  <Text style={[detailStyles.tableCell, detailStyles.colOppValue]}>
                    {opponentTeam?.short_name ?? 'OPP'} {match.was_home ? '(H)' : '(A)'}
                  </Text>
                  <Text style={[detailStyles.tableCell, detailStyles.colMinValue]}>{match.minutes}</Text>
                  <Text style={[detailStyles.tableCell, detailStyles.colPtsValue]}>{match.total_points}</Text>
                </View>
              );
            })}
            {allPreviousFixtures.length > 5 && (
              <TouchableOpacity
                style={detailStyles.expandButton}
                onPress={() => setShowAllPrevious((value) => !value)}
                activeOpacity={0.85}
              >
                <Text style={detailStyles.expandButtonText}>
                  {showAllPrevious ? 'Show less' : `Show all ${allPreviousFixtures.length}`}
                </Text>
              </TouchableOpacity>
            )}
          </DialogSection>
        )}

        {upcomingFixtures.length > 0 && (
          <DialogSection title="UPCOMING FIXTURES">
            <View style={detailStyles.tableHeader}>
              <Text style={[detailStyles.tableHeaderCell, detailStyles.colGw]}>GW</Text>
              <Text style={[detailStyles.tableHeaderCell, detailStyles.colOpp]}>Opp</Text>
              <Text style={[detailStyles.tableHeaderCell, detailStyles.colDiff]}>Diff</Text>
            </View>
            <View style={detailStyles.tableDivider} />
            {upcomingFixtures.map((fixture) => {
              const opponentTeamId = fixture.is_home ? fixture.team_a : fixture.team_h;
              const opponentTeam = bootstrapData.teams.find((entry) => entry.id === opponentTeamId);
              const difficultyTheme = getDifficultyTheme(fixture.difficulty);
              return (
                <View key={`${fixture.id}-${fixture.event}`} style={detailStyles.tableRow}>
                  <Text style={[detailStyles.tableCell, detailStyles.colGwValue]}>{fixture.event}</Text>
                  <Text style={[detailStyles.tableCell, detailStyles.colOppValue]}>
                    {opponentTeam?.short_name ?? 'TBD'} {fixture.is_home ? '(H)' : '(A)'}
                  </Text>
                  <View style={detailStyles.colDiffValue}>
                    <View style={[detailStyles.difficultyPill, { backgroundColor: difficultyTheme.backgroundColor }]}>
                      <Text style={[detailStyles.difficultyText, { color: difficultyTheme.color }]}>{fixture.difficulty}</Text>
                    </View>
                  </View>
                </View>
              );
            })}
            {allUpcomingFixtures.length > 5 && (
              <TouchableOpacity
                style={detailStyles.expandButton}
                onPress={() => setShowAllUpcoming((value) => !value)}
                activeOpacity={0.85}
              >
                <Text style={detailStyles.expandButtonText}>
                  {showAllUpcoming ? 'Show less' : `Show all ${allUpcomingFixtures.length}`}
                </Text>
              </TouchableOpacity>
            )}
          </DialogSection>
        )}

        {detail == null && !isLoadingLeagueStats && (
          <Text style={detailStyles.emptyStateText}>Match details are unavailable for this player right now.</Text>
        )}
      </>
    );
  })();

  const startsContent = (() => {
    if (isLoadingLeagueStats && leagueStats == null) {
      return (
        <View style={detailStyles.emptyStateWrap}>
          <ActivityIndicator color={Colors.primary} />
          <Text style={detailStyles.emptyStateText}>Checking league teams…</Text>
          <Text style={detailStyles.emptyStateSubtext}>This checks up to 50 teams</Text>
        </View>
      );
    }

    if (leagueStats == null) {
      return (
        <View style={detailStyles.emptyStateWrap}>
          <Text style={detailStyles.emptyStateTitle}>No league data available</Text>
          <Text style={detailStyles.emptyStateText}>{leagueStatsError ?? 'Open a league to see starts info.'}</Text>
        </View>
      );
    }

    return (
      <>
        <Text style={detailStyles.tabSectionTitle}>TEAMS WHO STARTED THIS PLAYER</Text>
        {captainedBy.length > 0 && (
          <>
            <Text style={detailStyles.subSectionHeading}>Captained By ({captainedBy.length})</Text>
            {captainedBy.map((manager) => (
              <ManagerRow key={`captain-${manager.rank}-${manager.entryName}`} manager={manager} icon="★" variant="captain" />
            ))}
          </>
        )}
        {startedBy.length > 0 && (
          <>
            <Text style={detailStyles.subSectionHeadingGreen}>Started By ({startedBy.length})</Text>
            {startedBy.map((manager) => (
              <ManagerRow key={`start-${manager.rank}-${manager.entryName}`} manager={manager} icon="✓" variant="start" />
            ))}
          </>
        )}
        {captainedBy.length === 0 && startedBy.length === 0 && (
          <View style={detailStyles.emptyCard}>
            <Text style={detailStyles.emptyStateText}>No teams in this league started this player.</Text>
          </View>
        )}
      </>
    );
  })();

  const benchContent = (() => {
    if (isLoadingLeagueStats && leagueStats == null) {
      return (
        <View style={detailStyles.emptyStateWrap}>
          <ActivityIndicator color={Colors.primary} />
          <Text style={detailStyles.emptyStateText}>Checking league teams…</Text>
          <Text style={detailStyles.emptyStateSubtext}>This checks up to 50 teams</Text>
        </View>
      );
    }

    if (leagueStats == null) {
      return (
        <View style={detailStyles.emptyStateWrap}>
          <Text style={detailStyles.emptyStateTitle}>No league data available</Text>
          <Text style={detailStyles.emptyStateText}>{leagueStatsError ?? 'Open a league to see bench info.'}</Text>
        </View>
      );
    }

    return (
      <>
        <Text style={[detailStyles.tabSectionTitle, detailStyles.tabSectionTitleBench]}>TEAMS WHO BENCHED THIS PLAYER</Text>
        {leagueStats.benchCount > 0 ? (
          <>
            <View style={detailStyles.benchSummaryCard}>
              <Text style={detailStyles.benchSummaryTitle}>Benched Count: {leagueStats.benchCount}</Text>
              <Text style={detailStyles.benchSummaryText}>
                These managers own this player but have them on the bench.
              </Text>
            </View>
            {benchedBy.map((manager) => (
              <ManagerRow key={`bench-${manager.rank}-${manager.entryName}`} manager={manager} icon="B" variant="bench" />
            ))}
          </>
        ) : (
          <View style={detailStyles.emptyCard}>
            <Text style={detailStyles.emptyStateText}>No teams in this league benched this player.</Text>
          </View>
        )}
      </>
    );
  })();

  return (
    <AppBottomSheet visible onClose={onClose} snapPoints={['97%']}>
      <View style={detailStyles.sheet}>
        <View style={detailStyles.header}>
          <View style={[detailStyles.teamBadge, { backgroundColor: getTeamBadgeColor(team?.id) }]}>
            <Text style={[detailStyles.teamBadgeText, { color: getTeamBadgeTextColor(team?.id) }]}>
              {teamShortName.toUpperCase()}
            </Text>
          </View>
          <View style={detailStyles.headerCopy}>
            <Text style={detailStyles.name}>{player.player.web_name}</Text>
            <Text style={detailStyles.meta}>{teamShortName} · {getPositionName(player.player.element_type)}</Text>
            <Text style={detailStyles.caption}>Selected league · GW{currentEvent}</Text>
          </View>
          <TouchableOpacity onPress={onClose} style={detailStyles.closeBtn}>
            <Text style={detailStyles.closeBtnText}>×</Text>
          </TouchableOpacity>
        </View>

        <View style={detailStyles.tabRow}>
          <DialogTabButton
            label="Summary"
            selected={selectedTab === 'summary'}
            onPress={() => setSelectedTab('summary')}
          />
          <DialogTabButton
            label={leagueStats ? `Starts (${leagueStats.startsCount})` : 'Starts'}
            selected={selectedTab === 'starts'}
            onPress={() => setSelectedTab('starts')}
          />
          <DialogTabButton
            label={leagueStats ? `Bench (${leagueStats.benchCount})` : 'Bench'}
            selected={selectedTab === 'bench'}
            onPress={() => setSelectedTab('bench')}
          />
          <DialogTabButton
            label="Season Log"
            selected={selectedTab === 'season_log'}
            onPress={() => setSelectedTab('season_log')}
          />
        </View>

        <BottomSheetScrollView
          style={detailStyles.scroll}
          showsVerticalScrollIndicator={false}
          contentContainerStyle={[detailStyles.scrollContent, { paddingBottom: insets.bottom + 24 }]}
        >
          {selectedTab === 'summary' && summaryContent}
          {selectedTab === 'starts' && startsContent}
          {selectedTab === 'bench' && benchContent}
          {selectedTab === 'season_log' && seasonLogContent}
          <TouchableOpacity style={detailStyles.closeAction} onPress={onClose} activeOpacity={0.9}>
            <Text style={detailStyles.closeActionText}>CLOSE</Text>
          </TouchableOpacity>
        </BottomSheetScrollView>
      </View>
    </AppBottomSheet>
  );
}

// ── Styles ─────────────────────────────────────────────────────────────────────
const scoreChipStyles = StyleSheet.create({
  chip: {
    flex: 1,
    backgroundColor: Colors.surface,
    borderRadius: Radius.md,
    padding: 10,
    alignItems: 'center',
    gap: 2,
  },
  chipHighlight: {
    backgroundColor: Colors.primaryContainer,
  },
  value: {
    fontSize: 18, fontWeight: '800',
    color: Colors.onSurface,
  },
  valueHighlight: {
    color: Colors.primary,
  },
  label: {
    fontSize: 9, fontWeight: '700',
    color: Colors.outline,
    letterSpacing: 0.5,
  },
});

const listStyles = StyleSheet.create({
  sectionTitle: {
    fontSize: 9, fontWeight: '800',
    color: Colors.outline,
    letterSpacing: 3,
    paddingHorizontal: 16,
    paddingTop: 12,
    paddingBottom: 6,
  },
  row: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: 16,
    paddingVertical: 10,
    borderBottomWidth: 1,
    borderBottomColor: Colors.outlineVariant + '22',
    gap: 8,
  },
  posTag: {
    width: 32, height: 20,
    backgroundColor: Colors.surfaceHigh,
    borderRadius: 4,
    justifyContent: 'center',
    alignItems: 'center',
  },
  posText: {
    fontSize: 8, fontWeight: '800',
    color: Colors.outline,
  },
  name: {
    flex: 1,
    fontSize: 13, fontWeight: '600',
    color: Colors.onSurface,
  },
  captainBadge: {
    width: 18, height: 18,
    borderRadius: 9,
    backgroundColor: Colors.primary,
    color: Colors.onPrimary,
    fontSize: 9, fontWeight: '900',
    textAlign: 'center',
    lineHeight: 18,
    overflow: 'hidden',
  },
  vcBadge: {
    width: 18, height: 18,
    borderRadius: 9,
    backgroundColor: Colors.surfaceHighest,
    color: Colors.onSurface,
    fontSize: 9, fontWeight: '900',
    textAlign: 'center',
    lineHeight: 18,
    overflow: 'hidden',
  },
  points: {
    fontSize: 14, fontWeight: '800',
    color: Colors.primary,
    minWidth: 30,
    textAlign: 'right',
  },
});

const detailStyles = StyleSheet.create({
  sheet: {
    flex: 1,
    minHeight: 0,
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
    backgroundColor: Colors.surfaceContainer,
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
    borderRadius: 999,
    width: '100%',
    marginTop: 10,
    backgroundColor: 'transparent',
  },
  tabIndicatorActive: {
    backgroundColor: Colors.primary,
  },
  scroll: {
    flex: 1,
    minHeight: 0,
  },
  scrollContent: {
    padding: 16,
    gap: 12,
  },
  loadingBlock: {
    marginTop: 24,
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
  sectionCardTitle: {
    fontSize: 12,
    fontWeight: '800',
    letterSpacing: 1.5,
    color: Colors.onSurfaceVariant,
    marginBottom: 10,
  },
  bodyText: {
    fontSize: 13,
    color: Colors.onSurface,
    lineHeight: 20,
  },
  subtleText: {
    fontSize: 12,
    color: Colors.outline,
    marginTop: 4,
  },
  tightGap: {
    height: 8,
  },
  divider: {
    height: 1,
    backgroundColor: Colors.outlineVariant + '55',
    marginVertical: 10,
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
  ownershipLabel: {
    fontSize: 13,
    color: Colors.outline,
  },
  ownershipValue: {
    fontSize: 14,
    fontWeight: '700',
    color: Colors.onSurface,
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
  colGw: {
    width: 34,
  },
  colOpp: {
    flex: 1,
  },
  colMin: {
    width: 44,
    textAlign: 'center',
  },
  colPts: {
    width: 44,
    textAlign: 'right',
  },
  colDiff: {
    width: 60,
    textAlign: 'center',
  },
  colGwValue: {
    width: 34,
    color: Colors.onSurface,
  },
  colOppValue: {
    flex: 1,
    color: Colors.onSurfaceVariant,
  },
  colMinValue: {
    width: 44,
    color: Colors.onSurfaceVariant,
    textAlign: 'center',
  },
  colPtsValue: {
    width: 44,
    color: Colors.primary,
    fontWeight: '800',
    textAlign: 'right',
  },
  colDiffValue: {
    width: 60,
    alignItems: 'center',
  },
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
  seasonTable: {
    minWidth: 660,
  },
  seasonHeaderRow: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  seasonHeaderCell: {
    fontSize: 11,
    fontWeight: '700',
    color: Colors.outline,
  },
  seasonRow: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingVertical: 6,
  },
  seasonCell: {
    fontSize: 12,
    color: Colors.onSurface,
  },
  seasonGw: {
    width: 34,
  },
  seasonOpp: {
    width: 86,
    color: Colors.onSurfaceVariant,
  },
  seasonStat: {
    width: 42,
    textAlign: 'center',
    color: Colors.onSurfaceVariant,
  },
  seasonMini: {
    width: 32,
    textAlign: 'center',
    color: Colors.onSurfaceVariant,
  },
  seasonExpected: {
    width: 48,
    textAlign: 'center',
    color: Colors.onSurfaceVariant,
  },
  seasonPoints: {
    color: Colors.primary,
    fontWeight: '800',
  },
  seasonFooterLabel: {
    color: Colors.onSurface,
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
  tabSectionTitleBench: {
    color: Colors.tertiary,
  },
  subSectionHeading: {
    fontSize: 13,
    fontWeight: '700',
    color: Colors.secondary,
    marginBottom: 8,
  },
  subSectionHeadingGreen: {
    fontSize: 13,
    fontWeight: '700',
    color: Colors.primary,
    marginTop: 12,
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
  managerCardStart: {
    backgroundColor: '#2D5A2738',
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
  emptyStateWrap: {
    paddingVertical: 24,
    alignItems: 'center',
    justifyContent: 'center',
    gap: 6,
  },
  emptyCard: {
    backgroundColor: Colors.surface,
    borderRadius: 12,
    padding: 16,
  },
  emptyStateTitle: {
    fontSize: 14,
    fontWeight: '700',
    color: Colors.onSurface,
  },
  emptyStateText: {
    fontSize: 13,
    color: Colors.outline,
    lineHeight: 18,
    textAlign: 'center',
  },
  emptyStateSubtext: {
    fontSize: 11,
    color: Colors.outline,
  },
  closeAction: {
    marginHorizontal: 16,
    marginTop: 12,
    backgroundColor: Colors.primaryContainer,
    borderRadius: 12,
    paddingVertical: 14,
    alignItems: 'center',
    justifyContent: 'center',
  },
  closeActionText: {
    fontSize: 14,
    fontWeight: '800',
    color: Colors.primary,
    letterSpacing: 1,
  },
  bottomSpacer: {
    height: 0,
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
  topBar: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingHorizontal: 16,
    paddingVertical: 12,
  },
  backBtn: {
    fontSize: 16, fontWeight: '600',
    color: Colors.primary,
  },
  viewToggle: {
    flexDirection: 'row',
    backgroundColor: Colors.surfaceHigh,
    borderRadius: Radius.md,
    padding: 3,
    gap: 3,
  },
  toggleBtn: {
    borderRadius: Radius.sm,
    paddingHorizontal: 12,
    paddingVertical: 6,
  },
  toggleBtnActive: {
    backgroundColor: Colors.primaryContainer,
  },
  toggleText: {
    fontSize: 11, fontWeight: '700',
    color: Colors.outline,
  },
  toggleTextActive: {
    color: Colors.primary,
  },
  managerHeader: {
    paddingHorizontal: 16,
    paddingBottom: 8,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
  },
  managerName: {
    fontSize: 18, fontWeight: '800',
    color: Colors.onSurface,
    flex: 1,
  },
  gwLabel: {
    fontSize: 11, fontWeight: '700',
    color: Colors.outline,
    letterSpacing: 1,
  },
  chipBanner: {
    backgroundColor: Colors.primaryContainer + '55',
    marginHorizontal: 16,
    borderRadius: Radius.md,
    paddingVertical: 6,
    paddingHorizontal: 12,
    marginBottom: 8,
  },
  chipBannerText: {
    fontSize: 12, fontWeight: '700',
    color: Colors.primary,
  },
  scoreRow: {
    flexDirection: 'row',
    paddingHorizontal: 16,
    paddingBottom: 8,
    gap: 8,
  },
  metaRow: {
    flexDirection: 'row',
    paddingHorizontal: 16,
    paddingBottom: 12,
    gap: 8,
  },
  metaChip: {
    backgroundColor: Colors.surface,
    borderRadius: Radius.full,
    paddingHorizontal: 12,
    paddingVertical: 8,
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
  },
  metaChipLabel: {
    fontSize: 11, fontWeight: '700',
    color: Colors.outline,
    letterSpacing: 0.6,
  },
  metaChipValue: {
    fontSize: 13, fontWeight: '800',
    color: Colors.onSurface,
  },
  metaChipValueCompact: {
    fontSize: 12,
  },
  metaChipValueDanger: {
    color: Colors.tertiary,
  },
  listView: {
    backgroundColor: Colors.surface,
    borderRadius: Radius.lg,
    marginHorizontal: 12,
    marginTop: 4,
    overflow: 'hidden',
  },
  transfersSection: {
    backgroundColor: Colors.surface,
    borderRadius: Radius.lg,
    marginHorizontal: 12,
    marginTop: 12,
    padding: 16,
    gap: 8,
  },
  sectionLabel: {
    fontSize: 9, fontWeight: '800',
    color: Colors.primary,
    letterSpacing: 3,
    marginBottom: 4,
  },
  transferRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 6,
    flexWrap: 'wrap',
  },
  transferOut: {
    fontSize: 12, fontWeight: '600',
    color: Colors.tertiary,
  },
  transferIn: {
    fontSize: 12, fontWeight: '600',
    color: Colors.primary,
  },
  transferCost: {
    fontSize: 11,
    color: Colors.outline,
  },
  profileGrid: {
    flexDirection: 'row',
    gap: 8,
    marginHorizontal: 12,
    marginTop: 12,
    marginBottom: 8,
  },
  profileStatCard: {
    flex: 1,
    backgroundColor: Colors.surface,
    borderRadius: Radius.lg,
    padding: 14,
  },
  profileStatValue: {
    fontSize: 20,
    fontWeight: '800',
    color: Colors.onSurface,
  },
  profileStatLabel: {
    fontSize: 10,
    fontWeight: '700',
    color: Colors.onSurfaceVariant,
    letterSpacing: 0.5,
    marginTop: 4,
  },
  chipsSection: {
    backgroundColor: Colors.surface,
    borderRadius: Radius.lg,
    marginHorizontal: 12,
    marginTop: 12,
    padding: 16,
  },
  chipGrid: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    justifyContent: 'space-between',
    rowGap: 10,
  },
  chipCard: {
    width: '48%',
    backgroundColor: Colors.surfaceContainer,
    borderRadius: Radius.md,
    paddingVertical: 18,
    paddingHorizontal: 14,
    alignItems: 'center',
    gap: 8,
  },
  chipCardLocked: {
    borderWidth: 1,
    borderStyle: 'dashed',
    borderColor: Colors.outlineVariant + '66',
    opacity: 0.72,
  },
  chipCardBadge: {
    width: 42,
    height: 42,
    borderRadius: 21,
    alignItems: 'center',
    justifyContent: 'center',
  },
  chipCardBadgeText: {
    fontSize: 13,
    fontWeight: '900',
  },
  chipCardTitle: {
    fontSize: 14,
    fontWeight: '800',
    color: Colors.onSurface,
  },
  chipCardTitleLocked: {
    color: Colors.outline,
  },
  chipCardSub: {
    fontSize: 11,
    fontWeight: '700',
    color: Colors.primary,
  },
  chipCardSubLocked: {
    color: Colors.outline,
  },
  seasonTransferGroup: {
    gap: 8,
    paddingVertical: 6,
    borderBottomWidth: 1,
    borderBottomColor: Colors.outlineVariant + '22',
  },
  seasonTransferHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  seasonTransferTitle: {
    fontSize: 11,
    fontWeight: '800',
    color: Colors.outline,
    letterSpacing: 1,
  },
  seasonTransferHit: {
    fontSize: 11,
    fontWeight: '800',
    color: Colors.tertiary,
  },
  historySection: {
    backgroundColor: Colors.surface,
    borderRadius: Radius.lg,
    marginHorizontal: 12,
    marginTop: 12,
    padding: 16,
  },
  historyHeader: {
    flexDirection: 'row',
    paddingBottom: 8,
    borderBottomWidth: 1,
    borderBottomColor: Colors.outlineVariant + '22',
  },
  historyCol: {
    flex: 1,
    fontSize: 10,
    fontWeight: '700',
    color: Colors.outline,
    letterSpacing: 0.5,
    textAlign: 'center',
  },
  historyRow: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingVertical: 10,
    borderBottomWidth: 1,
    borderBottomColor: Colors.outlineVariant + '22',
  },
  historyCell: {
    flex: 1,
    fontSize: 12,
    color: Colors.onSurface,
    textAlign: 'center',
  },
  historyCellAccent: {
    color: Colors.primary,
    fontWeight: '800',
  },
  historyInfoCell: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
  },
  historyInfoHit: {
    fontSize: 10,
    color: Colors.tertiary,
    fontWeight: '700',
  },
  historyInfoChip: {
    fontSize: 10,
    color: Colors.primary,
    fontWeight: '700',
  },
  historyInfoTransfer: {
    fontSize: 10,
    color: Colors.outline,
  },
  showMoreBtn: {
    alignItems: 'center',
    paddingTop: 12,
  },
  showMoreBtnText: {
    fontSize: 12,
    fontWeight: '700',
    color: Colors.primary,
  },
  errorText: {
    color: Colors.tertiary,
    fontSize: 14,
    textAlign: 'center',
    marginBottom: 16,
  },
  retryBtn: {
    backgroundColor: Colors.primaryContainer,
    borderRadius: Radius.md,
    paddingVertical: 10,
    paddingHorizontal: 24,
  },
  retryBtnText: {
    color: Colors.primary,
    fontWeight: '600',
  },
});
