package com.fpl.tracker.data.api

import com.fpl.tracker.data.models.*
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Retrofit interface for YOUR backend server.
 *
 * The backend proxies all FPL API calls (with caching) and also exposes the
 * new /api/league/{id}/gw/{gw}/player/{pid}/stats endpoint which replaces
 * the N individual picks calls with a single pre-computed response.
 *
 * Change BACKEND_URL in BackendRetrofitInstance to your server's address.
 */
interface BackendApiService {

    // ── Proxied FPL endpoints (identical paths to FPL API) ───────────────────
    // The backend caches these so the app doesn't hammer FPL directly.

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
        @Query("event") eventId: Int? = null
    ): Response<LeagueStandings>

    @GET("element-summary/{element_id}/")
    suspend fun getPlayerDetail(@Path("element_id") elementId: Int): Response<PlayerDetailResponse>

    @GET("entry/{manager_id}/transfers/")
    suspend fun getManagerTransfers(
        @Path("manager_id") managerId: Long
    ): Response<List<ManagerTransfer>>

    // ── New backend-only endpoint ─────────────────────────────────────────────
    //
    // Returns pre-computed Starts/Bench/Captain stats for a player in a league.
    // The backend fetches all managers' picks ONCE per GW per league and caches
    // them. Every call here is served instantly from the DB — no extra FPL calls.
    //
    @GET("league/{league_id}/gw/{gameweek}/player/{player_id}/stats")
    suspend fun getLeaguePlayerStats(
        @Path("league_id") leagueId: Long,
        @Path("gameweek") gameweek: Int,
        @Path("player_id") playerId: Int
    ): Response<BackendLeaguePlayerStats>
}
