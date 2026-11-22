package com.example.musicapp.data

import com.google.gson.annotations.SerializedName

data class Artist(
    val id: Long,
    val name: String,
    @SerializedName("picture_medium") val pictureMedium: String? = null,
    @SerializedName("picture_big") val pictureBig: String? = null
)

data class Album(
    val id: Long = 0,
    val title: String? = null,
    @SerializedName("cover_medium") val coverMedium: String? = null,
    @SerializedName("cover_big") val coverBig: String? = null
)

data class Track(
    val id: Long,
    val title: String,
    val duration: Int = 0,
    val preview: String? = null,
    val artist: Artist,
    val album: Album? = null
)

data class TrackList(val data: List<Track> = emptyList())
data class ArtistList(val data: List<Artist> = emptyList())
