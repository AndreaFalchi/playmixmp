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
    val startTimeMs: Long,
    val hotcueIndex: Int = -1,
    val type: Int = 0 // 0: Cue, 1: Fade-In, 2: Fade-Out, 3: Load, 4: Grid, 5: Loop
)

data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val duration: Long,
    val fileName: String? = null,
    val contentUri: String, // String for KMP compatibility
    val comment: String? = null,

    var isInTraktorPlaylist: Boolean = false,
    var traktorOrder: Int = -1,
    var traktorBpm: Float? = null,
    var traktorKey: String? = null,
    var nmlFileAttribute: String? = null,
    var nmlMatchedTitle: String? = null,
    var traktorCuePoints: List<TraktorCuePoint> = emptyList()
)
