package com.fpl.tracker.data.api

import com.fpl.tracker.data.models.*
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface FPLApiService {
    
    @GET("bootstrap-static/")
    suspend fun getBootstrapData(): Response<BootstrapData>
    
    @GET("fixtures/")
    suspend fun getAllFixtures(): Response<List<Fixture>>
    
    @GET("fixtures/")
    suspend fun getFixturesByEvent(@Query("event") eventId: Int): Response<List<Fixture>>
    
    @GET("entry/{manager_id}/")
    suspend fun getManagerData(@Path("manager_id") managerId: Long): Response<ManagerData>
    
    @GET("entry/{manager_id}/history/")
    suspend fun getManagerHistory(@Path("manager_id") managerId: Long): Response<ManagerHistory>
    
    @GET("entry/{manager_id}/event/{event_id}/picks/")
    suspend fun getManagerPicks(
        @Path("manager_id") managerId: Long,
        @Path("event_id") eventId: Int
    ): Response<ManagerPicks>
    
    @GET("event/{event_id}/live/")
    suspend fun getLiveGameweek(@Path("event_id") eventId: Int): Response<LiveGameweek>
    
    @GET("leagues-classic/{league_id}/standings/")
    suspend fun getLeagueStandings(
        @Path("league_id") leagueId: Long,
        @Query("page_standings") page: Int = 1,
        @Query("event") eventId: Int? = null  // Null = current GW; non-null = historical standings
    ): Response<LeagueStandings>
    
    @GET("element-summary/{element_id}/")
    suspend fun getPlayerDetail(@Path("element_id") elementId: Int): Response<PlayerDetailResponse>

    @GET("entry/{manager_id}/transfers/")
    suspend fun getManagerTransfers(
        @Path("manager_id") managerId: Long
    ): Response<List<ManagerTransfer>>
}

