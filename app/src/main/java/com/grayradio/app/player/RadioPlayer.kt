package com.grayradio.app.player

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.grayradio.app.R
import com.grayradio.app.data.entity.Station
import com.google.common.util.concurrent.ListenableFuture
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
 * MediaController facade over [PlaybackService]. UI and ViewModel talk only to this class;
 * ExoPlayer lives in the foreground MediaSessionService so audio survives backgrounding.
 */
class RadioPlayer(context: Context) {
    private val appContext = context.applicationContext

    private val _state = MutableStateFlow(PlaybackUiState())
    val state: StateFlow<PlaybackUiState> = _state.asStateFlow()

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null
    private var pendingStation: Station? = null
    private var currentStation: Station? = null

    /** Invoked just before starting playback (e.g. request POST_NOTIFICATIONS). */
    var onPlayRequested: (() -> Unit)? = null

    private val listener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _state.update { it.copy(isPlaying = isPlaying) }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            val buffering = playbackState == Player.STATE_BUFFERING
            _state.update { it.copy(isBuffering = buffering) }
            if (playbackState == Player.STATE_IDLE && controller?.mediaItemCount == 0) {
                currentStation = null
                _state.update { PlaybackUiState() }
            }
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

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            // Keep station from our play() calls; metadata title is already set there.
        }
    }

    init {
        connect()
    }

    private fun connect() {
        val token = SessionToken(
            appContext,
            ComponentName(appContext, PlaybackService::class.java),
        )
        val future = MediaController.Builder(appContext, token).buildAsync()
        controllerFuture = future
        future.addListener(
            {
                runCatching {
                    val c = future.get()
                    controller = c
                    c.addListener(listener)
                    syncFromController(c)
                    pendingStation?.let { station ->
                        pendingStation = null
                        playInternal(c, station)
                    }
                }
            },
            ContextCompat.getMainExecutor(appContext),
        )
    }

    private fun syncFromController(c: MediaController) {
        _state.update {
            it.copy(
                isPlaying = c.isPlaying,
                isBuffering = c.playbackState == Player.STATE_BUFFERING,
                station = currentStation,
            )
        }
    }

    fun play(station: Station) {
        onPlayRequested?.invoke()
        val c = controller
        if (c == null) {
            pendingStation = station
            currentStation = station
            _state.update {
                it.copy(
                    station = station,
                    isBuffering = true,
                    isPlaying = false,
                    errorMessage = null,
                )
            }
            return
        }
        playInternal(c, station)
    }

    private fun playInternal(c: MediaController, station: Station) {
        currentStation = station
        val currentUrl = c.currentMediaItem?.localConfiguration?.uri?.toString()
        if (currentUrl == station.streamUrl && c.playbackState != Player.STATE_IDLE) {
            c.playWhenReady = true
            c.play()
            _state.update {
                it.copy(station = station, errorMessage = null, isBuffering = c.playbackState == Player.STATE_BUFFERING)
            }
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

        val artworkUri = station.logoUrl?.takeIf { it.isNotBlank() }?.let { Uri.parse(it) }
            ?: Uri.parse("android.resource://${appContext.packageName}/${R.drawable.brand_logo}")

        val mediaItem = MediaItem.Builder()
            .setUri(station.streamUrl)
            .setMediaId(station.id.toString())
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(station.name)
                    .setArtist(appContext.getString(R.string.app_name))
                    .setAlbumTitle(appContext.getString(R.string.app_name))
                    .setArtworkUri(artworkUri)
                    .setIsBrowsable(false)
                    .setIsPlayable(true)
                    .build(),
            )
            .build()

        c.setMediaItem(mediaItem)
        c.prepare()
        c.playWhenReady = true
        c.play()
    }

    fun pause() {
        controller?.pause()
        _state.update { it.copy(isPlaying = false) }
    }

    fun togglePlayPause() {
        val c = controller ?: return
        val station = currentStation ?: _state.value.station ?: return
        if (c.isPlaying) {
            pause()
        } else {
            onPlayRequested?.invoke()
            if (c.playbackState == Player.STATE_IDLE || c.playbackState == Player.STATE_ENDED || c.mediaItemCount == 0) {
                play(station)
            } else {
                c.play()
            }
        }
    }

    fun stop() {
        controller?.let { c ->
            c.stop()
            c.clearMediaItems()
        }
        pendingStation = null
        currentStation = null
        _state.update { PlaybackUiState() }
    }

    fun clearError() {
        _state.update { it.copy(errorMessage = null) }
    }

    fun release() {
        controller?.removeListener(listener)
        controller?.release()
        controller = null
        controllerFuture?.let { MediaController.releaseFuture(it) }
        controllerFuture = null
        pendingStation = null
        currentStation = null
        _state.value = PlaybackUiState()
    }
}
