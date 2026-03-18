const routeStats = new Map();

const counters = {
  requestsTotal: 0,
  requestErrors: 0,
  cacheHits: 0,
  cacheMisses: 0,
  snapshotRefreshes: 0,
  snapshotRefreshFailures: 0,
  snapshotLockWaits: 0,
  fplCalls: 0,
  fplFailures: 0,
};

function increment(name, amount = 1) {
  counters[name] = (counters[name] ?? 0) + amount;
}

function recordRequest(route, status, durationMs) {
  increment('requestsTotal');
  if (status >= 400) increment('requestErrors');
  const existing = routeStats.get(route) ?? {
    count: 0,
    errorCount: 0,
    totalDurationMs: 0,
    lastDurationMs: 0,
    lastStatus: 0,
  };
  existing.count += 1;
  if (status >= 400) existing.errorCount += 1;
  existing.totalDurationMs += durationMs;
  existing.lastDurationMs = durationMs;
  existing.lastStatus = status;
  routeStats.set(route, existing);
}

function recordCacheHit(fromCache) {
  increment(fromCache ? 'cacheHits' : 'cacheMisses');
}

function recordSnapshotRefresh(success) {
  increment(success ? 'snapshotRefreshes' : 'snapshotRefreshFailures');
}

function recordSnapshotLockWait() {
  increment('snapshotLockWaits');
}

function recordFplCall(success) {
  increment('fplCalls');
  if (!success) increment('fplFailures');
}

function getMetrics() {
  const routes = {};
  for (const [route, stats] of routeStats.entries()) {
    routes[route] = {
      count: stats.count,
      errorCount: stats.errorCount,
      avgDurationMs: stats.count > 0 ? Math.round(stats.totalDurationMs / stats.count) : 0,
      lastDurationMs: stats.lastDurationMs,
      lastStatus: stats.lastStatus,
    };
  }
  return {
    ...counters,
    routes,
    uptimeSeconds: Math.round(process.uptime()),
    memoryMB: Math.round(process.memoryUsage().heapUsed / 1024 / 1024),
  };
}

module.exports = {
  recordRequest,
  recordCacheHit,
  recordSnapshotRefresh,
  recordSnapshotLockWait,
  recordFplCall,
  getMetrics,
};
