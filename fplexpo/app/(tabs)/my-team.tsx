/**
 * My Team Tab (ManagerStatsScreen.kt port)
 * Manager profile with rank, value, GW history, chips, and transfers.
 */
import React, { useEffect, useState, useCallback } from 'react';
import {
  View, Text, FlatList, TouchableOpacity, ActivityIndicator,
  StyleSheet, RefreshControl,
} from 'react-native';
import { useRouter } from 'expo-router';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { Api } from '@/services/api';
import { Storage } from '@/utils/storage';
import {
  ManagerData, ManagerHistory, ChipPlay,
  BootstrapData, ManagerTransfer,
} from '@/types/fpl';
import { Colors, Radius } from '@/constants/theme';

function formatRank(n: number): string {
  return new Intl.NumberFormat().format(n);
}

function chipLabel(name: string, index: number): string {
  const n = index + 1;
  switch (name) {
    case 'bboost':   return n > 1 ? `BB${n}` : 'BB';
    case 'wildcard': return `WC${n}`;
    case '3xc':      return n > 1 ? `TC${n}` : 'TC';
    case 'freehit':  return n > 1 ? `FH${n}` : 'FH';
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
  const usages = chips
    .filter((chip) => chip.name === definition.identifier)
    .sort((a, b) => a.event - b.event);
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

export default function MyTeamScreen() {
  const router = useRouter();
  const insets = useSafeAreaInsets();

  const [managerId, setManagerId] = useState<number | null>(null);
  const [managerData, setManagerData] = useState<ManagerData | null>(null);
  const [managerHistory, setManagerHistory] = useState<ManagerHistory | null>(null);
  const [bootstrapData, setBootstrapData] = useState<BootstrapData | null>(null);
  const [transfers, setTransfers] = useState<ManagerTransfer[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [isRefreshing, setIsRefreshing] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [showAllGw, setShowAllGw] = useState(false);
  const [showAllTransfers, setShowAllTransfers] = useState(false);

  const loadData = useCallback(async (id: number) => {
    try {
      const [data, history, bootstrap, txfrs] = await Promise.all([
        Api.getManagerData(id),
        Api.getManagerHistory(id),
        Api.getBootstrapData(),
        Api.getManagerTransfers(id),
      ]);
      setManagerData(data);
      setManagerHistory(history);
      setBootstrapData(bootstrap);
      setTransfers(txfrs);
      setError(null);
    } catch (e: any) {
      setError(e.message ?? 'Failed to load data');
    }
  }, []);

  useEffect(() => {
    Storage.getManagerId().then((id) => {
      if (!id) { router.replace('/login'); return; }
      setManagerId(id);
      loadData(id).finally(() => setIsLoading(false));
    });
  }, []);

  const handleRefresh = async () => {
    if (!managerId) return;
    setIsRefreshing(true);
    await loadData(managerId);
    setIsRefreshing(false);
  };

  if (isLoading) {
    return (
      <View style={styles.center}>
        <ActivityIndicator color={Colors.primary} size="large" />
      </View>
    );
  }

  if (error || !managerData) {
    return (
      <View style={[styles.center, { padding: 24 }]}>
        <Text style={styles.errorText}>⚠️  {error ?? 'No data'}</Text>
        <TouchableOpacity
          style={styles.retryBtn}
          onPress={() => managerId && loadData(managerId).finally(() => setIsLoading(false))}
        >
          <Text style={styles.retryBtnText}>Retry</Text>
        </TouchableOpacity>
      </View>
    );
  }

  const currentHistory = managerHistory?.current ?? [];
  const chips = managerHistory?.chips ?? [];
  const currentEvent = bootstrapData?.events.find((event) => event.is_current)?.id ?? managerData.current_event;

  // ── Derived stats ─────────────────────────────────────────────────────────
  const latestGw = currentHistory[currentHistory.length - 1];
  const bankM = latestGw ? (latestGw.bank / 10).toFixed(1) : '0.0';
  const squadM = latestGw ? ((latestGw.value - latestGw.bank) / 10).toFixed(1) : '0.0';

  const bestRankEntry = [...currentHistory].sort((a, b) => a.overall_rank - b.overall_rank)[0];
  const highestPtsEntry = [...currentHistory].sort((a, b) => b.points - a.points)[0];

  const teamName = managerData.name || `${managerData.player_first_name} ${managerData.player_last_name}`;

  // Recent transfers (5 most recent GWs)
  const transfersByGw = transfers
    .sort((a, b) => b.event - a.event)
    .reduce((acc, t) => {
      if (!acc[t.event]) acc[t.event] = [];
      acc[t.event].push(t);
      return acc;
    }, {} as Record<number, ManagerTransfer[]>);
  const recentTransferGws = Object.entries(transfersByGw)
    .sort((a, b) => parseInt(b[0]) - parseInt(a[0]))
    .slice(0, showAllTransfers ? Object.keys(transfersByGw).length : 5);

  // GW history list
  const gwList = showAllGw
    ? [...currentHistory].reverse()
    : [...currentHistory].reverse().slice(0, 10);

  const chipDefinitions = orderedChipDefinitions(chips);

  const players = bootstrapData?.elements ?? [];
  const getPlayerName = (id: number) =>
    players.find((p) => p.id === id)?.web_name ?? `Player ${id}`;

  return (
    <FlatList
      style={styles.root}
      contentContainerStyle={{ paddingBottom: 40 }}
      refreshControl={
        <RefreshControl refreshing={isRefreshing} onRefresh={handleRefresh} tintColor={Colors.primary} />
      }
      ListHeaderComponent={
        <>
          {/* ── HERO ──────────────────────────────────────────────── */}
          <View style={[styles.hero, { paddingTop: insets.top + 16 }]}>
            <Text style={styles.heroRegion}>
              🏳  {managerData.player_region_name.toUpperCase()} · REGION
            </Text>
            <Text style={styles.heroName}>
              {managerData.player_first_name.toUpperCase()}{'\n'}
              {managerData.player_last_name.toUpperCase()}
            </Text>
            <View style={styles.heroTeamRow}>
              <Text style={styles.heroTeam}>{teamName}</Text>
              <Text style={styles.heroSep}> / </Text>
              <Text style={styles.heroId}>#{managerData.id}</Text>
            </View>

            {/* Rank chip */}
            <View style={styles.rankChip}>
              <View style={styles.rankChipDot} />
              <Text style={styles.rankChipText}>
                #{formatRank(managerData.summary_overall_rank)}  OVERALL
              </Text>
            </View>

            <TouchableOpacity
              style={styles.openTeamBtn}
              onPress={() => router.push(`/formation/${managerData.id}/${currentEvent}`)}
              activeOpacity={0.9}
            >
              <Text style={styles.openTeamBtnText}>Open My Team</Text>
              <Text style={styles.openTeamBtnSub}>GW {currentEvent}</Text>
            </TouchableOpacity>
          </View>

          {/* ── STAT GRID ──────────────────────────────────────────── */}
          <View style={styles.statGrid}>
            <StatCard label="Total Points" value={String(managerData.summary_overall_points)} />
            <StatCard label="GW Points" value={String(managerData.summary_event_points)} accent />
            <StatCard label="Squad Value" value={`£${squadM}m`} />
            <StatCard label="Bank" value={`£${bankM}m`} />
            <StatCard
              label="Best Rank"
              value={bestRankEntry ? `#${formatRank(bestRankEntry.overall_rank)}` : '—'}
              sub={bestRankEntry ? `GW ${bestRankEntry.event}` : undefined}
            />
            <StatCard
              label="Best GW Pts"
              value={highestPtsEntry ? String(highestPtsEntry.points) : '—'}
              sub={highestPtsEntry ? `GW ${highestPtsEntry.event}` : undefined}
            />
          </View>

          {/* ── CHIPS ─────────────────────────────────────────────── */}
          <>
            <SectionTitle title="CHIPS" />
            <View style={styles.chipGrid}>
              {chipDefinitions.map((chipDef) => {
                const usage = resolveChipUsage(chipDef, chips);
                return (
                  <View
                    key={chipDef.label}
                    style={[
                      chipStyles.chipCard,
                      usage == null && chipStyles.chipCardLocked,
                    ]}
                  >
                    <View
                      style={[
                        chipStyles.chipIcon,
                        { backgroundColor: usage ? chipDef.color : Colors.surfaceHigh },
                      ]}
                    >
                      <Text style={[chipStyles.chipIconText, { color: usage ? chipDef.textColor : Colors.outline }]}>
                        {usage ? chipDef.label.slice(0, 2) : 'L'}
                      </Text>
                    </View>
                    <Text style={[chipStyles.chipCardLabel, usage == null && chipStyles.chipCardLabelLocked]}>
                      {chipDef.label}
                    </Text>
                    <Text style={[chipStyles.chipCardSub, usage == null && chipStyles.chipCardSubLocked]}>
                      {usage ? `GW ${usage.event}` : 'UNUSED'}
                    </Text>
                  </View>
                );
              })}
            </View>
          </>

          {/* ── RECENT TRANSFERS ──────────────────────────────────── */}
          {recentTransferGws.length > 0 && (
            <>
              <SectionTitle title="RECENT TRANSFERS" />
              {recentTransferGws.map(([gw, txs]) => (
                <View key={gw} style={transferStyles.gwGroup}>
                  <View style={transferStyles.groupHeader}>
                    <Text style={transferStyles.gwLabel}>GW {gw}</Text>
                    <View style={transferStyles.groupMeta}>
                      {currentHistory.find((entry) => entry.event === Number(gw))?.event_transfers_cost ? (
                        <Text style={transferStyles.hitText}>
                          -{currentHistory.find((entry) => entry.event === Number(gw))?.event_transfers_cost} pts
                        </Text>
                      ) : null}
                      {chips.find((chip) => chip.event === Number(gw)) ? (
                        <Text style={transferStyles.chipText}>
                          {chipLabel(
                            chips.find((chip) => chip.event === Number(gw))!.name,
                            chips
                              .filter((chip) => chip.name === chips.find((entry) => entry.event === Number(gw))!.name && chip.event <= Number(gw))
                              .length - 1,
                          )}
                        </Text>
                      ) : null}
                    </View>
                  </View>
                  {txs.map((t, i) => (
                    <View key={i} style={transferStyles.row}>
                      <View style={[transferStyles.badge, { backgroundColor: Colors.tertiaryContainer + '33' }]}>
                        <Text style={[transferStyles.badgeText, { color: Colors.tertiary }]}>OUT</Text>
                      </View>
                      <Text style={transferStyles.playerName} numberOfLines={1}>
                        {getPlayerName(t.element_out)}
                      </Text>
                      <Text style={transferStyles.cost}>£{(t.element_out_cost / 10).toFixed(1)}m</Text>
                      <View style={[transferStyles.badge, { backgroundColor: Colors.primaryContainer + '33' }]}>
                        <Text style={[transferStyles.badgeText, { color: Colors.primary }]}>IN</Text>
                      </View>
                      <Text style={transferStyles.playerName} numberOfLines={1}>
                        {getPlayerName(t.element_in)}
                      </Text>
                      <Text style={transferStyles.cost}>£{(t.element_in_cost / 10).toFixed(1)}m</Text>
                    </View>
                  ))}
                </View>
              ))}
              {Object.keys(transfersByGw).length > 5 && (
                <TouchableOpacity
                  style={styles.showMoreBtn}
                  onPress={() => setShowAllTransfers((value) => !value)}
                >
                  <Text style={styles.showMoreText}>
                    {showAllTransfers ? 'Show Less Transfers ▲' : `Show All ${Object.keys(transfersByGw).length} GWs ▼`}
                  </Text>
                </TouchableOpacity>
              )}
            </>
          )}

          {/* ── GW HISTORY ────────────────────────────────────────── */}
          <SectionTitle title="GAMEWEEK HISTORY" />
          <View style={styles.historyHeader}>
            <Text style={styles.historyCol}>GW</Text>
            <Text style={styles.historyCol}>Pts</Text>
            <Text style={styles.historyCol}>Total</Text>
            <Text style={styles.historyCol}>Rank</Text>
            <Text style={styles.historyCol}>O.Rank</Text>
          </View>
        </>
      }
      data={gwList}
      keyExtractor={(item) => String(item.event)}
      renderItem={({ item }) => (
        <View style={styles.historyRow}>
          <TouchableOpacity
            style={styles.historyRowButton}
            activeOpacity={0.85}
            onPress={() => router.push(`/formation/${managerData.id}/${item.event}`)}
          >
            <Text style={styles.historyCell}>{item.event}</Text>
            <Text style={[styles.historyCell, { color: Colors.primary, fontWeight: '700' }]}>
              {item.points}
            </Text>
            <Text style={styles.historyCell}>{item.total_points}</Text>
            <Text style={styles.historyCell}>
              {item.rank != null ? formatRank(item.rank) : '—'}
            </Text>
            <View style={styles.historyLastCol}>
              <Text style={styles.historyOverallCell}>{formatRank(item.overall_rank)}</Text>
              {item.event_transfers_cost > 0 ? (
                <Text style={styles.historyMetaHit}>-{item.event_transfers_cost} hit</Text>
              ) : chips.find((chip) => chip.event === item.event) ? (
                <Text style={styles.historyMetaChip}>
                  {(() => {
                    const chip = chips.find((entry) => entry.event === item.event)!;
                    const index = chips
                      .filter((entry) => entry.name === chip.name && entry.event <= item.event)
                      .length - 1;
                    return chipLabel(chip.name, index);
                  })()}
                </Text>
              ) : item.event_transfers > 0 ? (
                <Text style={styles.historyMetaTransfer}>{item.event_transfers} xfer</Text>
              ) : null}
            </View>
          </TouchableOpacity>
        </View>
      )}
      ListFooterComponent={
        currentHistory.length > 10 ? (
          <TouchableOpacity
            style={styles.showMoreBtn}
            onPress={() => setShowAllGw((v) => !v)}
          >
            <Text style={styles.showMoreText}>
              {showAllGw ? 'Show Less ▲' : `Show All ${currentHistory.length} GWs ▼`}
            </Text>
          </TouchableOpacity>
        ) : null
      }
    />
  );
}

// ── Sub-components ─────────────────────────────────────────────────────────────

function SectionTitle({ title }: { title: string }) {
  return (
    <Text style={secStyles.title}>{title}</Text>
  );
}

function StatCard({
  label, value, sub, accent = false,
}: { label: string; value: string; sub?: string; accent?: boolean }) {
  return (
    <View style={[cardStyles.card, accent && cardStyles.cardAccent]}>
      <Text style={[cardStyles.value, accent && cardStyles.valueAccent]}>{value}</Text>
      {sub && <Text style={cardStyles.sub}>{sub}</Text>}
      <Text style={cardStyles.label}>{label}</Text>
    </View>
  );
}

const secStyles = StyleSheet.create({
  title: {
    fontSize: 10, fontWeight: '800',
    color: Colors.primary,
    letterSpacing: 3,
    paddingHorizontal: 16,
    paddingTop: 20,
    paddingBottom: 10,
  },
});

const cardStyles = StyleSheet.create({
  card: {
    flex: 1, minWidth: '45%',
    backgroundColor: Colors.surface,
    borderRadius: Radius.md,
    padding: 14,
    gap: 2,
    margin: 4,
  },
  cardAccent: {
    backgroundColor: Colors.primaryContainer,
  },
  value: {
    fontSize: 20, fontWeight: '800',
    color: Colors.onSurface,
  },
  valueAccent: {
    color: Colors.primary,
  },
  sub: {
    fontSize: 11,
    color: Colors.outline,
  },
  label: {
    fontSize: 10, fontWeight: '700',
    color: Colors.onSurfaceVariant,
    letterSpacing: 0.5,
    marginTop: 2,
  },
});

const chipStyles = StyleSheet.create({
  chipCard: {
    width: '48%',
    backgroundColor: Colors.surface,
    borderRadius: Radius.lg,
    paddingVertical: 18,
    paddingHorizontal: 14,
    alignItems: 'center',
    gap: 8,
  },
  chipCardLocked: {
    borderWidth: 1,
    borderColor: Colors.outlineVariant + '66',
    borderStyle: 'dashed',
    opacity: 0.72,
  },
  chipIcon: {
    width: 44,
    height: 44,
    borderRadius: 22,
    alignItems: 'center',
    justifyContent: 'center',
  },
  chipIconText: {
    fontSize: 14,
    fontWeight: '900',
  },
  chipCardLabel: {
    fontSize: 14,
    fontWeight: '800',
    color: Colors.onSurface,
    letterSpacing: 0.5,
  },
  chipCardLabelLocked: {
    color: Colors.outline,
  },
  chipCardSub: {
    fontSize: 11,
    fontWeight: '600',
    color: Colors.primary,
  },
  chipCardSubLocked: {
    color: Colors.outline,
  },
});

const transferStyles = StyleSheet.create({
  gwGroup: {
    backgroundColor: Colors.surface,
    borderRadius: Radius.md,
    marginHorizontal: 16,
    marginBottom: 8,
    padding: 12,
    gap: 8,
  },
  gwLabel: {
    fontSize: 10, fontWeight: '800',
    color: Colors.outline,
    letterSpacing: 2,
  },
  groupHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 2,
  },
  groupMeta: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
  },
  hitText: {
    fontSize: 10,
    fontWeight: '800',
    color: Colors.tertiary,
  },
  chipText: {
    fontSize: 10,
    fontWeight: '800',
    color: Colors.primary,
  },
  row: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 6,
  },
  badge: {
    paddingHorizontal: 5,
    paddingVertical: 2,
    borderRadius: 4,
  },
  badgeText: {
    fontSize: 9, fontWeight: '800',
    letterSpacing: 0.5,
  },
  playerName: {
    flex: 1,
    fontSize: 12, fontWeight: '600',
    color: Colors.onSurface,
  },
  cost: {
    fontSize: 11,
    color: Colors.outline,
    marginRight: 4,
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
  hero: {
    paddingHorizontal: 20,
    paddingBottom: 20,
    gap: 6,
  },
  heroRegion: {
    fontSize: 10, fontWeight: '700',
    color: Colors.outline,
    letterSpacing: 2,
  },
  heroName: {
    fontSize: 32, fontWeight: '900',
    color: Colors.onSurface,
    letterSpacing: -1,
    lineHeight: 36,
  },
  heroTeamRow: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  heroTeam: {
    fontSize: 13, fontWeight: '700',
    color: Colors.primary,
    letterSpacing: 1,
  },
  heroSep: {
    fontSize: 13,
    color: Colors.outline,
  },
  heroId: {
    fontSize: 13,
    color: Colors.outline,
  },
  rankChip: {
    flexDirection: 'row',
    alignItems: 'center',
    alignSelf: 'flex-start',
    backgroundColor: Colors.primaryContainer + '66',
    borderRadius: Radius.full,
    paddingHorizontal: 12,
    paddingVertical: 5,
    gap: 6,
    marginTop: 6,
  },
  rankChipDot: {
    width: 7, height: 7,
    borderRadius: 3.5,
    backgroundColor: Colors.primary,
  },
  rankChipText: {
    fontSize: 11, fontWeight: '700',
    color: Colors.primary,
    letterSpacing: 1,
  },
  openTeamBtn: {
    marginTop: 12,
    alignSelf: 'flex-start',
    backgroundColor: Colors.surface,
    borderRadius: Radius.full,
    paddingHorizontal: 14,
    paddingVertical: 10,
    gap: 2,
  },
  openTeamBtnText: {
    fontSize: 13,
    fontWeight: '800',
    color: Colors.primary,
  },
  openTeamBtnSub: {
    fontSize: 10,
    fontWeight: '700',
    color: Colors.outline,
    letterSpacing: 1,
  },
  statGrid: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    paddingHorizontal: 12,
    paddingTop: 8,
  },
  chipGrid: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    justifyContent: 'space-between',
    paddingHorizontal: 16,
    rowGap: 10,
  },
  historyHeader: {
    flexDirection: 'row',
    paddingHorizontal: 16,
    paddingVertical: 8,
    backgroundColor: Colors.surfaceHigh,
  },
  historyCol: {
    flex: 1,
    fontSize: 10, fontWeight: '700',
    color: Colors.outline,
    letterSpacing: 1,
    textAlign: 'center',
  },
  historyRow: {
    borderBottomWidth: 1,
    borderBottomColor: Colors.outlineVariant + '33',
  },
  historyRowButton: {
    flexDirection: 'row',
    paddingHorizontal: 16,
    paddingVertical: 10,
    alignItems: 'center',
  },
  historyCell: {
    flex: 1,
    fontSize: 12,
    color: Colors.onSurface,
    textAlign: 'center',
  },
  historyLastCol: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
  },
  historyOverallCell: {
    fontSize: 12,
    color: Colors.onSurface,
    textAlign: 'center',
  },
  historyMetaHit: {
    fontSize: 9,
    color: Colors.tertiary,
    marginTop: 2,
  },
  historyMetaChip: {
    fontSize: 9,
    color: Colors.primary,
    marginTop: 2,
    fontWeight: '700',
  },
  historyMetaTransfer: {
    fontSize: 9,
    color: Colors.outline,
    marginTop: 2,
  },
  showMoreBtn: {
    padding: 16,
    alignItems: 'center',
  },
  showMoreText: {
    fontSize: 12, fontWeight: '600',
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
