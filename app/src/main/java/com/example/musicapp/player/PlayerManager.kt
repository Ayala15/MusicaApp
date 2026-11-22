package com.example.musicapp.player

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import com.example.musicapp.data.Track
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class PlayerState(
    val track: Track? = null,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 30_000L,
    val hasPrev: Boolean = false,
    val hasNext: Boolean = false
)

object PlayerManager {
    private var player: ExoPlayer? = null
    private var queue: List<Track> = emptyList()
    private val handler = Handler(Looper.getMainLooper())

    private val _state = MutableStateFlow(PlayerState())
    val state: StateFlow<PlayerState> = _state

    private val ticker = object : Runnable {
        override fun run() {
            update()
            if (player?.isPlaying == true || player?.playbackState == Player.STATE_BUFFERING) {
                handler.postDelayed(this, 500)
            }
        }
    }

    @OptIn(UnstableApi::class)
    fun init(context: Context) {
        if (player != null) return

        // Configuración de búfer ultrarrápido: reduce el búfer necesario a solo 500ms
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                1000,   // Min buffer duration
                30000,  // Max buffer duration
                500,    // Buffer for playback start (0.5s en lugar de 2.5s por defecto)
                1000    // Buffer after rebuffer
            )
            .build()

        player = ExoPlayer.Builder(context.applicationContext)
            .setLoadControl(loadControl)
            .build()
            .apply {
                addListener(object : Player.Listener {
                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        update()
                        if (isPlaying) {
                            handler.removeCallbacks(ticker)
                            handler.post(ticker)
                        }
                    }

                    override fun onPlaybackStateChanged(playbackState: Int) {
                        update()
                        if (playbackState == Player.STATE_BUFFERING) {
                            handler.removeCallbacks(ticker)
                            handler.post(ticker)
                        }
                    }

                    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) = update()
                })
            }
    }

    /** Reproduce [start] y deja el resto de [tracks] en la cola. */
    fun playQueue(tracks: List<Track>, start: Track) {
        val p = player ?: return
        val playable = tracks.filter { !it.preview.isNullOrEmpty() }
        if (playable.isEmpty()) return
        val index = playable.indexOfFirst { it.id == start.id }.coerceAtLeast(0)
        queue = playable
        p.setMediaItems(playable.map { MediaItem.fromUri(it.preview!!) }, index, 0L)
        p.prepare()
        p.play()
        update()
    }

    fun togglePlay() {
        val p = player ?: return
        if (p.isPlaying) {
            p.pause()
        } else {
            if (p.playbackState == Player.STATE_ENDED) p.seekTo(0, 0L)
            p.play()
        }
        update()
    }

    fun next() { player?.seekToNextMediaItem() }
    fun previous() { player?.seekToPreviousMediaItem() }
    fun seekTo(ms: Long) { player?.seekTo(ms) }

    fun stop() {
        val p = player ?: return
        p.stop()
        p.clearMediaItems()
        queue = emptyList()
        update()
    }

    fun release() {
        handler.removeCallbacks(ticker)
        player?.release()
        player = null
        queue = emptyList()
        _state.value = PlayerState()
    }

    private fun update() {
        val p = player ?: return
        val playbackState = p.playbackState
        _state.value = PlayerState(
            track = queue.getOrNull(p.currentMediaItemIndex),
            isPlaying = p.isPlaying,
            isBuffering = playbackState == Player.STATE_BUFFERING,
            positionMs = p.currentPosition,
            durationMs = p.duration.takeIf { it > 0 } ?: 30_000L,
            hasPrev = p.hasPreviousMediaItem(),
            hasNext = p.hasNextMediaItem()
        )
    }
}
