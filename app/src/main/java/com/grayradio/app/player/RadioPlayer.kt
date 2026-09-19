package com.grayradio.app.player

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.grayradio.app.data.entity.Station
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class PlaybackUiState(
    val station: Station? = null,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val errorMessage: String? = null,
)

/**
 * Thin ExoPlayer wrapper exposing Flow state for Compose.
 */
class RadioPlayer(context: Context) {
    private val appContext = context.applicationContext

    private val _state = MutableStateFlow(PlaybackUiState())
    val state: StateFlow<PlaybackUiState> = _state.asStateFlow()

    private var player: ExoPlayer? = null

    private val listener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _state.update { it.copy(isPlaying = isPlaying) }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            val buffering = playbackState == Player.STATE_BUFFERING
            _state.update { it.copy(isBuffering = buffering) }
        }

        override fun onPlayerError(error: PlaybackException) {
            _state.update {
                it.copy(
                    isPlaying = false,
                    isBuffering = false,
                    errorMessage = error.message ?: "stream_error",
                )
            }
        }
    }

    private fun ensurePlayer(): ExoPlayer {
        player?.let { return it }
        return ExoPlayer.Builder(appContext).build().also { exo ->
            exo.addListener(listener)
            exo.playWhenReady = true
            player = exo
        }
    }

    fun play(station: Station) {
        val exo = ensurePlayer()
        val current = _state.value.station
        if (current?.id == station.id && exo.playbackState != Player.STATE_IDLE) {
            exo.playWhenReady = true
            exo.play()
            _state.update { it.copy(station = station, errorMessage = null) }
            return
        }
        _state.update {
            it.copy(
                station = station,
                isBuffering = true,
                isPlaying = false,
                errorMessage = null,
            )
        }
        exo.setMediaItem(MediaItem.fromUri(station.streamUrl))
        exo.prepare()
        exo.playWhenReady = true
        exo.play()
    }

    fun pause() {
        player?.pause()
        _state.update { it.copy(isPlaying = false) }
    }

    fun togglePlayPause() {
        val exo = player ?: return
        val station = _state.value.station ?: return
        if (exo.isPlaying) {
            pause()
        } else {
            if (exo.playbackState == Player.STATE_IDLE || exo.playbackState == Player.STATE_ENDED) {
                play(station)
            } else {
                exo.play()
            }
        }
    }

    fun stop() {
        player?.stop()
        player?.clearMediaItems()
        _state.update { PlaybackUiState() }
    }

    fun clearError() {
        _state.update { it.copy(errorMessage = null) }
    }

    fun release() {
        player?.removeListener(listener)
        player?.release()
        player = null
        _state.value = PlaybackUiState()
    }
}
