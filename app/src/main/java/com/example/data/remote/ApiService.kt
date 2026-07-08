package com.example.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {

    @POST("api/login")
    suspend fun login(@Body request: LoginRequest): LoginResponse

    @GET("api/profile")
    suspend fun obtenerPerfil(@Header("Authorization") token: String): PerfilResponse

    @GET("api/groups")
    suspend fun obtenerMisGrupos(@Header("Authorization") token: String): List<GrupoResponse>

    @GET("api/groups/{id}")
    suspend fun obtenerDetalleGrupo(
        @Header("Authorization") token: String,
        @Path("id") grupoId: Int
    ): GrupoResponse

    @GET("api/groups/{id}/leaderboard")
    suspend fun obtenerClasificacionGrupo(
        @Header("Authorization") token: String,
        @Path("id") grupoId: Int
    ): List<UsuarioClasificacionResponse>

    @GET("api/matches")
    suspend fun obtenerProximosPartidos(
        @Header("Authorization") token: String,
        @Query("next") next: Boolean = true
    ): List<PartidoResponse>

    @POST("api/groups")
    suspend fun crearGrupo(
        @Header("Authorization") token: String,
        @Body request: CrearGrupoRequest
    ): CrearGrupoResponse

    @POST("api/groups/join")
    suspend fun unirseAGrupo(
        @Header("Authorization") token: String,
        @Body request: UnirseGrupoRequest
    ): UnirseGrupoResponse

    @GET("api/matches")
    suspend fun obtenerTodosLosPartidos(
        @Header("Authorization") token: String
    ): List<PartidoResponse>

    @POST("api/predictions")
    suspend fun registrarPrediccion(
        @Header("Authorization") token: String,
        @Body request: PrediccionRequest
    ): Response<PrediccionResponse>

    @GET("api/predictions/me")
    suspend fun obtenerMisPredicciones(
        @Header("Authorization") token: String
    ): List<PrediccionResponse>

    @GET("api/matches/updates")
    suspend fun obtenerActualizacionesPartidos(
        @Header("Authorization") token: String
    ): List<PartidoResponse>

    @GET("api/stadiums")
    suspend fun obtenerEstadios(@Header("Authorization") token: String): List<EstadioResponse>
}

// --- DATA CLASSES ---
data class LoginRequest(val email: String, val password: String)
data class LoginResponse(val token: String, val name: String, val email: String)
data class PerfilResponse(val name: String, val email: String, val total_score: Int, val groups_count: Int, val predictions_count: Int)
data class GrupoResponse(val id: Int, val name: String, val participants_count: Int, val user_score: Int, val invite_code: String)

data class UsuarioClasificacionResponse(
    val user_id: Int,
    val name: String,
    val score: Int,
    val rank: Int? = null
)

data class PartidoResponse(
    val id: Int,
    val home_team: String,
    val away_team: String,
    val match_date: String,
    val phase: String,
    val status: String,
    val home_score: Int? = null,
    val away_score: Int? = null,
    val stadium_id: Int? = null
)

data class CrearGrupoRequest(val name: String)
data class UnirseGrupoRequest(val invite_code: String)
data class CrearGrupoResponse(val id: Int, val name: String, val invite_code: String, val created_at: String)
data class UnirseGrupoResponse(val message: String, val group: GrupoResponse)
data class PrediccionRequest(val match_id: Int, val home_score: Int, val away_score: Int)
data class PrediccionResponse(val id: Int? = null, val match_id: Int, val home_score: Int, val away_score: Int, val status: String? = null, val points_earned: Int? = null)
data class EstadioResponse(val id: Int, val name: String, val city: String, val country: String, val capacity: Int, val latitude: Double, val longitude: Double)