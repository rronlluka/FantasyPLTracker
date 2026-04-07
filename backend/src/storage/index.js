const db = require('../db');

module.exports = {
  db,
  cacheGet: db.cacheGet,
  cacheSet: db.cacheSet,
  deleteCacheKey: db.deleteCacheKey,
  deleteAllCache: db.deleteAllCache,
  listActiveCache: db.listActiveCache,
  cachePrune: db.cachePrune,
  getLeaguePicks: db.getLeaguePicks,
  countLeaguePicks: db.countLeaguePicks,
  getLeagueSnapshot: db.getLeagueSnapshot,
  saveLeagueSnapshot: db.saveLeagueSnapshot,
  saveLeaguePicks: db.saveLeaguePicks,
  getPlayerStats: db.getPlayerStats,
  savePlayerStats: db.savePlayerStats,
  countDefConAwardsForEvent: db.countDefConAwardsForEvent,
  getDefConAwardsForEvent: db.getDefConAwardsForEvent,
  saveDefConAwards: db.saveDefConAwards,
  getDefConCountsThroughEvent: db.getDefConCountsThroughEvent,
  getDbInfo: db.getDbInfo,
};
