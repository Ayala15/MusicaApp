package com.example.musicapp.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.musicapp.data.Api
import com.example.musicapp.data.Artist
import com.example.musicapp.data.Track
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import com.example.musicapp.data.ReccoApi
import com.example.musicapp.data.ReccoTrack

data class UiState(
    val loading: Boolean = true,
    val error: String? = null,
    val chartTracks: List<Track> = emptyList(),
    val artists: List<Artist> = emptyList(),
    val featured: List<Artist> = emptyList(),
    val query: String = "",
    val results: List<Track>? = null,
    val resultsTitle: String = "",
    val searching: Boolean = false,
    val recs: List<Track> = emptyList(),
    val recsLoading: Boolean = false,
    val recsError: Boolean = false
)

class MusicViewModel : ViewModel() {

    var state by mutableStateOf(UiState())
        private set

    private val api = Api.service
    private val recco = ReccoApi.service
    private var searchJob: Job? = null
    private var recsJob: Job? = null

    // Artistas muy conocidos que se muestran destacados en el inicio
    val featuredNames = listOf(
        "Bad Bunny", "Shakira", "Taylor Swift", "The Weeknd", "Queen", "Michael Jackson",
        "Karol G", "Daddy Yankee", "Coldplay", "Ed Sheeran", "Juanes", "Maluma"
    )

    init {
        loadHome()
    }

    fun loadHome() {
        viewModelScope.launch {
            state = state.copy(loading = true, error = null)
            try {
                withContext(Dispatchers.IO) {
                    coroutineScope {
                        val tracks = async { api.chartTracks().data }
                        val artists = async { api.chartArtists().data }
                        val featured = async {
                            featuredNames.map { name ->
                                async { runCatching { api.searchArtist(name).data.firstOrNull() }.getOrNull() }
                            }.awaitAll().filterNotNull()
                        }
                        val t = tracks.await()
                        val a = artists.await()
                        val f = featured.await()
                        withContext(Dispatchers.Main) {
                            state = state.copy(
                                loading = false,
                                chartTracks = t,
                                artists = a,
                                featured = f
                            )
                        }
                        loadRecommendations()
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                state = state.copy(loading = false, error = "No se pudo cargar el catálogo. Revisa tu conexión a internet.")
            }
        }
    }

    fun onQueryChange(q: String) {
        state = state.copy(query = q)
        searchJob?.cancel()
        if (q.isBlank()) {
            state = state.copy(results = null, searching = false, resultsTitle = "")
            return
        }
        searchJob = viewModelScope.launch {
            delay(400)
            state = state.copy(searching = true, resultsTitle = "Resultados para \"${q.trim()}\"")
            try {
                val r = withContext(Dispatchers.IO) { api.search(q.trim()).data }
                state = state.copy(results = r, searching = false)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                state = state.copy(results = emptyList(), searching = false, resultsTitle = "Sin conexión")
            }
        }
    }

    fun openArtist(artist: Artist) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            state = state.copy(query = "", searching = true, results = emptyList(), resultsTitle = artist.name)
            try {
                val r = withContext(Dispatchers.IO) { api.artistTop(artist.id).data }
                state = state.copy(results = r, searching = false)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                state = state.copy(results = emptyList(), searching = false, resultsTitle = "Sin conexión")
            }
        }
    }

    /**
     * 1) Busca en ReccoBeats algunas canciones de artistas conocidos (semillas).
     * 2) Pide /v1/track/recommendation con esas semillas.
     * 3) Cada recomendación se enlaza con Deezer (ISRC) para tener portada y audio.
     */
    fun loadRecommendations() {
        recsJob?.cancel()
        recsJob = viewModelScope.launch {
            state = state.copy(recsLoading = true, recsError = false)
            try {
                withContext(Dispatchers.IO) {
                    val picked = featuredNames.shuffled().take(3)
                    val seeds = coroutineScope {
                        picked.map { name ->
                            async {
                                runCatching {
                                    val artistId = recco.searchArtist(name).content?.firstOrNull()?.id
                                    if (artistId == null) {
                                        emptyList<String>()
                                    } else {
                                        recco.artistTracks(artistId, 2).content.orEmpty().mapNotNull { it.id }
                                    }
                                }.getOrDefault(emptyList<String>())
                            }
                        }.awaitAll().flatten()
                    }.distinct().take(5)

                    if (seeds.isEmpty()) throw IllegalStateException("Sin semillas")

                    val recs = recco.recommend(seeds.joinToString(","), 20).content.orEmpty()
                    val gate = Semaphore(2)
                    val tracks = coroutineScope {
                        recs.map { r -> async { gate.withPermit { resolveOnDeezer(r) } } }
                            .awaitAll()
                            .filterNotNull()
                    }.distinctBy { it.id }

                    withContext(Dispatchers.Main) {
                        state = state.copy(recs = tracks, recsLoading = false, recsError = tracks.isEmpty())
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                state = state.copy(recsLoading = false, recsError = true)
            }
        }
    }

    private suspend fun resolveOnDeezer(r: ReccoTrack): Track? = withContext(Dispatchers.IO) {
        val isrc = r.isrc
        if (!isrc.isNullOrBlank()) {
            val t = runCatching { api.trackByIsrc(isrc) }.getOrNull()
            if (t != null && t.id != 0L && !t.preview.isNullOrEmpty()) return@withContext t
        }
        val title = r.trackTitle ?: return@withContext null
        val artist = r.artists?.firstOrNull()?.name ?: return@withContext null
        val found = runCatching {
            api.search("artist:\"$artist\" track:\"$title\"", 1).data.firstOrNull()
        }.getOrNull()
        return@withContext found?.takeIf { it.id != 0L && !it.preview.isNullOrEmpty() }
    }

    fun clearResults() {
        searchJob?.cancel()
        state = state.copy(query = "", results = null, searching = false, resultsTitle = "")
    }
}
