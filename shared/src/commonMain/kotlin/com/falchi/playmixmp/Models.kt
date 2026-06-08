package com.falchi.playmixmp

enum class PlaybackState {
    NONE,
    PLAYING,
    PAUSED,
    BUFFERING,
}

data class SongProgress(
    val currentTimeMs: Long = 0L,
    val totalDurationMs: Long = 0L,
    val bufferPositionMs: Long = 0L
)

data class TraktorCuePoint(
    val name: String,
    val startTimeMs: Long
)

data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val duration: Long,
    val fileName: String? = null,
    val contentUri: String, // String for KMP compatibility

    var isInTraktorPlaylist: Boolean = false,
    var traktorOrder: Int = -1,
    var traktorBpm: Float? = null,
    var nmlFileAttribute: String? = null,
    var nmlMatchedTitle: String? = null,
    var traktorCuePoints: List<TraktorCuePoint> = emptyList()
)
