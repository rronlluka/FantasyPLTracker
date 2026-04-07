/**
 * FootballPitch (FootballPitch.kt port)
 * Renders a formation view with player cards arranged by position.
 */
import React from 'react';
import {
  View, Text, TouchableOpacity, StyleSheet,
} from 'react-native';
import { Colors, Radius } from '@/constants/theme';
import type { Fixture, LiveElement, Team } from '@/types/fpl';

export interface PlayerWithDetails {
  pick: {
    element: number;
    position: number;
    multiplier: number;
    is_captain: boolean;
    is_vice_captain: boolean;
  };
  player: {
    id: number;
    web_name: string;
    element_type: number;
    team: number;
    now_cost: number;
    selected_by_percent: string;
    event_points: number;
    total_points: number;
    photo: string;
  };
  liveDeltaPoints: number;
  currentPoints: number;
  displayPoints: number;
  teamShortName: string;
  teamInfo?: Team;
  fixture?: Fixture;
  liveStats?: LiveElement;
}

function getTeamColor(teamId: number): string {
  const teamColors: Record<number, string> = {
    1: '#EF0107',
    2: '#95BFE5',
    3: '#E62333',
    4: '#E30613',
    5: '#0057B8',
    6: '#034694',
    7: '#1B458F',
    8: '#003399',
    9: '#000000',
    10: '#0057B8',
    11: '#003090',
    12: '#C8102E',
    13: '#6CABDD',
    14: '#DA291C',
    15: '#241F20',
    16: '#E53233',
    17: '#D71920',
    18: '#132257',
    19: '#7A263A',
    20: '#FDB913',
  };
  return teamColors[teamId] ?? Colors.surfaceHigh;
}

function positionLabel(elementType: number): string {
  switch (elementType) {
    case 1: return 'GK';
    case 2: return 'DEF';
    case 3: return 'MID';
    case 4: return 'FWD';
    default: return '?';
  }
}

function PlayerIndicators({ player }: { player: PlayerWithDetails }) {
  const goals = player.liveStats?.stats.goals_scored ?? 0;
  const assists = player.liveStats?.stats.assists ?? 0;

  const topIndicators: { key: string; label: string; active?: boolean; invert?: boolean }[] = [];
  const bottomIndicators: { key: string; label: string }[] = [];

  if (goals > 0) {
    bottomIndicators.push({ key: 'goals', label: goals > 1 ? `⚽${goals}` : '⚽' });
  }
  if (assists > 0) {
    bottomIndicators.push({ key: 'assists', label: assists > 1 ? `A${assists}` : 'A' });
  }
  if (player.pick.is_captain) {
    topIndicators.push({ key: 'captain', label: 'C', active: true, invert: true });
  } else if (player.pick.is_vice_captain) {
    topIndicators.push({ key: 'vice', label: 'V' });
  }

  if (topIndicators.length === 0 && bottomIndicators.length === 0) return null;

  return (
    <>
      {topIndicators.length > 0 && (
        <View style={playerStyles.indicatorStackTop}>
          {topIndicators.map((indicator) => (
            <View
              key={indicator.key}
              style={[
                playerStyles.indicatorBadge,
                indicator.active && playerStyles.indicatorBadgeActive,
              ]}
            >
              <Text
                style={[
                  playerStyles.indicatorText,
                  indicator.active && playerStyles.indicatorTextActive,
                  indicator.invert && playerStyles.indicatorTextInvert,
                ]}
              >
                {indicator.label}
              </Text>
            </View>
          ))}
        </View>
      )}

      {bottomIndicators.length > 0 && (
        <View style={playerStyles.indicatorStackBottom}>
          {bottomIndicators.map((indicator) => (
            <View key={indicator.key} style={playerStyles.indicatorBadge}>
              <Text style={playerStyles.indicatorText}>{indicator.label}</Text>
            </View>
          ))}
        </View>
      )}
    </>
  );
}

// ── Starter Card ──────────────────────────────────────────────────────────────
function StarterPlayerCard({
  player,
  onPress,
}: {
  player: PlayerWithDetails;
  onPress?: (p: PlayerWithDetails) => void;
}) {
  const teamColor = getTeamColor(player.player.team);
  const displayPoints = player.displayPoints;
  const isCaptain = player.pick.is_captain;

  return (
    <TouchableOpacity
      style={playerStyles.card}
      onPress={() => onPress?.(player)}
      activeOpacity={0.8}
    >
      {/* Team colour top stripe */}
      <View style={[playerStyles.stripe, { backgroundColor: teamColor }]} />

      <PlayerIndicators player={player} />

      {/* Web name */}
      <Text style={playerStyles.name} numberOfLines={1}>
        {player.player.web_name}
      </Text>

      <Text style={playerStyles.teamName} numberOfLines={1}>
        {player.teamShortName}
      </Text>

      {/* Points pill */}
      <View style={[playerStyles.pointsPill, isCaptain && playerStyles.pointsPillCaptain]}>
        <Text style={[playerStyles.pointsText, isCaptain && playerStyles.pointsTextCaptain]}>
          {displayPoints}
        </Text>
      </View>

      {/* Position label */}
      <Text style={playerStyles.position}>{positionLabel(player.player.element_type)}</Text>
    </TouchableOpacity>
  );
}

function BenchPlayerCard({
  player,
  onPress,
}: {
  player: PlayerWithDetails;
  onPress?: (p: PlayerWithDetails) => void;
}) {
  const teamColor = getTeamColor(player.player.team);

  return (
    <TouchableOpacity
      style={[playerStyles.card, playerStyles.benchCard]}
      onPress={() => onPress?.(player)}
      activeOpacity={0.8}
    >
      <View style={[playerStyles.stripe, { backgroundColor: teamColor }]} />
      <PlayerIndicators player={player} />

      <Text style={playerStyles.name} numberOfLines={1}>
        {player.player.web_name}
      </Text>

      <Text style={playerStyles.teamName} numberOfLines={1}>
        {player.teamShortName}
      </Text>

      <View style={playerStyles.pointsPill}>
        <Text style={playerStyles.pointsText}>
          {player.displayPoints}
        </Text>
      </View>

      <Text style={playerStyles.position}>{positionLabel(player.player.element_type)}</Text>
    </TouchableOpacity>
  );
}

// ── Player Row (row of same position) ─────────────────────────────────────────
function PlayerRow({
  players,
  onPlayerClick,
  spread = false,
}: {
  players: PlayerWithDetails[];
  onPlayerClick?: (p: PlayerWithDetails) => void;
  spread?: boolean;
}) {
  return (
    <View style={[rowStyles.row, spread && rowStyles.rowSpread]}>
      {players.map((p) => (
        <StarterPlayerCard
          key={p.pick.element}
          player={p}
          onPress={onPlayerClick}
        />
      ))}
    </View>
  );
}

// ── Football Pitch ────────────────────────────────────────────────────────────
export function FootballPitch({
  startingXI,
  bench = [],
  onPlayerClick,
  tall = false,
}: {
  startingXI: PlayerWithDetails[];
  bench?: PlayerWithDetails[];
  onPlayerClick?: (p: PlayerWithDetails) => void;
  tall?: boolean;
}) {
  const gk = startingXI.filter((p) => p.player.element_type === 1);
  const def = startingXI.filter((p) => p.player.element_type === 2);
  const mid = startingXI.filter((p) => p.player.element_type === 3);
  const fwd = startingXI.filter((p) => p.player.element_type === 4);

  return (
    <View>
      {/* ── PITCH ───────────────────────────────────────────────── */}
      <View style={[pitchStyles.pitch, tall && pitchStyles.pitchTall]}>
        {/* Pitch markings overlay */}
        <View style={pitchStyles.centreCircle} />
        <View style={pitchStyles.centreLine} />
        <View style={pitchStyles.penaltyBoxTop} />
        <View style={pitchStyles.penaltyBoxBottom} />

        {/* Players */}
        <View style={pitchStyles.playerArea}>
          {gk.length > 0 && <PlayerRow players={gk} onPlayerClick={onPlayerClick} spread />}
          {def.length > 0 && <PlayerRow players={def} onPlayerClick={onPlayerClick} spread />}
          {mid.length > 0 && <PlayerRow players={mid} onPlayerClick={onPlayerClick} spread />}
          {fwd.length > 0 && <PlayerRow players={fwd} onPlayerClick={onPlayerClick} spread />}
        </View>
      </View>

      {/* ── BENCH ───────────────────────────────────────────────── */}
      {bench.length > 0 && (
        <View style={pitchStyles.benchSection}>
          <Text style={pitchStyles.benchLabel}>BENCH</Text>
          <View style={rowStyles.row}>
            {bench.map((p) => (
              <BenchPlayerCard
                key={p.pick.element}
                player={p}
                onPress={onPlayerClick}
              />
            ))}
          </View>
        </View>
      )}
    </View>
  );
}

const playerStyles = StyleSheet.create({
  card: {
    flex: 1,
    maxWidth: 76,
    minWidth: 60,
    backgroundColor: Colors.surface,
    borderRadius: Radius.sm,
    alignItems: 'center',
    paddingBottom: 8,
    paddingTop: 0,
    marginHorizontal: 3,
    borderWidth: 1,
    borderColor: Colors.outlineVariant + '66',
  },
  benchCard: {
    opacity: 0.9,
  },
  stripe: {
    width: '100%',
    height: 4,
    marginBottom: 4,
  },
  captainBadge: {
    display: 'none',
  },
  indicatorStackTop: {
    position: 'absolute',
    top: -8,
    right: -8,
    gap: 4,
    alignItems: 'flex-end',
    zIndex: 3,
  },
  indicatorStackBottom: {
    position: 'absolute',
    bottom: -10,
    right: -10,
    flexDirection: 'row',
    gap: 4,
    alignItems: 'center',
    zIndex: 3,
  },
  indicatorBadge: {
    minWidth: 18,
    height: 18,
    paddingHorizontal: 4,
    borderRadius: 9,
    backgroundColor: Colors.surface,
    justifyContent: 'center',
    alignItems: 'center',
    borderWidth: 1,
    borderColor: Colors.outlineVariant,
  },
  indicatorBadgeActive: {
    backgroundColor: Colors.primary,
    borderColor: Colors.primary,
  },
  indicatorText: {
    fontSize: 8,
    fontWeight: '900',
    color: Colors.onSurface,
  },
  indicatorTextActive: {
    color: Colors.onPrimary,
  },
  indicatorTextInvert: {
    color: Colors.onPrimary,
  },
  name: {
    fontSize: 10, fontWeight: '700',
    color: Colors.onSurface,
    textAlign: 'center',
    paddingHorizontal: 4,
    marginBottom: 2,
  },
  teamName: {
    fontSize: 8,
    fontWeight: '700',
    color: Colors.outline,
    textAlign: 'center',
    marginBottom: 4,
  },
  pointsPill: {
    backgroundColor: Colors.surfaceHigh,
    borderRadius: Radius.full,
    paddingHorizontal: 8,
    paddingVertical: 2,
    marginBottom: 2,
  },
  pointsPillCaptain: {
    backgroundColor: Colors.primaryContainer,
  },
  pointsText: {
    fontSize: 12, fontWeight: '800',
    color: Colors.onSurface,
  },
  pointsTextCaptain: {
    color: Colors.primary,
  },
  position: {
    fontSize: 9, fontWeight: '700',
    color: Colors.outline,
    letterSpacing: 0.5,
  },
});

const rowStyles = StyleSheet.create({
  row: {
    flexDirection: 'row',
    justifyContent: 'center',
    gap: 4,
  },
  rowSpread: {
    justifyContent: 'space-evenly',
    paddingHorizontal: 4,
  },
});

const pitchStyles = StyleSheet.create({
  pitch: {
    backgroundColor: Colors.primaryContainer,
    borderRadius: Radius.xl,
    overflow: 'hidden',
    borderWidth: 2,
    borderColor: Colors.surfaceHighest,
    position: 'relative',
    minHeight: 420,
  },
  pitchTall: {
    minHeight: 480,
  },
  centreCircle: {
    position: 'absolute',
    width: 100, height: 100,
    borderRadius: 50,
    borderWidth: 1.5,
    borderColor: 'rgba(161,212,148,0.25)',
    top: '50%',
    left: '50%',
    transform: [{ translateX: -50 }, { translateY: -50 }],
  },
  centreLine: {
    position: 'absolute',
    top: '50%',
    left: 0, right: 0,
    height: 1.5,
    backgroundColor: 'rgba(161,212,148,0.2)',
  },
  penaltyBoxTop: {
    position: 'absolute',
    top: 0, left: '25%', right: '25%',
    height: 60,
    borderBottomWidth: 1.5,
    borderLeftWidth: 1.5,
    borderRightWidth: 1.5,
    borderColor: 'rgba(161,212,148,0.2)',
  },
  penaltyBoxBottom: {
    position: 'absolute',
    bottom: 0, left: '25%', right: '25%',
    height: 60,
    borderTopWidth: 1.5,
    borderLeftWidth: 1.5,
    borderRightWidth: 1.5,
    borderColor: 'rgba(161,212,148,0.2)',
  },
  playerArea: {
    flex: 1,
    justifyContent: 'space-between',
    paddingVertical: 28,
    paddingHorizontal: 10,
  },
  benchSection: {
    marginTop: 12,
    backgroundColor: Colors.surface,
    borderRadius: Radius.lg,
    padding: 12,
  },
  benchLabel: {
    fontSize: 9, fontWeight: '800',
    color: Colors.outline,
    letterSpacing: 3,
    textAlign: 'center',
    marginBottom: 8,
  },
});
