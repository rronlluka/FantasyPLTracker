/**
 * Leagues Tab
 * Mirrors the Android app's favourite league hero and overall rank layout.
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
import { Ionicons } from '@expo/vector-icons';
import { useRouter } from 'expo-router';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { Colors, Radius } from '@/constants/theme';
import { Api } from '@/services/api';
import { LeagueInfo, ManagerData, ManagerHistory } from '@/types/fpl';
import { Storage } from '@/utils/storage';

function formatRank(value: number): string {
  return new Intl.NumberFormat().format(value);
}

function formatRankDeltaShort(value: number): string {
  const absolute = Math.abs(value);
  if (absolute >= 1_000_000) return `${Math.floor(absolute / 1_000_000)}M`;
  if (absolute >= 1_000) return `${Math.floor(absolute / 1_000)}K`;
  return String(absolute);
}

function getOverallRankDelta(managerData: ManagerData | null, history: ManagerHistory | null): number | null {
  if (!managerData) return null;
  const previousGw = managerData.current_event - 1;
  if (previousGw < 1) return null;
  const previousEntry = history?.current?.find((entry) => entry.event === previousGw);
  if (!previousEntry) return null;
  return previousEntry.overall_rank - managerData.summary_overall_rank;
}

export default function LeaguesScreen() {
  const router = useRouter();
  const insets = useSafeAreaInsets();

  const [managerId, setManagerId] = useState<number | null>(null);
  const [managerData, setManagerData] = useState<ManagerData | null>(null);
  const [managerHistory, setManagerHistory] = useState<ManagerHistory | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [isRefreshing, setIsRefreshing] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [favoriteLeagueId, setFavoriteLeagueId] = useState<number | null>(null);

  const loadData = useCallback(async (id: number) => {
    try {
      const [data, history, favoriteId] = await Promise.all([
        Api.getManagerData(id),
        Api.getManagerHistory(id),
        Storage.getFavoriteLeagueId(),
      ]);
      setManagerData(data);
      setManagerHistory(history);
      setFavoriteLeagueId(favoriteId);
      setError(null);
    } catch (e: any) {
      setError(e.message ?? 'Failed to load data');
    }
  }, []);

  useEffect(() => {
    let isMounted = true;

    async function bootstrap() {
      const id = await Storage.getManagerId();
      if (!id) {
        router.replace('/login');
        return;
      }
      if (!isMounted) return;

      setManagerId(id);
      await loadData(id);
      if (isMounted) {
        setIsLoading(false);
      }
    }

    bootstrap();

    return () => {
      isMounted = false;
    };
  }, [loadData, router]);

  const handleRefresh = useCallback(async () => {
    if (!managerId) return;
    setIsRefreshing(true);
    await loadData(managerId);
    setIsRefreshing(false);
  }, [loadData, managerId]);

  const handleLogout = useCallback(async () => {
    await Storage.clearAll();
    router.replace('/login');
  }, [router]);

  const toggleFavorite = useCallback(async (league: LeagueInfo) => {
    if (favoriteLeagueId === league.id) {
      await Storage.removeFavoriteLeague();
      setFavoriteLeagueId(null);
      return;
    }

    await Storage.saveFavoriteLeague(league.id, league.name);
    setFavoriteLeagueId(league.id);
  }, [favoriteLeagueId]);

  const leagues = useMemo(() => managerData?.leagues.classic ?? [], [managerData]);
  const favoriteLeague = leagues.find((league) => league.id === favoriteLeagueId) ?? null;
  const sortedLeagues = useMemo(() => {
    if (!favoriteLeague) {
      return [...leagues].sort((a, b) => (a.entry_rank ?? Number.MAX_SAFE_INTEGER) - (b.entry_rank ?? Number.MAX_SAFE_INTEGER));
    }

    return [
      favoriteLeague,
      ...leagues
        .filter((league) => league.id !== favoriteLeague.id)
        .sort((a, b) => (a.entry_rank ?? Number.MAX_SAFE_INTEGER) - (b.entry_rank ?? Number.MAX_SAFE_INTEGER)),
    ];
  }, [favoriteLeague, leagues]);

  const overallRankDelta = getOverallRankDelta(managerData, managerHistory);

  if (isLoading) {
    return (
      <View style={[styles.center, { backgroundColor: Colors.background }]}>
        <ActivityIndicator color={Colors.primary} size="large" />
      </View>
    );
  }

  if (error && !managerData) {
    return (
      <View style={[styles.center, styles.errorScreen]}>
        <Text style={styles.errorText}>⚠ {error}</Text>
        <TouchableOpacity style={styles.retryButton} onPress={() => managerId && loadData(managerId)}>
          <Text style={styles.retryButtonText}>Retry</Text>
        </TouchableOpacity>
      </View>
    );
  }

  return (
    <View style={[styles.root, { paddingTop: insets.top }]}>
      <View style={styles.header}>
        <Text style={styles.headerTitle}>FPL Tracker</Text>
        <TouchableOpacity style={styles.iconButton} onPress={handleLogout} hitSlop={12}>
          <Ionicons name="log-out-outline" size={22} color={Colors.onSurface} />
        </TouchableOpacity>
      </View>

      <FlatList
        data={sortedLeagues}
        keyExtractor={(item) => String(item.id)}
        contentContainerStyle={styles.listContent}
        showsVerticalScrollIndicator={false}
        refreshControl={(
          <RefreshControl
            refreshing={isRefreshing}
            onRefresh={handleRefresh}
            tintColor={Colors.primary}
          />
        )}
        ListHeaderComponent={managerData ? (
          <View style={styles.heroSection}>
            <Text style={styles.sectionEyebrow}>MY LEAGUES</Text>
            <Text style={styles.managerTeamName} numberOfLines={1}>{managerData.name}</Text>
            <Text style={styles.managerName}>
              {managerData.player_first_name} {managerData.player_last_name}
            </Text>

            {favoriteLeague ? (
              <>
                <FavoriteLeagueHeroCard
                  league={favoriteLeague}
                  onPress={() => router.push(`/league/${favoriteLeague.id}`)}
                />
                <OverallRankStrip
                  overallRank={managerData.summary_overall_rank}
                  overallPoints={managerData.summary_overall_points}
                  rankDelta={overallRankDelta}
                />
              </>
            ) : (
              <OverallRankHeroCard
                overallRank={managerData.summary_overall_rank}
                overallPoints={managerData.summary_overall_points}
                rankDelta={overallRankDelta}
                currentGw={managerData.current_event}
              />
            )}

            <View style={styles.listHeaderRow}>
              <Text style={styles.listHeaderTitle}>All Leagues</Text>
              <Text style={styles.listHeaderCount}>{leagues.length} leagues</Text>
            </View>
          </View>
        ) : null}
        renderItem={({ item }) => (
          <LeagueRow
            league={item}
            isFavorite={favoriteLeagueId === item.id}
            onPress={() => router.push(`/league/${item.id}`)}
            onToggleFavorite={() => toggleFavorite(item)}
          />
        )}
        ItemSeparatorComponent={() => <View style={{ height: 10 }} />}
      />
    </View>
  );
}

function FavoriteLeagueHeroCard({
  league,
  onPress,
}: {
  league: LeagueInfo;
  onPress: () => void;
}) {
  const rankChange =
    league.entry_rank != null && league.entry_last_rank != null
      ? league.entry_last_rank - league.entry_rank
      : 0;
  const isUp = rankChange > 0;

  return (
    <TouchableOpacity activeOpacity={0.88} onPress={onPress}>
      <View style={styles.favoriteHero}>
        <View style={styles.favoriteEyebrowRow}>
          <Ionicons name="star" size={12} color={Colors.secondary} />
          <Text style={styles.favoriteEyebrow}>FAVOURITE LEAGUE</Text>
        </View>

        <View style={styles.favoriteHeroRow}>
          <View style={styles.favoriteHeroContent}>
            <Text style={styles.favoriteLeagueName}>{league.name}</Text>
            {league.entry_rank != null && (
              <View style={styles.favoriteRankRow}>
                <Text style={styles.favoriteRankLabel}>Your rank</Text>
                <View style={styles.favoriteRankChip}>
                  <Text style={styles.favoriteRankChipText}>#{formatRank(league.entry_rank)}</Text>
                </View>
                {rankChange !== 0 && (
                  <View style={styles.favoriteRankDeltaRow}>
                    <Ionicons
                      name={isUp ? 'caret-up' : 'caret-down'}
                      size={11}
                      color={isUp ? Colors.primary : '#FF6B6B'}
                    />
                    <Text
                      style={[
                        styles.favoriteRankDeltaText,
                        { color: isUp ? Colors.primary : '#FF6B6B' },
                      ]}
                    >
                      {formatRankDeltaShort(rankChange)}
                    </Text>
                  </View>
                )}
              </View>
            )}
          </View>

          <View style={styles.favoriteArrowCircle}>
            <Ionicons name="arrow-forward" size={16} color={Colors.primary} />
          </View>
        </View>
      </View>
    </TouchableOpacity>
  );
}

function OverallRankStrip({
  overallRank,
  overallPoints,
  rankDelta,
}: {
  overallRank: number;
  overallPoints: number;
  rankDelta: number | null;
}) {
  return (
    <View style={styles.rankStrip}>
      <View>
        <Text style={styles.stripLabel}>OVERALL RANK</Text>
        <View style={styles.stripValueRow}>
          <Text style={styles.stripRankValue}>#{formatRank(overallRank)}</Text>
          <RankDelta delta={rankDelta} compact />
        </View>
      </View>

      <View style={styles.stripRight}>
        <Text style={styles.stripLabel}>TOTAL POINTS</Text>
        <Text style={styles.stripPointsValue}>{overallPoints} pts</Text>
      </View>
    </View>
  );
}

function OverallRankHeroCard({
  overallRank,
  overallPoints,
  rankDelta,
  currentGw,
}: {
  overallRank: number;
  overallPoints: number;
  rankDelta: number | null;
  currentGw: number;
}) {
  return (
    <View style={styles.overallHero}>
      <View style={styles.overallHeroLeft}>
        <View style={styles.globalArenaPill}>
          <Text style={styles.globalArenaPillText}>GLOBAL ARENA</Text>
        </View>
        <Text style={styles.overallHeroTitle}>OVERALL</Text>
        <Text style={styles.overallHeroSubtitle}>Global Ranking · GW {currentGw}</Text>
        <Text style={styles.overallHeroHint}>⭐ Star a league to pin it here</Text>
      </View>

      <View style={styles.overallHeroRight}>
        <Text style={styles.overallHeroRank}>{formatRank(overallRank)}</Text>
        <RankDelta delta={rankDelta} withDirectionWord />
        <Text style={styles.overallHeroPoints}>{overallPoints} pts</Text>
      </View>
    </View>
  );
}

function RankDelta({
  delta,
  compact = false,
  withDirectionWord = false,
}: {
  delta: number | null;
  compact?: boolean;
  withDirectionWord?: boolean;
}) {
  if (delta == null) return null;

  if (delta === 0) {
    return <Text style={compact ? styles.compactNoChange : styles.noChange}>No change</Text>;
  }

  const isUp = delta > 0;
  const color = isUp ? Colors.primary : '#FF6B6B';
  const label = withDirectionWord
    ? `${isUp ? 'UP' : 'DOWN'} ${formatRankDeltaShort(delta)}`
    : formatRankDeltaShort(delta);

  return (
    <View style={compact ? styles.compactDeltaRow : styles.deltaRow}>
      <Ionicons name={isUp ? 'caret-up' : 'caret-down'} size={compact ? 12 : 14} color={color} />
      <Text style={[compact ? styles.compactDeltaText : styles.deltaText, { color }]}>
        {label}
      </Text>
    </View>
  );
}

function LeagueRow({
  league,
  isFavorite,
  onPress,
  onToggleFavorite,
}: {
  league: LeagueInfo;
  isFavorite: boolean;
  onPress: () => void;
  onToggleFavorite: () => void;
}) {
  const rank = league.entry_rank;
  const lastRank = league.entry_last_rank;

  let changeText = 'No change';
  let changeColor = 'rgba(255,255,255,0.35)';
  if (rank != null && lastRank != null) {
    const diff = lastRank - rank;
    if (diff > 0) {
      changeText = `▼ ${formatRankDeltaShort(diff)} this GW`;
      changeColor = '#FF6B6B';
    } else if (diff < 0) {
      changeText = `▲ ${formatRankDeltaShort(diff)} this GW`;
      changeColor = Colors.primary;
    }
  }

  return (
    <TouchableOpacity style={styles.leagueRow} activeOpacity={0.82} onPress={onPress}>
      <View style={[styles.rankBadge, isFavorite && styles.rankBadgeFavorite]}>
        <Text style={[styles.rankBadgeText, isFavorite && styles.rankBadgeTextFavorite]}>
          {rank != null ? `#${rank}` : '–'}
        </Text>
      </View>

      <View style={styles.leagueInfo}>
        <Text style={styles.leagueName} numberOfLines={1}>{league.name}</Text>
        <Text style={[styles.leagueChange, { color: changeColor }]}>{changeText}</Text>
      </View>

      <TouchableOpacity onPress={onToggleFavorite} hitSlop={10} style={styles.starButton}>
        <Ionicons
          name={isFavorite ? 'star' : 'star-outline'}
          size={18}
          color={isFavorite ? Colors.secondary : 'rgba(255,255,255,0.25)'}
        />
      </TouchableOpacity>

      <Ionicons name="arrow-forward" size={15} color="rgba(255,255,255,0.25)" />
    </TouchableOpacity>
  );
}

const styles = StyleSheet.create({
  root: {
    flex: 1,
    backgroundColor: Colors.background,
  },
  center: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
  },
  errorScreen: {
    backgroundColor: Colors.background,
    paddingHorizontal: 24,
  },
  header: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingHorizontal: 14,
    paddingTop: 8,
    paddingBottom: 6,
  },
  headerTitle: {
    fontSize: 18,
    fontWeight: '500',
    color: Colors.onSurface,
  },
  iconButton: {
    width: 36,
    height: 36,
    alignItems: 'center',
    justifyContent: 'center',
  },
  listContent: {
    paddingBottom: 28,
  },
  heroSection: {
    paddingHorizontal: 8,
    paddingTop: 22,
    paddingBottom: 10,
  },
  sectionEyebrow: {
    fontSize: 11,
    fontWeight: '700',
    color: Colors.primary,
    letterSpacing: 2,
    marginBottom: 6,
  },
  managerTeamName: {
    fontSize: 28,
    fontWeight: '300',
    color: '#FFFFFF',
    marginBottom: 2,
  },
  managerName: {
    fontSize: 14,
    color: 'rgba(255,255,255,0.45)',
    marginBottom: 18,
  },
  favoriteHero: {
    borderRadius: 16,
    paddingHorizontal: 18,
    paddingVertical: 18,
    marginHorizontal: 6,
    marginBottom: 12,
    backgroundColor: '#2F6124',
    borderWidth: 1,
    borderColor: 'rgba(161,212,148,0.08)',
  },
  favoriteEyebrowRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 6,
    marginBottom: 12,
  },
  favoriteEyebrow: {
    fontSize: 10,
    fontWeight: '700',
    color: 'rgba(255,224,131,0.92)',
    letterSpacing: 1.5,
  },
  favoriteHeroRow: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    gap: 16,
  },
  favoriteHeroContent: {
    flex: 1,
  },
  favoriteLeagueName: {
    fontSize: 22,
    fontWeight: '300',
    lineHeight: 26,
    color: '#FFFFFF',
  },
  favoriteRankRow: {
    flexDirection: 'row',
    alignItems: 'center',
    flexWrap: 'wrap',
    gap: 8,
    marginTop: 12,
  },
  favoriteRankLabel: {
    fontSize: 13,
    color: 'rgba(255,255,255,0.55)',
  },
  favoriteRankChip: {
    backgroundColor: 'rgba(161,212,148,0.22)',
    borderRadius: 6,
    paddingHorizontal: 8,
    paddingVertical: 4,
  },
  favoriteRankChipText: {
    fontSize: 13,
    fontWeight: '700',
    color: Colors.primary,
  },
  favoriteRankDeltaRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 2,
  },
  favoriteRankDeltaText: {
    fontSize: 12,
    fontWeight: '700',
  },
  favoriteArrowCircle: {
    width: 42,
    height: 42,
    borderRadius: 21,
    backgroundColor: 'rgba(161,212,148,0.18)',
    alignItems: 'center',
    justifyContent: 'center',
  },
  rankStrip: {
    marginHorizontal: 6,
    backgroundColor: Colors.surfaceContainer,
    borderRadius: 14,
    paddingHorizontal: 18,
    paddingVertical: 16,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    marginBottom: 22,
  },
  stripLabel: {
    fontSize: 9,
    fontWeight: '700',
    color: 'rgba(255,255,255,0.4)',
    letterSpacing: 1.5,
    marginBottom: 6,
  },
  stripValueRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 6,
  },
  stripRankValue: {
    fontSize: 24,
    fontWeight: '900',
    color: Colors.primary,
  },
  stripRight: {
    alignItems: 'flex-end',
  },
  stripPointsValue: {
    fontSize: 24,
    fontWeight: '900',
    color: '#FFFFFF',
  },
  overallHero: {
    marginHorizontal: 6,
    backgroundColor: Colors.surfaceContainer,
    borderRadius: 16,
    borderWidth: 1,
    borderColor: 'rgba(255,255,255,0.05)',
    padding: 18,
    flexDirection: 'row',
    justifyContent: 'space-between',
    marginBottom: 22,
  },
  overallHeroLeft: {
    flex: 1,
    paddingRight: 16,
  },
  globalArenaPill: {
    alignSelf: 'flex-start',
    backgroundColor: Colors.primaryContainer,
    borderRadius: 4,
    paddingHorizontal: 8,
    paddingVertical: 6,
    marginBottom: 12,
  },
  globalArenaPillText: {
    fontSize: 10,
    fontWeight: '700',
    color: 'rgba(161,212,148,0.92)',
    letterSpacing: 1,
  },
  overallHeroTitle: {
    fontSize: 18,
    fontWeight: '700',
    color: Colors.onSurface,
    marginBottom: 4,
  },
  overallHeroSubtitle: {
    fontSize: 13,
    color: 'rgba(255,255,255,0.45)',
    marginBottom: 12,
  },
  overallHeroHint: {
    fontSize: 11,
    color: 'rgba(255,255,255,0.3)',
  },
  overallHeroRight: {
    alignItems: 'flex-end',
    justifyContent: 'flex-start',
  },
  overallHeroRank: {
    fontSize: 40,
    fontWeight: '900',
    color: Colors.secondary,
    letterSpacing: -1,
  },
  overallHeroPoints: {
    marginTop: 8,
    fontSize: 14,
    fontWeight: '600',
    color: 'rgba(255,255,255,0.55)',
  },
  deltaRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 2,
    marginTop: 2,
  },
  deltaText: {
    fontSize: 11,
    fontWeight: '700',
    letterSpacing: 0.5,
  },
  noChange: {
    marginTop: 4,
    fontSize: 11,
    color: 'rgba(255,255,255,0.4)',
  },
  compactDeltaRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 1,
  },
  compactDeltaText: {
    fontSize: 11,
    fontWeight: '700',
  },
  compactNoChange: {
    fontSize: 11,
    color: 'rgba(255,255,255,0.4)',
  },
  listHeaderRow: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingHorizontal: 6,
    marginBottom: 10,
  },
  listHeaderTitle: {
    fontSize: 16,
    fontWeight: '500',
    color: '#FFFFFF',
  },
  listHeaderCount: {
    fontSize: 13,
    color: 'rgba(255,255,255,0.35)',
  },
  leagueRow: {
    marginHorizontal: 14,
    backgroundColor: Colors.surface,
    borderRadius: 14,
    paddingHorizontal: 14,
    paddingVertical: 14,
    flexDirection: 'row',
    alignItems: 'center',
  },
  rankBadge: {
    width: 46,
    height: 46,
    borderRadius: 10,
    backgroundColor: 'rgba(255,255,255,0.07)',
    alignItems: 'center',
    justifyContent: 'center',
    marginRight: 14,
  },
  rankBadgeFavorite: {
    backgroundColor: 'rgba(45,90,39,0.6)',
  },
  rankBadgeText: {
    fontSize: 15,
    fontWeight: '900',
    color: Colors.primary,
  },
  rankBadgeTextFavorite: {
    color: Colors.secondary,
  },
  leagueInfo: {
    flex: 1,
    paddingRight: 8,
  },
  leagueName: {
    fontSize: 15,
    fontWeight: '600',
    color: '#FFFFFF',
    marginBottom: 2,
  },
  leagueChange: {
    fontSize: 12,
    fontWeight: '500',
  },
  starButton: {
    width: 36,
    height: 36,
    alignItems: 'center',
    justifyContent: 'center',
    marginRight: 2,
  },
  errorText: {
    color: Colors.tertiary,
    fontSize: 14,
    textAlign: 'center',
    marginBottom: 16,
  },
  retryButton: {
    backgroundColor: Colors.primaryContainer,
    borderRadius: Radius.md,
    paddingVertical: 10,
    paddingHorizontal: 24,
  },
  retryButtonText: {
    color: Colors.primary,
    fontWeight: '600',
  },
});
