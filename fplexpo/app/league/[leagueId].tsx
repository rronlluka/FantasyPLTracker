/**
 * League Standings Screen
 * Mirrors the Android standings screen with GW selector and chip-history toggle.
 */
import React, { useCallback, useEffect, useMemo, useState } from 'react';
import {
  ActivityIndicator,
  FlatList,
  RefreshControl,
  StyleSheet,
  Text,
  TouchableOpacity,
  View,
} from 'react-native';
import { BottomSheetScrollView } from '@gorhom/bottom-sheet';
import { Ionicons } from '@expo/vector-icons';
import { useLocalSearchParams, useRouter } from 'expo-router';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { Colors, Radius } from '@/constants/theme';
import { AppBottomSheet } from '@/components/ui/app-bottom-sheet';
import { Api } from '@/services/api';
import {
  BootstrapData,
  Fixture,
  GameweekHistory,
  LeagueStandings,
  ManagerHistory,
  StandingEntry,
} from '@/types/fpl';
import { Storage } from '@/utils/storage';

type UsedChip = {
  name: string;
  event: number;
  number: number;
};

type ManagerLeagueMeta = {
  activeChip: string | null;
  captainName: string | null;
  allChips: UsedChip[];
  inPlay: number;
  toStart: number;
  livePoints: number;
};

function formatTotal(value: number): string {
  return new Intl.NumberFormat().format(value);
}

function chipBg(name: string) {
  switch (name) {
    case 'bboost':
      return Colors.chipBB;
    case 'wildcard':
      return Colors.chipWC;
    case '3xc':
      return Colors.chipTC;
    case 'freehit':
      return Colors.chipFH;
    default:
      return Colors.surfaceHigh;
  }
}

function chipFg(name: string) {
  switch (name) {
    case 'bboost':
      return Colors.chipBBText;
    case 'wildcard':
      return Colors.chipWCText;
    case '3xc':
      return Colors.chipTCText;
    case 'freehit':
      return Colors.chipFHText;
    default:
      return Colors.onSurface;
  }
}

function chipShort(name: string) {
  switch (name) {
    case 'bboost':
      return 'BB';
    case 'wildcard':
      return 'WC';
    case '3xc':
      return 'TC';
    case 'freehit':
      return 'FH';
    default:
      return '?';
  }
}

function chipLabel(name: string, number: number) {
  const safeNumber = Math.min(Math.max(number, 1), 2);
  switch (name) {
    case 'bboost':
      return safeNumber > 1 ? `BB${safeNumber}` : 'BB';
    case 'wildcard':
      return `WC${safeNumber}`;
    case '3xc':
      return safeNumber > 1 ? `TC${safeNumber}` : 'TC';
    case 'freehit':
      return safeNumber > 1 ? `FH${safeNumber}` : 'FH';
    default:
      return '?';
  }
}

function buildUsedChips(chips: ManagerHistory['chips'] | undefined): UsedChip[] {
  if (!chips?.length) return [];

  const countByName = new Map<string, number>();
  return [...chips]
    .sort((a, b) => a.event - b.event)
    .map((chip) => {
      const nextCount = (countByName.get(chip.name) ?? 0) + 1;
      countByName.set(chip.name, nextCount);
      return { name: chip.name, event: chip.event, number: nextCount };
    });
}

function getCurrentEvent(bootstrap: BootstrapData): number {
  return (
    bootstrap.events.find((event) => event.is_current)?.id ??
    bootstrap.events.find((event) => event.is_next)?.id ??
    bootstrap.events[0]?.id ??
    1
  );
}

export default function LeagueStandingsScreen() {
  const { leagueId } = useLocalSearchParams<{ leagueId: string }>();
  const router = useRouter();
  const insets = useSafeAreaInsets();
  const id = parseInt(leagueId, 10);

  const [standings, setStandings] = useState<LeagueStandings | null>(null);
  const [displayEntries, setDisplayEntries] = useState<StandingEntry[]>([]);
  const [fixtures, setFixtures] = useState<Fixture[]>([]);
  const [managerMetaMap, setManagerMetaMap] = useState<Map<number, ManagerLeagueMeta>>(new Map());
  const [currentEvent, setCurrentEvent] = useState(1);
  const [availableGameweeks, setAvailableGameweeks] = useState<number[]>([]);
  const [selectedGameweek, setSelectedGameweek] = useState<number | null>(null);
  const [currentManagerId, setCurrentManagerId] = useState<number | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [isRefreshing, setIsRefreshing] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [isFavorite, setIsFavorite] = useState(false);
  const [showChipHistory, setShowChipHistory] = useState(false);
  const [showGwModal, setShowGwModal] = useState(false);

  const loadData = useCallback(async () => {
    try {
      const [standingsData, bootstrap, favoriteLeagueId, savedManagerId] = await Promise.all([
        Api.getLeagueStandings(id, 1, selectedGameweek ?? undefined),
        Api.getBootstrapData(),
        Storage.getFavoriteLeagueId(),
        Storage.getManagerId(),
      ]);

      const activeEvent = getCurrentEvent(bootstrap);
      const displayGameweek = selectedGameweek ?? activeEvent;
      const isHistorical = selectedGameweek != null && selectedGameweek !== activeEvent;

      setStandings(standingsData);
      await Storage.saveSelectedLeague(id, standingsData.league.name);
      setCurrentEvent(activeEvent);
      setAvailableGameweeks(Array.from({ length: activeEvent }, (_, index) => index + 1));
      setIsFavorite(favoriteLeagueId === id);
      setCurrentManagerId(savedManagerId);

      let currentFixtures: Fixture[] = [];
      let live = null;
      if (!isHistorical) {
        [currentFixtures, live] = await Promise.all([
          Api.getFixturesByEvent(activeEvent).catch(() => []),
          Api.getLiveGameweek(activeEvent).catch(() => null),
        ]);
      }

      setFixtures(currentFixtures);

      const liveElementMap = new Map(live?.elements.map((element) => [element.id, element]) ?? []);
      const managerEntries = standingsData.standings.results;
      const managerResults = await Promise.all(
        managerEntries.map(async (entry) => {
          const [picks, history] = await Promise.all([
            Api.getManagerPicks(entry.entry, displayGameweek).catch(() => null),
            Api.getManagerHistory(entry.entry).catch(() => null),
          ]);

          const captainName = picks?.picks
            .find((pick) => pick.is_captain)
            ?.element;

          const captainPlayer = captainName != null
            ? bootstrap.elements.find((player) => player.id === captainName)?.web_name ?? null
            : null;

          const targetGwData: GameweekHistory | null =
            history?.current?.find((gw) => gw.event === displayGameweek) ?? null;

          let inPlay = 0;
          let toStart = 0;
          let livePoints = 0;

          if (!isHistorical && picks) {
            const scoringPicks = picks.picks.filter((pick) =>
              picks.active_chip === 'bboost' ? pick.position <= 15 : pick.position <= 11,
            );

            scoringPicks.forEach((pick) => {
              const player = bootstrap.elements.find((element) => element.id === pick.element);
              if (!player) return;

              const fixture = currentFixtures.find(
                (item) => item.team_h === player.team || item.team_a === player.team,
              );
              if (!fixture) return;

              const isLiveFixture =
                fixture.started === true &&
                fixture.finished === false &&
                fixture.finished_provisional === false;

              if (isLiveFixture) {
                inPlay += 1;
                const liveElement = liveElementMap.get(pick.element);
                if (liveElement) {
                  livePoints += liveElement.stats.total_points * pick.multiplier;
                }
                return;
              }

              if (fixture.started !== true) {
                toStart += 1;
              }
            });
          }

          return {
            entryId: entry.entry,
            picks,
            targetGwData,
            meta: {
              activeChip: picks?.active_chip ?? null,
              captainName: captainPlayer,
              allChips: buildUsedChips(history?.chips),
              inPlay,
              toStart,
              livePoints,
            } satisfies ManagerLeagueMeta,
          };
        }),
      );

      const nextMetaMap = new Map<number, ManagerLeagueMeta>();
      managerResults.forEach(({ entryId, meta }) => {
        nextMetaMap.set(entryId, meta);
      });

      setManagerMetaMap(nextMetaMap);

      if (isHistorical) {
        const correctedEntries = managerResults
          .map(({ entryId, targetGwData }) => {
            const standing = managerEntries.find((item) => item.entry === entryId);
            if (!standing) return null;

            return {
              ...standing,
              event_total: targetGwData?.points ?? 0,
              total: targetGwData?.total_points ?? 0,
            };
          })
          .filter((entry): entry is StandingEntry => entry != null)
          .sort((a, b) => b.total - a.total)
          .map((entry, index) => ({
            ...entry,
            rank: index + 1,
            last_rank: index + 1,
          }));

        setDisplayEntries(correctedEntries);
      } else {
        setDisplayEntries(standingsData.standings.results);
      }

      if (isHistorical) {
        setFixtures([]);
      }

      setError(null);
    } catch (e: any) {
      setError(e.message ?? 'Failed to load standings');
    }
  }, [id, selectedGameweek]);

  useEffect(() => {
    setIsLoading(true);
    loadData().finally(() => setIsLoading(false));
  }, [loadData]);

  const handleRefresh = useCallback(async () => {
    setIsRefreshing(true);
    await loadData();
    setIsRefreshing(false);
  }, [loadData]);

  const toggleFavorite = useCallback(async () => {
    if (isFavorite) {
      await Storage.removeFavoriteLeague();
      setIsFavorite(false);
      return;
    }

    if (standings?.league.name) {
      await Storage.saveFavoriteLeague(id, standings.league.name);
      setIsFavorite(true);
    }
  }, [id, isFavorite, standings]);

  const displayGameweek = selectedGameweek ?? currentEvent;
  const isHistorical = selectedGameweek != null && selectedGameweek !== currentEvent;
  const entries = displayEntries;
  const hasLiveFixtures = useMemo(
    () => fixtures.some((fixture) =>
      fixture.started === true && fixture.finished === false && fixture.finished_provisional === false,
    ),
    [fixtures],
  );

  if (isLoading) {
    return (
      <View style={styles.center}>
        <ActivityIndicator color={Colors.primary} size="large" />
      </View>
    );
  }

  if (error || !standings) {
    return (
      <View style={[styles.center, { padding: 24 }]}>
        <Text style={styles.errorText}>⚠ {error ?? 'No data'}</Text>
        <TouchableOpacity style={styles.retryBtn} onPress={loadData}>
          <Text style={styles.retryBtnText}>Retry</Text>
        </TouchableOpacity>
      </View>
    );
  }

  return (
    <View style={[styles.root, { paddingTop: insets.top }]}>
      <View style={styles.topBar}>
        <TouchableOpacity style={styles.iconWrap} onPress={() => router.back()} hitSlop={10}>
          <Ionicons name="arrow-back" size={22} color={Colors.primary} />
        </TouchableOpacity>

        <Text style={styles.topBarTitle}>PITCH-SIDE GALLERY</Text>

        <View style={styles.topBarActions}>
          <TouchableOpacity
            style={styles.iconWrap}
            onPress={() => setShowChipHistory((value) => !value)}
            hitSlop={10}
          >
            <Ionicons
              name="albums"
              size={18}
              color={showChipHistory ? Colors.primary : Colors.onSurfaceVariant}
            />
          </TouchableOpacity>
          <TouchableOpacity style={styles.iconWrap} onPress={toggleFavorite} hitSlop={10}>
            <Ionicons
              name={isFavorite ? 'star' : 'star-outline'}
              size={21}
              color={isFavorite ? Colors.secondary : Colors.onSurfaceVariant}
            />
          </TouchableOpacity>
          <TouchableOpacity style={styles.iconWrap} onPress={handleRefresh} hitSlop={10}>
            <Ionicons name="refresh" size={21} color={Colors.onSurfaceVariant} />
          </TouchableOpacity>
        </View>
      </View>

      {showChipHistory ? (
        <FlatList
          data={entries}
          keyExtractor={(item) => String(item.id)}
          refreshControl={(
            <RefreshControl
              refreshing={isRefreshing}
              onRefresh={handleRefresh}
              tintColor={Colors.primary}
            />
          )}
          ListHeaderComponent={(
            <>
              <View style={styles.chipsHero}>
                <Text style={styles.chipsLeagueName}>{standings.league.name.toUpperCase()}</Text>
                <Text style={styles.chipsTitle}>LEAGUE CHIPS</Text>
              </View>

              <View style={styles.seasonProgressRow}>
                <View style={styles.seasonProgressCard}>
                  <Text style={styles.seasonProgressLabel}>SEASON PROGRESS</Text>
                  <Text style={styles.seasonProgressValue}>GW {currentEvent} / 38</Text>
                </View>
              </View>

              <View style={styles.legendRow}>
                <ChipLegendCard chip="wildcard" title="WILDCARD" />
                <ChipLegendCard chip="freehit" title="FREE HIT" />
              </View>
              <View style={styles.legendRow}>
                <ChipLegendCard chip="bboost" title="BENCH BOOST" />
                <ChipLegendCard chip="3xc" title="TRIPLE CPT" />
              </View>
            </>
          )}
          renderItem={({ item }) => (
            <ChipHistoryRow
              entry={item}
              isUserTeam={item.entry === currentManagerId}
              chips={managerMetaMap.get(item.entry)?.allChips ?? []}
              onChipPress={(eventId) => router.push(`/formation/${item.entry}/${eventId}`)}
            />
          )}
          ItemSeparatorComponent={() => <View style={{ height: 10 }} />}
          contentContainerStyle={styles.chipsListContent}
          showsVerticalScrollIndicator={false}
        />
      ) : (
        <FlatList
          data={entries}
          keyExtractor={(item) => String(item.id)}
          refreshControl={(
            <RefreshControl
              refreshing={isRefreshing}
              onRefresh={handleRefresh}
              tintColor={Colors.primary}
            />
          )}
          ListHeaderComponent={(
            <>
              <View style={styles.standingsHero}>
                <Text style={styles.standingsTitle}>LEAGUE STANDINGS</Text>
                <View style={styles.leaguePill}>
                  <Text style={styles.leaguePillName}>{standings.league.name}</Text>
                  <Text style={styles.leaguePillMeta}> • {entries.length} managers</Text>
                </View>
              </View>

              <View style={styles.gameweekRow}>
                <Text style={styles.gameweekLabel}>Standings after:</Text>
                <TouchableOpacity style={styles.gameweekButton} onPress={() => setShowGwModal(true)}>
                  <Text style={[styles.gameweekButtonText, isHistorical && styles.gameweekButtonTextActive]}>
                    GW {displayGameweek}
                  </Text>
                  <Ionicons name="chevron-down" size={16} color={Colors.onSurfaceVariant} />
                </TouchableOpacity>
              </View>

              {isHistorical && (
                <View style={styles.historicalBanner}>
                  <Ionicons name="time-outline" size={14} color={Colors.primary} />
                  <Text style={styles.historicalBannerText}>
                    Historical view, standings as of end of GW {displayGameweek}
                  </Text>
                  <TouchableOpacity onPress={() => setSelectedGameweek(null)}>
                    <Text style={styles.historicalBannerAction}>Live</Text>
                  </TouchableOpacity>
                </View>
              )}

              <View style={styles.tableHeader}>
                <Text style={[styles.tableCol, { width: 42, textAlign: 'left' }]}>RK</Text>
                <Text style={[styles.tableCol, styles.managerHeader]}>TEAM & MANAGER</Text>
                {!isHistorical && hasLiveFixtures && (
                  <>
                    <Text style={[styles.tableCol, styles.statusHeader]}>▶</Text>
                    <Text style={[styles.tableCol, styles.statusHeader]}>⌛</Text>
                  </>
                )}
                <Text style={[styles.tableCol, styles.pointsHeader]}>{isHistorical ? `GW${displayGameweek}` : 'GW PTS'}</Text>
                <Text style={[styles.tableCol, styles.pointsHeader]}>TOTAL</Text>
              </View>
            </>
          )}
          renderItem={({ item }) => {
            const meta = managerMetaMap.get(item.entry);
            const livePoints = hasLiveFixtures ? (meta?.livePoints ?? 0) : 0;
            const chip = meta?.activeChip ?? null;
            const captainName = meta?.captainName ?? null;
            const isUserTeam = item.entry === currentManagerId;
            const gwDisplayValue = item.event_total + livePoints;

            return (
              <TouchableOpacity
                style={[styles.entryCard, isUserTeam && styles.entryCardActive]}
                onPress={() => router.push(`/formation/${item.entry}/${displayGameweek}`)}
                activeOpacity={0.84}
              >
                {isUserTeam && <View style={styles.activeRowBar} />}

                <Text style={[styles.rankNum, isUserTeam && styles.rankNumActive]}>
                  {String(item.rank).padStart(2, '0')}
                </Text>

                <View style={styles.managerCell}>
                  <View style={styles.managerNameRow}>
                    <Text style={styles.managerName} numberOfLines={1}>
                      {item.entry_name.toUpperCase()}
                    </Text>
                    {chip && (
                      <View style={[styles.chipBadge, { backgroundColor: chipBg(chip) }]}>
                        <Text style={[styles.chipBadgeText, { color: chipFg(chip) }]}>
                          {chipShort(chip)}
                        </Text>
                      </View>
                    )}
                  </View>
                  <Text style={[styles.playerName, isUserTeam && styles.playerNameActive]} numberOfLines={1}>
                    {item.player_name.toUpperCase()}
                  </Text>
                  {captainName && (
                    <Text style={styles.captainText} numberOfLines={1}>© {captainName}</Text>
                  )}
                </View>

                {!isHistorical && hasLiveFixtures && (
                  <>
                    <Text style={styles.statusCell}>
                      {(meta?.inPlay ?? 0) > 0 ? meta?.inPlay : '–'}
                    </Text>
                    <Text style={styles.statusCell}>
                      {(meta?.toStart ?? 0) > 0 ? meta?.toStart : '–'}
                    </Text>
                  </>
                )}

                <View style={styles.pointsWrap}>
                  <Text style={[styles.pointsCell, isUserTeam && styles.pointsCellActive]}>
                    {gwDisplayValue}
                  </Text>
                  {!isHistorical && livePoints > 0 && hasLiveFixtures && (
                    <Text style={styles.liveDeltaText}>+{livePoints} live</Text>
                  )}
                </View>
                <Text style={styles.totalCell}>{formatTotal(item.total)}</Text>
              </TouchableOpacity>
            );
          }}
          ItemSeparatorComponent={() => <View style={{ height: 10 }} />}
          contentContainerStyle={styles.listContent}
          showsVerticalScrollIndicator={false}
        />
      )}

      <AppBottomSheet
        visible={showGwModal}
        onClose={() => setShowGwModal(false)}
        snapPoints={['46%']}
      >
        <View style={styles.modalContent}>
          <View style={styles.modalCard}>
            <TouchableOpacity
              style={[styles.modalItem, selectedGameweek == null && styles.modalItemActive]}
              onPress={() => {
                setSelectedGameweek(null);
                setShowGwModal(false);
              }}
            >
              <Text style={[styles.modalItemText, selectedGameweek == null && styles.modalItemTextActive]}>
                GW {currentEvent} (Live)
              </Text>
            </TouchableOpacity>

            <BottomSheetScrollView
              style={styles.modalList}
              showsVerticalScrollIndicator={false}
              contentContainerStyle={{ paddingBottom: insets.bottom + 8 }}
            >
              {[...availableGameweeks].reverse().map((gw) => {
                if (gw === currentEvent) return null;
                const active = selectedGameweek === gw;
                return (
                  <TouchableOpacity
                    key={gw}
                    style={[styles.modalItem, active && styles.modalItemActive]}
                    onPress={() => {
                      setSelectedGameweek(gw);
                      setShowGwModal(false);
                    }}
                  >
                    <Text style={[styles.modalItemText, active && styles.modalItemTextActive]}>
                      GW {gw}
                    </Text>
                  </TouchableOpacity>
                );
              })}
            </BottomSheetScrollView>
          </View>
        </View>
      </AppBottomSheet>
    </View>
  );
}

function ChipLegendCard({ chip, title }: { chip: string; title: string }) {
  return (
    <View style={styles.legendCard}>
      <View style={[styles.legendBadge, { backgroundColor: chipBg(chip) }]}>
        <Text style={[styles.legendBadgeText, { color: chipFg(chip) }]}>{chipShort(chip)}</Text>
      </View>
      <Text style={styles.legendTitle}>{title}</Text>
    </View>
  );
}

function ChipHistoryRow({
  entry,
  isUserTeam,
  chips,
  onChipPress,
}: {
  entry: StandingEntry;
  isUserTeam: boolean;
  chips: UsedChip[];
  onChipPress: (eventId: number) => void;
}) {
  return (
    <View style={[styles.chipRowCard, isUserTeam && styles.chipRowCardActive]}>
      {isUserTeam && <View style={styles.activeRowBar} />}

      <View style={styles.chipRowHeader}>
        <Text style={[styles.chipRowRank, isUserTeam && styles.rankNumActive]}>
          {String(entry.rank).padStart(2, '0')}
        </Text>
        <View style={styles.chipRowMeta}>
          <Text style={styles.chipRowTeam} numberOfLines={1}>{entry.entry_name.toUpperCase()}</Text>
          <Text style={styles.chipRowManager} numberOfLines={1}>
            {entry.player_name} | {formatTotal(entry.total)} pts
          </Text>
        </View>
      </View>

      <View style={styles.chipTokensWrap}>
        {chips.map((chip) => (
          <TouchableOpacity
            key={`${chip.name}-${chip.event}-${chip.number}`}
            style={styles.chipToken}
            onPress={() => onChipPress(chip.event)}
            activeOpacity={0.82}
          >
            <View style={[styles.chipTokenBadge, { backgroundColor: chipBg(chip.name) }]}>
              <Text style={[styles.chipTokenBadgeText, { color: chipFg(chip.name) }]}>
                {chipLabel(chip.name, chip.number)}
              </Text>
            </View>
            <Text style={styles.chipTokenEvent}>GW {chip.event}</Text>
          </TouchableOpacity>
        ))}
      </View>
    </View>
  );
}

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
    alignItems: 'center',
    paddingHorizontal: 10,
    paddingVertical: 8,
  },
  iconWrap: {
    width: 34,
    height: 34,
    alignItems: 'center',
    justifyContent: 'center',
  },
  topBarTitle: {
    flex: 1,
    fontSize: 15,
    fontWeight: '700',
    letterSpacing: 1,
    color: Colors.primary,
    marginLeft: 4,
  },
  topBarActions: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 2,
  },
  standingsHero: {
    paddingHorizontal: 20,
    paddingTop: 22,
    paddingBottom: 12,
  },
  standingsTitle: {
    fontSize: 28,
    fontWeight: '300',
    color: Colors.onSurface,
    marginBottom: 14,
  },
  leaguePill: {
    alignSelf: 'flex-start',
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: Colors.surfaceContainer,
    borderRadius: 10,
    paddingHorizontal: 12,
    paddingVertical: 9,
  },
  leaguePillName: {
    fontSize: 15,
    fontWeight: '600',
    color: Colors.secondary,
  },
  leaguePillMeta: {
    fontSize: 12,
    color: Colors.onSurfaceVariant,
  },
  gameweekRow: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingHorizontal: 20,
    paddingTop: 8,
    paddingBottom: 10,
  },
  gameweekLabel: {
    fontSize: 12,
    color: Colors.onSurfaceVariant,
  },
  gameweekButton: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
    backgroundColor: Colors.surfaceHigh,
    borderRadius: Radius.full,
    paddingHorizontal: 18,
    paddingVertical: 11,
    borderWidth: 1,
    borderColor: 'rgba(255,255,255,0.08)',
  },
  gameweekButtonText: {
    fontSize: 15,
    fontWeight: '700',
    color: Colors.onSurface,
  },
  gameweekButtonTextActive: {
    color: Colors.primary,
  },
  historicalBanner: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
    backgroundColor: 'rgba(45,90,39,0.25)',
    paddingHorizontal: 16,
    paddingVertical: 8,
    marginBottom: 6,
  },
  historicalBannerText: {
    flex: 1,
    fontSize: 11,
    color: Colors.primary,
  },
  historicalBannerAction: {
    fontSize: 11,
    fontWeight: '700',
    color: Colors.secondary,
  },
  tableHeader: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: 20,
    paddingVertical: 10,
  },
  tableCol: {
    fontSize: 10,
    fontWeight: '700',
    color: Colors.secondary,
    letterSpacing: 1.3,
  },
  managerHeader: {
    flex: 1,
  },
  statusHeader: {
    width: 28,
    textAlign: 'center',
    color: Colors.secondary,
    opacity: 0.7,
  },
  pointsHeader: {
    width: 64,
    textAlign: 'right',
  },
  listContent: {
    paddingHorizontal: 8,
    paddingBottom: 24,
  },
  entryCard: {
    marginHorizontal: 0,
    backgroundColor: Colors.surface,
    borderRadius: 12,
    paddingHorizontal: 14,
    paddingVertical: 14,
    flexDirection: 'row',
    alignItems: 'center',
    overflow: 'hidden',
  },
  entryCardActive: {
    backgroundColor: '#3B6E2C',
  },
  activeRowBar: {
    position: 'absolute',
    left: 0,
    top: 0,
    bottom: 0,
    width: 4,
    backgroundColor: Colors.primary,
  },
  rankNum: {
    width: 42,
    fontSize: 18,
    fontWeight: '300',
    color: 'rgba(255,255,255,0.38)',
  },
  rankNumActive: {
    color: Colors.primary,
  },
  managerCell: {
    flex: 1,
    paddingRight: 10,
  },
  managerNameRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 6,
    marginBottom: 2,
  },
  managerName: {
    flex: 1,
    fontSize: 14,
    fontWeight: '700',
    color: Colors.onSurface,
  },
  playerName: {
    fontSize: 11,
    color: Colors.onSurfaceVariant,
    marginBottom: 2,
  },
  playerNameActive: {
    color: 'rgba(161,212,148,0.85)',
  },
  captainText: {
    fontSize: 11,
    color: Colors.primary,
  },
  statusCell: {
    width: 28,
    fontSize: 13,
    color: Colors.onSurfaceVariant,
    textAlign: 'center',
  },
  pointsWrap: {
    width: 78,
    alignItems: 'flex-end',
  },
  chipBadge: {
    borderRadius: 2,
    paddingHorizontal: 6,
    paddingVertical: 4,
  },
  chipBadgeText: {
    fontSize: 9,
    fontWeight: '800',
  },
  pointsCell: {
    fontSize: 17,
    fontWeight: '500',
    color: Colors.onSurface,
    textAlign: 'right',
  },
  pointsCellActive: {
    color: Colors.primary,
  },
  liveDeltaText: {
    fontSize: 10,
    color: Colors.primary,
    marginTop: 2,
  },
  totalCell: {
    width: 74,
    fontSize: 18,
    fontWeight: '500',
    color: Colors.onSurface,
    textAlign: 'right',
  },
  chipsHero: {
    paddingHorizontal: 20,
    paddingTop: 20,
    paddingBottom: 8,
  },
  chipsLeagueName: {
    fontSize: 11,
    fontWeight: '700',
    color: Colors.primary,
    letterSpacing: 2,
    marginBottom: 4,
  },
  chipsTitle: {
    fontSize: 28,
    fontWeight: '300',
    color: Colors.onSurface,
  },
  seasonProgressRow: {
    paddingHorizontal: 20,
    alignItems: 'flex-end',
    marginBottom: 16,
  },
  seasonProgressCard: {
    backgroundColor: Colors.surface,
    borderRadius: 12,
    borderWidth: 1,
    borderColor: 'rgba(255,255,255,0.05)',
    paddingHorizontal: 14,
    paddingVertical: 10,
  },
  seasonProgressLabel: {
    fontSize: 9,
    letterSpacing: 1.2,
    color: Colors.onSurfaceVariant,
    marginBottom: 4,
  },
  seasonProgressValue: {
    fontSize: 18,
    fontWeight: '700',
    color: Colors.secondary,
  },
  legendRow: {
    flexDirection: 'row',
    gap: 8,
    paddingHorizontal: 16,
    marginBottom: 8,
  },
  legendCard: {
    flex: 1,
    flexDirection: 'row',
    alignItems: 'center',
    gap: 10,
    backgroundColor: Colors.surfaceContainer,
    borderRadius: 12,
    borderWidth: 1,
    borderColor: 'rgba(255,255,255,0.05)',
    padding: 12,
  },
  legendBadge: {
    width: 32,
    height: 32,
    borderRadius: 4,
    alignItems: 'center',
    justifyContent: 'center',
  },
  legendBadgeText: {
    fontSize: 11,
    fontWeight: '800',
  },
  legendTitle: {
    fontSize: 12,
    color: Colors.onSurfaceVariant,
  },
  chipsListContent: {
    paddingBottom: 24,
  },
  chipRowCard: {
    marginHorizontal: 12,
    backgroundColor: Colors.surface,
    borderRadius: 12,
    paddingHorizontal: 14,
    paddingVertical: 14,
    overflow: 'hidden',
  },
  chipRowCardActive: {
    backgroundColor: '#253E1B',
  },
  chipRowHeader: {
    flexDirection: 'row',
    marginBottom: 12,
  },
  chipRowRank: {
    width: 42,
    fontSize: 18,
    fontWeight: '300',
    color: 'rgba(255,255,255,0.38)',
  },
  chipRowMeta: {
    flex: 1,
  },
  chipRowTeam: {
    fontSize: 14,
    fontWeight: '700',
    color: Colors.onSurface,
    marginBottom: 2,
  },
  chipRowManager: {
    fontSize: 12,
    color: Colors.onSurfaceVariant,
  },
  chipTokensWrap: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: 8,
  },
  chipToken: {
    alignItems: 'center',
  },
  chipTokenBadge: {
    minWidth: 34,
    borderRadius: 4,
    paddingHorizontal: 9,
    paddingVertical: 8,
    alignItems: 'center',
  },
  chipTokenBadgeText: {
    fontSize: 11,
    fontWeight: '800',
  },
  chipTokenEvent: {
    marginTop: 4,
    fontSize: 10,
    color: Colors.onSurfaceVariant,
  },
  modalContent: {
    flex: 1,
    paddingHorizontal: 28,
    paddingBottom: 20,
  },
  modalCard: {
    flex: 1,
    backgroundColor: Colors.surfaceContainer,
    borderRadius: 16,
    paddingVertical: 8,
    borderWidth: 1,
    borderColor: 'rgba(255,255,255,0.06)',
  },
  modalList: {
    flex: 1,
  },
  modalItem: {
    paddingHorizontal: 18,
    paddingVertical: 14,
  },
  modalItemActive: {
    backgroundColor: 'rgba(45,90,39,0.32)',
  },
  modalItemText: {
    fontSize: 15,
    color: Colors.onSurface,
  },
  modalItemTextActive: {
    color: Colors.primary,
    fontWeight: '700',
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
