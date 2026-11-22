package com.example.musicapp.data

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

data class ReccoArtist(val id: String? = null, val name: String? = null)

data class ReccoTrack(
    val id: String? = null,
    val trackTitle: String? = null,
    val artists: List<ReccoArtist>? = null,
    val durationMs: Int? = null,
    val isrc: String? = null,
    val href: String? = null,
    val popularity: Int? = null
)

data class ReccoTrackPage(val content: List<ReccoTrack>? = null)
data class ReccoArtistPage(val content: List<ReccoArtist>? = null)

/**
 * API de ReccoBeats (gratis, sin API key): https://api.reccobeats.com/v1
 * /track/recommendation exige `seeds` (IDs de canciones) y acepta `size`.
 * ReccoBeats no entrega audio ni portadas, por eso cada recomendación
 * se enlaza con Deezer (por ISRC) para obtener portada y vista previa.
 */
interface ReccoApi {
    @GET("artist/search")
    suspend fun searchArtist(
        @Query("searchText") name: String,
        @Query("size") size: Int = 1
    ): ReccoArtistPage

    @GET("artist/{id}/track")
    suspend fun artistTracks(
        @Path("id") id: String,
        @Query("size") size: Int = 2
    ): ReccoTrackPage

    @GET("track/recommendation")
    suspend fun recommend(
        @Query("seeds") seeds: String,
        @Query("size") size: Int = 20
    ): ReccoTrackPage

    companion object {
        val service: ReccoApi by lazy {
            Retrofit.Builder()
                .baseUrl("https://api.reccobeats.com/v1/")
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(ReccoApi::class.java)
        }
    }
}
