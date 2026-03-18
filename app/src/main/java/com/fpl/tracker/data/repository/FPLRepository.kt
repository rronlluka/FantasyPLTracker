package com.fpl.tracker.data.repository

import com.fpl.tracker.data.api.FPLApiService
import com.fpl.tracker.data.models.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class FPLRepository(private val api: FPLApiService) {

    suspend fun getBootstrapData(): Result<BootstrapData> = withContext(Dispatchers.IO) {
        try {
            val response = api.getBootstrapData()
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to fetch bootstrap data: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getManagerData(managerId: Long): Result<ManagerData> = withContext(Dispatchers.IO) {
        try {
            val response = api.getManagerData(managerId)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to fetch manager data: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getManagerHistory(managerId: Long): Result<ManagerHistory> = withContext(Dispatchers.IO) {
        try {
            val response = api.getManagerHistory(managerId)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to fetch manager history: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getManagerPicks(managerId: Long, eventId: Int): Result<ManagerPicks> = withContext(Dispatchers.IO) {
        try {
            val response = api.getManagerPicks(managerId, eventId)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to fetch manager picks: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getLiveGameweek(eventId: Int): Result<LiveGameweek> = withContext(Dispatchers.IO) {
        try {
            val response = api.getLiveGameweek(eventId)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to fetch live gameweek: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getLeagueStandings(
        leagueId: Long,
        page: Int = 1,
        eventId: Int? = null   // null = live/current, non-null = historical GW
    ): Result<LeagueStandings> = withContext(Dispatchers.IO) {
        try {
            val response = api.getLeagueStandings(leagueId, page, eventId)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to fetch league standings: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getFixturesByEvent(eventId: Int): Result<List<Fixture>> = withContext(Dispatchers.IO) {
        try {
            val response = api.getFixturesByEvent(eventId)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to fetch fixtures: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getPlayerDetail(elementId: Int): Result<PlayerDetailResponse> = withContext(Dispatchers.IO) {
        try {
            val response = api.getPlayerDetail(elementId)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to fetch player detail: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getManagerTransfers(managerId: Long): Result<List<ManagerTransfer>> = withContext(Dispatchers.IO) {
        try {
            val response = api.getManagerTransfers(managerId)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to fetch manager transfers: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

