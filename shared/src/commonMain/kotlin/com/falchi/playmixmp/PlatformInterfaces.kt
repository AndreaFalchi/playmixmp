package com.falchi.playmixmp

import kotlinx.coroutines.flow.StateFlow

interface AudioPlayer {
    val playbackState: StateFlow<PlaybackState>
    val currentPosition: StateFlow<Long>
    val duration: StateFlow<Long>
    val bufferedPosition: StateFlow<Long>
    val isPlaying: StateFlow<Boolean>

    fun prepare(uri: String, playWhenReady: Boolean, volume: Float)
    fun play()
    fun pause()
    fun stop()
    fun seekTo(positionMs: Long)
    fun setVolume(volume: Float)
    fun release()
}

interface MediaLibrary {
    suspend fun getSongsFromDownloads(): List<Song>
    suspend fun getSongsFromFolder(folderPath: String): List<Song>
    suspend fun getSubfoldersWithAudio(): List<Pair<String, String>> // Name, Path
}

interface NmlParser {
    suspend fun parse(xmlData: String): Pair<Map<String, TraktorCollectionTrack>, List<String>>
}

data class TraktorCollectionTrack(
    val key: String,
    val title: String?,
    val fileName: String?,
    val artist: String?,
    val comment: String?,
    val bpm: Float?,
    val cuePoints: List<TraktorCuePoint>
)
