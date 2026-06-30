package com.falchi.playmixmp

import android.content.Context
import android.net.Uri
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AndroidAudioPlayer(context: Context) : AudioPlayer {
    private val exoPlayer = ExoPlayer.Builder(context)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .setUsage(C.USAGE_MEDIA)
                .build(),
            false // DISABILITATO: Permette la riproduzione simultanea per l'Automix e ignora le interruzioni (chiamate/notifiche)
        )
        .setHandleAudioBecomingNoisy(false) // DISABILITATO: La musica NON si ferma se scolleghi le cuffie
        .setWakeMode(C.WAKE_MODE_LOCAL) // Mantiene attiva la CPU durante la riproduzione
        .build()
    
    private val _playbackState = MutableStateFlow(PlaybackState.NONE)
    override val playbackState = _playbackState.asStateFlow()
    
    private val _currentPosition = MutableStateFlow(0L)
    override val currentPosition = _currentPosition.asStateFlow()
    
    private val _duration = MutableStateFlow(0L)
    override val duration = _duration.asStateFlow()
    
    private val _bufferedPosition = MutableStateFlow(0L)
    override val bufferedPosition = _bufferedPosition.asStateFlow()
    
    private val _isPlaying = MutableStateFlow(false)
    override val isPlaying = _isPlaying.asStateFlow()
    
    private var progressJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main)

    init {
        exoPlayer.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                _playbackState.value = when (state) {
                    Player.STATE_READY -> if (exoPlayer.playWhenReady) PlaybackState.PLAYING else PlaybackState.PAUSED
                    Player.STATE_BUFFERING -> PlaybackState.BUFFERING
                    else -> PlaybackState.NONE
                }
                _duration.value = exoPlayer.duration.coerceAtLeast(0L)
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _isPlaying.value = isPlaying
                if (isPlaying) {
                    startProgressUpdate()
                } else {
                    stopProgressUpdate()
                }
            }
        })
    }

    private fun startProgressUpdate() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (true) {
                _currentPosition.value = exoPlayer.currentPosition
                _bufferedPosition.value = exoPlayer.bufferedPosition
                delay(500)
            }
        }
    }

    private fun stopProgressUpdate() {
        progressJob?.cancel()
        progressJob = null
    }

    override fun prepare(uri: String, playWhenReady: Boolean, volume: Float) {
        Logger.i("Player preparing URI: $uri")
        exoPlayer.setMediaItem(MediaItem.fromUri(Uri.parse(uri)))
        exoPlayer.volume = volume
        exoPlayer.playWhenReady = playWhenReady
        exoPlayer.prepare()
    }

    override fun play() {
        Logger.i("Player play()")
        exoPlayer.play()
    }

    override fun pause() {
        Logger.i("Player pause()")
        exoPlayer.pause()
    }

    override fun stop() {
        Logger.i("Player stop()")
        exoPlayer.stop()
    }

    override fun seekTo(positionMs: Long) {
        exoPlayer.seekTo(positionMs)
    }

    override fun setVolume(volume: Float) {
        exoPlayer.volume = volume
    }

    override fun release() {
        stopProgressUpdate()
        exoPlayer.release()
    }
}
