const fpl = require('./fpl');
const storage = require('./storage');
const { TTL, withCache } = require('./cache');

function findCurrentEvent(events) {
  return events.find((event) => event.is_current)
    ?? events.find((event) => event.is_next)
    ?? [...events].reverse().find((event) => event.finished)
    ?? events[events.length - 1];
}

function getResolvedEvent(events, eventId) {
  return events.find((event) => event.id === eventId) ?? findCurrentEvent(events);
}

function valueToPriceLabel(value) {
  return `£${(value / 10).toFixed(1)}m`;
}

function signedPriceDelta(value) {
  const pounds = value / 10;
  const sign = pounds > 0 ? '+' : '';
  return `${sign}£${pounds.toFixed(1)}m`;
}

function buildPlayerMaps(bootstrap) {
  const teamsById = new Map(bootstrap.teams.map((team) => [team.id, team]));
  const positionsById = new Map(bootstrap.element_types.map((pos) => [pos.id, pos]));
  const playersById = new Map(bootstrap.elements.map((player) => [player.id, player]));
  return { teamsById, positionsById, playersById };
}

function makeLeaderboardRow(player, maps, primaryValue, primaryDisplay, secondaryDisplay, extra = {}) {
  const team = maps.teamsById.get(player.team);
  const position = maps.positionsById.get(player.element_type);
  return {
    playerId: player.id,
    name: player.web_name,
    teamId: player.team,
    teamShortName: team?.short_name ?? '—',
    position: position?.singular_name_short ?? 'UNK',
    primaryValue,
    primaryDisplay,
    secondaryDisplay,
    currentPrice: player.now_cost,
    ...extra,
  };
}

function section(title, statLabel, rows, extra = {}) {
  return {
    title,
    statLabel,
    rows,
    ...extra,
  };
}

function getSortedPlayers(players, selector) {
  return [...players].sort((a, b) => {
    const aVal = selector(a);
    const bVal = selector(b);
    if (bVal !== aVal) return bVal - aVal;
    return (b.total_points ?? 0) - (a.total_points ?? 0) || a.web_name.localeCompare(b.web_name);
  });
}

function getPriceChange(player) {
  return Number(player.cost_change_start ?? 0);
}

function getLiveTtl(event) {
  return event?.is_current ? TTL.LIVE_ACTIVE : TTL.LIVE_IDLE;
}

function getDefConThreshold(elementType) {
  return elementType === 2 ? 10 : 12;
}

function extractDefConActions(liveElement) {
  return (liveElement.explain ?? [])
    .flatMap((entry) => entry.stats ?? [])
    .filter((stat) => String(stat.identifier).toLowerCase().includes('def'))
    .reduce((sum, stat) => sum + Number(stat.value ?? 0), 0);
}

async function ensureDefConAwardsThroughEvent(bootstrap, maxEventId) {
  const playerMap = new Map(bootstrap.elements.map((player) => [player.id, player]));
  const relevantEvents = bootstrap.events.filter((event) => event.id <= maxEventId && (event.finished || event.is_current));

  for (const event of relevantEvents) {
    const shouldRefresh = event.is_current;
    const existingCount = storage.countDefConAwardsForEvent(event.id);
    if (!shouldRefresh && existingCount > 0) continue;

    const { data: live } = await withCache(
      `live:defcon:gw${event.id}`,
      getLiveTtl(event),
      () => fpl.getLiveGameweek(event.id),
    );

    const awards = [];
    for (const liveElement of live.elements ?? []) {
      const player = playerMap.get(liveElement.id);
      if (!player || player.element_type === 1) continue;

      const actions = extractDefConActions(liveElement);
      if (actions >= getDefConThreshold(player.element_type)) {
        awards.push({
          playerId: player.id,
          elementType: player.element_type,
          actions,
          points: 2,
        });
      }
    }

    storage.saveDefConAwards(event.id, awards);
  }
}

function buildDefConSection(bootstrap, maps, eventId, elementType, title) {
  const counts = storage.getDefConCountsThroughEvent(eventId, elementType);
  const rows = counts
    .map((row) => {
      const player = maps.playersById.get(row.player_id);
      if (!player) return null;
      return makeLeaderboardRow(
        player,
        maps,
        row.defcon_count,
        String(row.defcon_count),
        `${valueToPriceLabel(player.now_cost)} current`,
        { bestActions: row.best_actions ?? 0 },
      );
    })
    .filter(Boolean);

  return section(title, 'BONUS +2', rows);
}

async function getStatsOverview(eventId = null) {
  const { data: bootstrap } = await withCache('bootstrap', TTL.BOOTSTRAP, fpl.getBootstrap);
  const resolvedEvent = getResolvedEvent(bootstrap.events, eventId);
  const maps = buildPlayerMaps(bootstrap);
  const players = bootstrap.elements ?? [];

  await ensureDefConAwardsThroughEvent(bootstrap, resolvedEvent.id);

  const { data: live } = await withCache(
    `live:gw${resolvedEvent.id}`,
    getLiveTtl(resolvedEvent),
    () => fpl.getLiveGameweek(resolvedEvent.id),
  );
  const liveById = new Map((live.elements ?? []).map((entry) => [entry.id, entry]));

  const mostPoints = getSortedPlayers(players, (player) => Number(player.total_points ?? 0))
    .map((player) => makeLeaderboardRow(
      player,
      maps,
      Number(player.total_points ?? 0),
      String(player.total_points ?? 0),
      `${player.points_per_game ?? '0'} ppg`,
    ));

  const gwPoints = players
    .map((player) => {
      const liveStats = liveById.get(player.id)?.stats;
      const gwPointsValue = Number(liveStats?.total_points ?? 0);
      return makeLeaderboardRow(
        player,
        maps,
        gwPointsValue,
        String(gwPointsValue),
        `${player.total_points ?? 0} season`,
        {
          liveDelta: resolvedEvent.is_current && !resolvedEvent.finished
            ? Math.max(gwPointsValue - Number(player.event_points ?? 0), 0)
            : 0,
        },
      );
    })
    .sort((a, b) => b.primaryValue - a.primaryValue || a.name.localeCompare(b.name));

  const goals = getSortedPlayers(players, (player) => Number(player.goals_scored ?? 0))
    .map((player) => makeLeaderboardRow(
      player,
      maps,
      Number(player.goals_scored ?? 0),
      String(player.goals_scored ?? 0),
      `${player.total_points ?? 0} pts`,
    ));

  const assists = getSortedPlayers(players, (player) => Number(player.assists ?? 0))
    .map((player) => makeLeaderboardRow(
      player,
      maps,
      Number(player.assists ?? 0),
      String(player.assists ?? 0),
      `${player.total_points ?? 0} pts`,
    ));

  const form = getSortedPlayers(players, (player) => Number.parseFloat(player.form ?? '0') || 0)
    .map((player) => makeLeaderboardRow(
      player,
      maps,
      Number.parseFloat(player.form ?? '0') || 0,
      player.form ?? '0.0',
      `${player.total_points ?? 0} pts`,
    ));

  const ownership = getSortedPlayers(players, (player) => Number.parseFloat(player.selected_by_percent ?? '0') || 0)
    .map((player) => makeLeaderboardRow(
      player,
      maps,
      Number.parseFloat(player.selected_by_percent ?? '0') || 0,
      `${(Number.parseFloat(player.selected_by_percent ?? '0') || 0).toFixed(1)}%`,
      `${player.total_points ?? 0} pts`,
    ));

  const marketRows = players.map((player) => {
    const delta = getPriceChange(player);
    return makeLeaderboardRow(
      player,
      maps,
      delta,
      signedPriceDelta(delta),
      valueToPriceLabel(player.now_cost),
      { priceDelta: delta },
    );
  });

  const marketRisers = [...marketRows]
    .filter((row) => row.priceDelta > 0)
    .sort((a, b) => b.priceDelta - a.priceDelta || a.name.localeCompare(b.name));
  const marketFallers = [...marketRows]
    .filter((row) => row.priceDelta < 0)
    .sort((a, b) => a.priceDelta - b.priceDelta || a.name.localeCompare(b.name));

  return {
    meta: {
      event: resolvedEvent.id,
      eventName: resolvedEvent.name,
      isLive: Boolean(resolvedEvent.is_current && !resolvedEvent.finished),
      currentEvent: findCurrentEvent(bootstrap.events)?.id ?? resolvedEvent.id,
    },
    sections: {
      mostPoints: section('MOST POINTS', 'PTS', mostPoints),
      gameweekPoints: section('GW POINTS', 'GW PTS', gwPoints, { isLive: Boolean(resolvedEvent.is_current && !resolvedEvent.finished) }),
      goals: section('GOALS', 'GOALS', goals),
      assists: section('ASSISTS', 'ASSISTS', assists),
      marketRisers: section('VALUE RISERS', 'CHANGE', marketRisers),
      marketFallers: section('VALUE FALLERS', 'CHANGE', marketFallers),
      form: section('FORM', 'FORM', form),
      ownership: section('OWNERSHIP', 'OWNED', ownership),
    },
    defCon: {
      DEF: buildDefConSection(bootstrap, maps, resolvedEvent.id, 2, 'DEF CON DEFENDERS'),
      MID: buildDefConSection(bootstrap, maps, resolvedEvent.id, 3, 'DEF CON MIDFIELDERS'),
      FWD: buildDefConSection(bootstrap, maps, resolvedEvent.id, 4, 'DEF CON ATTACKERS'),
    },
  };
}

module.exports = {
  getStatsOverview,
};
