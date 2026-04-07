import React, { useCallback, useEffect, useMemo, useState } from 'react';
import {
  ActivityIndicator,
  RefreshControl,
  ScrollView,
  StyleSheet,
  Text,
  TouchableOpacity,
  View,
} from 'react-native';
import { BottomSheetScrollView } from '@gorhom/bottom-sheet';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { Api } from '@/services/api';
import {
  BackendLeaguePlayerStats,
  BootstrapData,
  Event,
  LiveGameweek,
  PlayerDetailResponse,
  StatsLeaderboardPlayer,
  StatsLeaderboardSection,
  StatsOverviewResponse,
} from '@/types/fpl';
import { Colors, Radius } from '@/constants/theme';
import { Storage } from '@/utils/storage';
import { AppBottomSheet } from '@/components/ui/app-bottom-sheet';
import { MatchDialogPlayer, PlayerLeagueModal } from './matches';

function currentEventFromBootstrap(bootstrap: BootstrapData): number {
  return bootstrap.events.find((event) => event.is_current)?.id
    ?? bootstrap.events.find((event) => event.is_next)?.id
    ?? bootstrap.events[bootstrap.events.length - 1]?.id
    ?? 1;
}

function formatPrice(value?: number): string {
  if (value == null) return '—';
  return `£${(value / 10).toFixed(1)}m`;
}

export default function StatsScreen() {
  const PAGE_SIZE = 20;
  const insets = useSafeAreaInsets();
  const [bootstrapData, setBootstrapData] = useState<BootstrapData | null>(null);
  const [statsOverview, setStatsOverview] = useState<StatsOverviewResponse | null>(null);
  const [liveGameweek, setLiveGameweek] = useState<LiveGameweek | null>(null);
  const [selectedEvent, setSelectedEvent] = useState<number | null>(null);
  const [favoriteLeagueId, setFavoriteLeagueId] = useState<number | null>(null);
  const [favoriteLeagueName, setFavoriteLeagueName] = useState<string | null>(null);
  const [gwPickerOpen, setGwPickerOpen] = useState(false);
  const [pointsFilter, setPointsFilter] = useState<'ALL' | 'GK' | 'DEF' | 'MID' | 'FWD'>('ALL');
  const [defConFilter, setDefConFilter] = useState<'DEF' | 'MID' | 'FWD'>('DEF');
  const [visibleCounts, setVisibleCounts] = useState<Record<string, number>>({});
  const [isLoading, setIsLoading] = useState(true);
  const [isRefreshing, setIsRefreshing] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const [selectedPlayer, setSelectedPlayer] = useState<MatchDialogPlayer | null>(null);
  const [playerDetail, setPlayerDetail] = useState<PlayerDetailResponse | null>(null);
  const [leagueStats, setLeagueStats] = useState<BackendLeaguePlayerStats | null>(null);
  const [leagueStatsError, setLeagueStatsError] = useState<string | null>(null);
  const [isLoadingPlayerData, setIsLoadingPlayerData] = useState(false);

  const loadData = useCallback(async (requestedEvent?: number, refreshing = false) => {
    if (refreshing) {
      setIsRefreshing(true);
    } else {
      setIsLoading(true);
    }
    setError(null);

    try {
      const [bootstrap, storedLeagueId, storedLeagueName] = await Promise.all([
        bootstrapData ? Promise.resolve(bootstrapData) : Api.getBootstrapData(),
        Storage.getFavoriteLeagueId(),
        Storage.getFavoriteLeagueName(),
      ]);

      const resolvedEvent = requestedEvent ?? selectedEvent ?? currentEventFromBootstrap(bootstrap);
      const [overview, live] = await Promise.all([
        Api.getStatsOverview(resolvedEvent),
        Api.getLiveGameweek(resolvedEvent),
      ]);

      setBootstrapData(bootstrap);
      setFavoriteLeagueId(storedLeagueId);
      setFavoriteLeagueName(storedLeagueName);
      setSelectedEvent(overview.meta.event);
      setStatsOverview(overview);
      setLiveGameweek(live);
    } catch (e: any) {
      setError(e.message ?? 'Failed to load stats');
    } finally {
      setIsLoading(false);
      setIsRefreshing(false);
    }
  }, [bootstrapData, selectedEvent]);

  useEffect(() => {
    void loadData();
  }, [loadData]);

  const liveMap = useMemo(
    () => new Map((liveGameweek?.elements ?? []).map((entry) => [entry.id, entry])),
    [liveGameweek],
  );

  const availableEvents = useMemo(
    () => (bootstrapData?.events ?? []).filter((event) => event.id <= currentEventFromBootstrap(bootstrapData ?? {
      events: [],
      teams: [],
      elements: [],
      element_types: [],
    })),
    [bootstrapData],
  );

  const handleSelectEvent = useCallback((eventId: number) => {
    setGwPickerOpen(false);
    void loadData(eventId, false);
  }, [loadData]);

  const handlePlayerPress = useCallback(async (row: StatsLeaderboardPlayer) => {
    if (!bootstrapData || selectedEvent == null) return;

    const player = bootstrapData.elements.find((entry) => entry.id === row.playerId);
    if (!player) return;

    const teamInfo = bootstrapData.teams.find((team) => team.id === player.team);
    const liveStats = liveMap.get(player.id);
    setSelectedPlayer({
      player,
      teamInfo,
      teamShortName: teamInfo?.short_name ?? row.teamShortName,
      liveStats,
    });
    setPlayerDetail(null);
    setLeagueStats(null);
    setLeagueStatsError(null);
    setIsLoadingPlayerData(true);

    try {
      const [detail, statsResult] = await Promise.all([
        Api.getPlayerDetail(player.id),
        favoriteLeagueId != null
          ? Api.getLeaguePlayerStats(favoriteLeagueId, selectedEvent, player.id)
          : Promise.resolve(null),
      ]);
      setPlayerDetail(detail);
      setLeagueStats(statsResult);
      if (favoriteLeagueId == null) {
        setLeagueStatsError('Pick a favorite league to see league ownership and starts.');
      }
    } catch (e: any) {
      setLeagueStatsError(e.message ?? 'Failed to load player detail');
    } finally {
      setIsLoadingPlayerData(false);
    }
  }, [bootstrapData, favoriteLeagueId, liveMap, selectedEvent]);

  const sections = statsOverview?.sections;
  const defConSection = statsOverview?.defCon[defConFilter];
  const filteredMostPoints = useMemo(() => {
    if (!sections?.mostPoints) return null;
    if (pointsFilter === 'ALL') return sections.mostPoints;
    return {
      ...sections.mostPoints,
      rows: sections.mostPoints.rows.filter((row) => row.position === pointsFilter),
    };
  }, [pointsFilter, sections?.mostPoints]);

  if (isLoading && statsOverview == null) {
    return (
      <View style={styles.loadingRoot}>
        <ActivityIndicator color={Colors.primary} />
      </View>
    );
  }

  return (
    <View style={styles.root}>
      <ScrollView
        contentContainerStyle={[styles.content, { paddingTop: insets.top + 10, paddingBottom: insets.bottom + 110 }]}
        refreshControl={
          <RefreshControl refreshing={isRefreshing} onRefresh={() => void loadData(selectedEvent ?? undefined, true)} tintColor={Colors.primary} />
        }
        showsVerticalScrollIndicator={false}
      >
        <View style={styles.headerRow}>
          <View>
            <Text style={styles.kicker}>OVERALL FPL</Text>
            <Text style={styles.subtitle}>Season leaders and trends</Text>
          </View>
          <View style={styles.headerActions}>
            <TouchableOpacity style={styles.refreshButton} onPress={() => void loadData(selectedEvent ?? undefined, true)}>
              <Text style={styles.refreshButtonText}>↺</Text>
            </TouchableOpacity>
          </View>
        </View>

        {error ? (
          <View style={styles.errorCard}>
            <Text style={styles.errorTitle}>Stats unavailable</Text>
            <Text style={styles.errorText}>{error}</Text>
          </View>
        ) : (
          <>
            {sections && (
              <>
                {filteredMostPoints && (
                  <LeaderboardCard
                    section={filteredMostPoints}
                    onPlayerPress={handlePlayerPress}
                    visibleCount={visibleCounts.mostPoints ?? PAGE_SIZE}
                    onLoadMore={() => setVisibleCounts((prev) => ({ ...prev, mostPoints: (prev.mostPoints ?? PAGE_SIZE) + PAGE_SIZE }))}
                    showSecondary={false}
                    headerExtra={(
                      <View style={styles.filterRow}>
                        {(['ALL', 'GK', 'DEF', 'MID', 'FWD'] as const).map((filter) => (
                          <TouchableOpacity
                            key={filter}
                            style={[styles.filterChip, pointsFilter === filter && styles.filterChipActive]}
                            onPress={() => setPointsFilter(filter)}
                          >
                            <Text style={[styles.filterChipText, pointsFilter === filter && styles.filterChipTextActive]}>
                              {filter}
                            </Text>
                          </TouchableOpacity>
                        ))}
                      </View>
                    )}
                  />
                )}
                <LeaderboardCard
                  section={sections.gameweekPoints}
                  onPlayerPress={handlePlayerPress}
                  visibleCount={visibleCounts.gameweekPoints ?? PAGE_SIZE}
                  onLoadMore={() => setVisibleCounts((prev) => ({ ...prev, gameweekPoints: (prev.gameweekPoints ?? PAGE_SIZE) + PAGE_SIZE }))}
                  highlightLive={Boolean(sections.gameweekPoints.isLive)}
                  headerExtra={(
                    <View style={styles.sectionHeaderActions}>
                      <Text style={styles.sectionHint}>
                        {statsOverview?.meta.eventName ?? `GW ${selectedEvent ?? '—'}`}
                        {statsOverview?.meta.isLive ? ' · LIVE' : ''}
                      </Text>
                      <TouchableOpacity style={styles.inlineGwButton} onPress={() => setGwPickerOpen(true)}>
                        <Text style={styles.inlineGwButtonLabel}>GW {selectedEvent ?? '—'}</Text>
                      </TouchableOpacity>
                    </View>
                  )}
                />
                <LeaderboardCard
                  section={sections.goals}
                  onPlayerPress={handlePlayerPress}
                  visibleCount={visibleCounts.goals ?? PAGE_SIZE}
                  onLoadMore={() => setVisibleCounts((prev) => ({ ...prev, goals: (prev.goals ?? PAGE_SIZE) + PAGE_SIZE }))}
                />
                <LeaderboardCard
                  section={sections.assists}
                  onPlayerPress={handlePlayerPress}
                  visibleCount={visibleCounts.assists ?? PAGE_SIZE}
                  onLoadMore={() => setVisibleCounts((prev) => ({ ...prev, assists: (prev.assists ?? PAGE_SIZE) + PAGE_SIZE }))}
                />

                <View style={styles.card}>
                  <View style={styles.cardHeaderRow}>
                    <View>
                      <Text style={styles.cardTitle}>DEF CON</Text>
                      <Text style={styles.cardSubtitle}>How many times each player earned the +2 bonus through GW {selectedEvent}</Text>
                    </View>
                    <Text style={styles.cardStatLabel}>{defConSection?.statLabel ?? 'BONUS +2'}</Text>
                  </View>
                  <View style={styles.filterRow}>
                    {(['DEF', 'MID', 'FWD'] as const).map((filter) => (
                      <TouchableOpacity
                        key={filter}
                        style={[styles.filterChip, defConFilter === filter && styles.filterChipActive]}
                        onPress={() => setDefConFilter(filter)}
                      >
                        <Text style={[styles.filterChipText, defConFilter === filter && styles.filterChipTextActive]}>
                          {filter}
                        </Text>
                      </TouchableOpacity>
                    ))}
                  </View>
                  {defConSection && (
                    <LeaderboardRows
                      rows={defConSection.rows}
                      visibleCount={visibleCounts.defCon ?? PAGE_SIZE}
                      onLoadMore={() => setVisibleCounts((prev) => ({ ...prev, defCon: (prev.defCon ?? PAGE_SIZE) + PAGE_SIZE }))}
                      onPlayerPress={handlePlayerPress}
                    />
                  )}
                </View>

                <LeaderboardCard
                  section={sections.marketRisers}
                  onPlayerPress={handlePlayerPress}
                  visibleCount={visibleCounts.marketRisers ?? PAGE_SIZE}
                  onLoadMore={() => setVisibleCounts((prev) => ({ ...prev, marketRisers: (prev.marketRisers ?? PAGE_SIZE) + PAGE_SIZE }))}
                />
                <LeaderboardCard
                  section={sections.marketFallers}
                  onPlayerPress={handlePlayerPress}
                  visibleCount={visibleCounts.marketFallers ?? PAGE_SIZE}
                  onLoadMore={() => setVisibleCounts((prev) => ({ ...prev, marketFallers: (prev.marketFallers ?? PAGE_SIZE) + PAGE_SIZE }))}
                />
                {sections.form && (
                  <LeaderboardCard
                    section={sections.form}
                    onPlayerPress={handlePlayerPress}
                    visibleCount={visibleCounts.form ?? PAGE_SIZE}
                    onLoadMore={() => setVisibleCounts((prev) => ({ ...prev, form: (prev.form ?? PAGE_SIZE) + PAGE_SIZE }))}
                  />
                )}
                {sections.ownership && (
                  <LeaderboardCard
                    section={sections.ownership}
                    onPlayerPress={handlePlayerPress}
                    visibleCount={visibleCounts.ownership ?? PAGE_SIZE}
                    onLoadMore={() => setVisibleCounts((prev) => ({ ...prev, ownership: (prev.ownership ?? PAGE_SIZE) + PAGE_SIZE }))}
                  />
                )}
              </>
            )}
          </>
        )}
      </ScrollView>

      <AppBottomSheet visible={gwPickerOpen} onClose={() => setGwPickerOpen(false)} snapPoints={['58%']} backgroundStyle={{ backgroundColor: Colors.surfaceHigh }}>
        <View style={styles.sheetContent}>
          <Text style={styles.sheetTitle}>Select Gameweek</Text>
          <BottomSheetScrollView style={{ flex: 1 }} showsVerticalScrollIndicator={false}>
            {availableEvents.map((event: Event) => {
              const active = event.id === selectedEvent;
              return (
                <TouchableOpacity
                  key={event.id}
                  style={[styles.sheetItem, active && styles.sheetItemActive]}
                  onPress={() => handleSelectEvent(event.id)}
                >
                  <Text style={[styles.sheetItemText, active && styles.sheetItemTextActive]}>{event.name}</Text>
                  {(event.is_current || event.is_next) && (
                    <Text style={styles.sheetItemMeta}>{event.is_current ? 'CURRENT' : 'NEXT'}</Text>
                  )}
                </TouchableOpacity>
              );
            })}
          </BottomSheetScrollView>
        </View>
      </AppBottomSheet>

      {selectedPlayer && bootstrapData && (
        <PlayerLeagueModal
          player={selectedPlayer}
          detail={playerDetail}
          bootstrapData={bootstrapData}
          leagueStats={leagueStats}
          leagueStatsError={leagueStatsError}
          isLoadingLeagueStats={isLoadingPlayerData}
          currentEvent={selectedEvent ?? statsOverview?.meta.event ?? 1}
          selectedLeagueName={favoriteLeagueName}
          onClose={() => setSelectedPlayer(null)}
          variant="stats"
        />
      )}
    </View>
  );
}

function LeaderboardCard({
  section,
  visibleCount,
  onLoadMore,
  onPlayerPress,
  highlightLive = false,
  showSecondary = true,
  headerExtra,
}: {
  section: StatsLeaderboardSection;
  visibleCount: number;
  onLoadMore: () => void;
  onPlayerPress: (row: StatsLeaderboardPlayer) => void;
  highlightLive?: boolean;
  showSecondary?: boolean;
  headerExtra?: React.ReactNode;
}) {
  return (
    <View style={styles.card}>
      <View style={styles.cardHeaderRow}>
        <View>
          <Text style={styles.cardTitle}>{section.title}</Text>
          {highlightLive && <Text style={styles.cardSubtitle}>Live totals update during matches</Text>}
        </View>
        <Text style={styles.cardStatLabel}>{section.statLabel}</Text>
      </View>
      {headerExtra}
      <LeaderboardRows rows={section.rows} visibleCount={visibleCount} onLoadMore={onLoadMore} onPlayerPress={onPlayerPress} showSecondary={showSecondary} />
    </View>
  );
}

function LeaderboardRows({
  rows,
  visibleCount,
  onLoadMore,
  onPlayerPress,
  showSecondary = true,
}: {
  rows: StatsLeaderboardPlayer[];
  visibleCount: number;
  onLoadMore: () => void;
  onPlayerPress: (row: StatsLeaderboardPlayer) => void;
  showSecondary?: boolean;
}) {
  const visibleRows = rows.slice(0, visibleCount);
  const remaining = rows.length - visibleRows.length;
  return (
    <>
      {visibleRows.map((row, index) => (
        <TouchableOpacity key={`${row.playerId}-${index}`} style={styles.row} onPress={() => onPlayerPress(row)} activeOpacity={0.82}>
          <Text style={styles.rankCell}>{String(index + 1).padStart(2, '0')}</Text>
          <View style={styles.playerCell}>
            <Text style={styles.playerName} numberOfLines={1}>{row.name}</Text>
            <Text style={styles.playerMeta} numberOfLines={1}>
              {row.teamShortName} · {row.position}
              {showSecondary && row.secondaryDisplay ? ` · ${row.secondaryDisplay}` : ''}
              {showSecondary && row.currentPrice != null && !row.secondaryDisplay ? ` · ${formatPrice(row.currentPrice)}` : ''}
            </Text>
          </View>
          <View style={styles.valueCell}>
            <Text style={[styles.primaryValue, row.priceDelta != null && row.priceDelta < 0 && styles.primaryValueNegative]}>
              {row.primaryDisplay}
            </Text>
            {row.liveDelta != null && row.liveDelta > 0 && <Text style={styles.liveDelta}>+{row.liveDelta} live</Text>}
          </View>
        </TouchableOpacity>
      ))}
      {remaining > 0 && (
        <TouchableOpacity style={styles.showMoreBtn} onPress={onLoadMore}>
          <Text style={styles.showMoreBtnText}>Load {Math.min(20, remaining)} more ({remaining} left)</Text>
        </TouchableOpacity>
      )}
    </>
  );
}

const styles = StyleSheet.create({
  root: {
    flex: 1,
    backgroundColor: Colors.background,
  },
  loadingRoot: {
    flex: 1,
    backgroundColor: Colors.background,
    alignItems: 'center',
    justifyContent: 'center',
  },
  content: {
    paddingHorizontal: 16,
    gap: 14,
  },
  headerRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'flex-start',
  },
  kicker: {
    fontSize: 11,
    fontWeight: '800',
    color: Colors.primary,
    letterSpacing: 2,
  },
  title: {
    marginTop: 8,
    fontSize: 32,
    fontWeight: '300',
    color: Colors.onSurface,
  },
  subtitle: {
    marginTop: 10,
    fontSize: 28,
    fontWeight: '300',
    color: Colors.onSurface,
  },
  headerActions: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
  },
  refreshButton: {
    width: 42,
    height: 42,
    borderRadius: 21,
    backgroundColor: Colors.surfaceHigh,
    alignItems: 'center',
    justifyContent: 'center',
  },
  refreshButtonText: {
    fontSize: 22,
    color: Colors.onSurface,
  },
  errorCard: {
    backgroundColor: Colors.surface,
    borderRadius: Radius.lg,
    padding: 18,
  },
  errorTitle: {
    fontSize: 15,
    fontWeight: '800',
    color: Colors.tertiary,
  },
  errorText: {
    marginTop: 8,
    fontSize: 13,
    lineHeight: 20,
    color: Colors.onSurfaceVariant,
  },
  card: {
    backgroundColor: Colors.surface,
    borderRadius: Radius.xl,
    padding: 14,
  },
  cardHeaderRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'flex-start',
    gap: 10,
    marginBottom: 8,
  },
  cardTitle: {
    fontSize: 12,
    fontWeight: '800',
    color: Colors.primary,
    letterSpacing: 2,
  },
  cardSubtitle: {
    marginTop: 4,
    fontSize: 11,
    color: Colors.outline,
  },
  cardStatLabel: {
    fontSize: 10,
    fontWeight: '800',
    color: Colors.secondary,
    letterSpacing: 1.5,
  },
  filterRow: {
    flexDirection: 'row',
    gap: 8,
    marginTop: 4,
    marginBottom: 6,
  },
  sectionHeaderActions: {
    marginTop: 4,
    alignItems: 'flex-start',
    gap: 8,
  },
  sectionHint: {
    fontSize: 11,
    color: Colors.outline,
  },
  inlineGwButton: {
    paddingHorizontal: 12,
    paddingVertical: 8,
    borderRadius: Radius.full,
    backgroundColor: Colors.surfaceHigh,
  },
  inlineGwButtonLabel: {
    fontSize: 11,
    fontWeight: '800',
    color: Colors.onSurface,
    letterSpacing: 1,
  },
  filterChip: {
    paddingHorizontal: 12,
    paddingVertical: 8,
    borderRadius: Radius.full,
    backgroundColor: Colors.surfaceHigh,
  },
  filterChipActive: {
    backgroundColor: Colors.primaryContainer,
  },
  filterChipText: {
    fontSize: 11,
    fontWeight: '800',
    color: Colors.onSurfaceVariant,
    letterSpacing: 1,
  },
  filterChipTextActive: {
    color: Colors.primary,
  },
  row: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 12,
    paddingVertical: 10,
    borderBottomWidth: 1,
    borderBottomColor: Colors.outlineVariant + '22',
  },
  rankCell: {
    width: 28,
    fontSize: 15,
    fontWeight: '800',
    color: Colors.outline,
  },
  playerCell: {
    flex: 1,
  },
  playerName: {
    fontSize: 14,
    fontWeight: '800',
    color: Colors.onSurface,
  },
  playerMeta: {
    marginTop: 3,
    fontSize: 11,
    color: Colors.outline,
  },
  valueCell: {
    alignItems: 'flex-end',
    minWidth: 72,
  },
  primaryValue: {
    fontSize: 18,
    fontWeight: '900',
    color: Colors.onSurface,
  },
  primaryValueNegative: {
    color: Colors.tertiary,
  },
  liveDelta: {
    marginTop: 2,
    fontSize: 10,
    fontWeight: '700',
    color: Colors.primary,
  },
  showMoreBtn: {
    marginTop: 10,
    alignSelf: 'flex-start',
    paddingHorizontal: 14,
    paddingVertical: 10,
    borderRadius: Radius.full,
    backgroundColor: Colors.surfaceHigh,
  },
  showMoreBtnText: {
    fontSize: 11,
    fontWeight: '800',
    color: Colors.primary,
    letterSpacing: 1,
  },
  sheetContent: {
    flex: 1,
    paddingHorizontal: 18,
    paddingBottom: 18,
  },
  sheetTitle: {
    fontSize: 15,
    fontWeight: '800',
    color: Colors.onSurface,
    marginBottom: 12,
  },
  sheetItem: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingVertical: 14,
    paddingHorizontal: 8,
    borderBottomWidth: 1,
    borderBottomColor: Colors.outlineVariant + '22',
  },
  sheetItemActive: {
    backgroundColor: Colors.primaryContainer + '33',
    borderRadius: Radius.md,
  },
  sheetItemText: {
    fontSize: 14,
    fontWeight: '600',
    color: Colors.onSurface,
  },
  sheetItemTextActive: {
    color: Colors.primary,
  },
  sheetItemMeta: {
    fontSize: 10,
    fontWeight: '800',
    color: Colors.outline,
    letterSpacing: 1,
  },
});
