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
  getDbInfo: db.getDbInfo,
};
