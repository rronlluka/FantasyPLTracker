package com.fpl.tracker.data.preferences

import android.content.Context
import android.content.SharedPreferences

class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences = 
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "fpl_tracker_prefs"
        private const val KEY_MANAGER_ID = "manager_id"
        private const val KEY_LEAGUE_ID = "league_id"
        private const val KEY_FAVORITE_LEAGUE_ID = "favorite_league_id"
        private const val KEY_FAVORITE_LEAGUE_NAME = "favorite_league_name"
    }

    fun saveManagerId(managerId: Long) {
        prefs.edit().putLong(KEY_MANAGER_ID, managerId).apply()
    }

    fun getManagerId(): Long? {
        val id = prefs.getLong(KEY_MANAGER_ID, -1L)
        return if (id == -1L) null else id
    }

    fun saveLeagueId(leagueId: Long) {
        prefs.edit().putLong(KEY_LEAGUE_ID, leagueId).apply()
    }

    fun getLeagueId(): Long? {
        val id = prefs.getLong(KEY_LEAGUE_ID, -1L)
        return if (id == -1L) null else id
    }
    
    fun saveFavoriteLeague(leagueId: Long, leagueName: String) {
        prefs.edit()
            .putLong(KEY_FAVORITE_LEAGUE_ID, leagueId)
            .putString(KEY_FAVORITE_LEAGUE_NAME, leagueName)
            .apply()
    }
    
    fun getFavoriteLeagueId(): Long? {
        val id = prefs.getLong(KEY_FAVORITE_LEAGUE_ID, -1L)
        return if (id == -1L) null else id
    }
    
    fun getFavoriteLeagueName(): String? {
        return prefs.getString(KEY_FAVORITE_LEAGUE_NAME, null)
    }
    
    fun removeFavoriteLeague() {
        prefs.edit()
            .remove(KEY_FAVORITE_LEAGUE_ID)
            .remove(KEY_FAVORITE_LEAGUE_NAME)
            .apply()
    }

    fun clearAll() {
        prefs.edit().clear().apply()
    }
}

