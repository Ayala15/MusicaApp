package com.example.musicapp.data

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Canciones guardadas por usuario, también solo en el dispositivo. */
object FavoritesRepository {
    private var prefs: SharedPreferences? = null
    private val gson = Gson()
    private var userKey: String? = null

    private val _items = MutableStateFlow<List<Track>>(emptyList())
    val items: StateFlow<List<Track>> = _items

    fun init(context: Context) {
        prefs = context.applicationContext.getSharedPreferences("favorites_prefs", Context.MODE_PRIVATE)
    }

    fun load(userId: String?) {
        userKey = userId
        _items.value = if (userId == null) emptyList() else read(userId)
    }

    fun toggle(track: Track) {
        val u = userKey ?: return
        val current = _items.value
        val updated = if (current.any { it.id == track.id }) {
            current.filter { it.id != track.id }
        } else {
            listOf(track) + current
        }
        _items.value = updated
        prefs?.edit()?.putString("fav_$u", gson.toJson(updated))?.apply()
    }

    private fun read(u: String): List<Track> {
        return try {
            val json = prefs?.getString("fav_$u", null) ?: return emptyList()
            gson.fromJson<List<Track>>(json, object : TypeToken<List<Track>>() {}.type) ?: emptyList()
        } catch (ex: Exception) {
            emptyList()
        }
    }
}
