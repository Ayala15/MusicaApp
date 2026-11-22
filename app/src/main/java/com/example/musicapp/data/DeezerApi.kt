package com.example.musicapp.data

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/** API pública de Deezer: no necesita API key. https://developers.deezer.com/api */
interface DeezerApi {
    @GET("chart/0/tracks")
    suspend fun chartTracks(@Query("limit") limit: Int = 50): TrackList

    @GET("chart/0/artists")
    suspend fun chartArtists(@Query("limit") limit: Int = 20): ArtistList

    @GET("search")
    suspend fun search(@Query("q") query: String, @Query("limit") limit: Int = 40): TrackList

    @GET("search/artist")
    suspend fun searchArtist(@Query("q") query: String, @Query("limit") limit: Int = 1): ArtistList

    @GET("track/isrc:{isrc}")
    suspend fun trackByIsrc(@Path("isrc") isrc: String): Track

    @GET("artist/{id}/top")
    suspend fun artistTop(@Path("id") id: Long, @Query("limit") limit: Int = 30): TrackList
}

object Api {
    val service: DeezerApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.deezer.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(DeezerApi::class.java)
    }
}
