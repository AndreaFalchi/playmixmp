package com.falchi.playmixmp

import androidx.compose.runtime.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

class MusicPlayerViewModel(
    private val player1: AudioPlayer,
    private val player2: AudioPlayer,
    private val mediaLibrary: MediaLibrary,
    private val nmlParser: NmlParser,
    private val scope: CoroutineScope
) {
    var songList by mutableStateOf<List<Song>>(emptyList())
        private set

    var subfolders by mutableStateOf<List<Pair<String, String>>>(emptyList())
        private set

    var currentPlayingSongIndex by mutableStateOf(-1)
        private set

    var isAutoplayEnabled by mutableStateOf(false)
    var isCurrentlyAutomixing by mutableStateOf(false)
    var crossfadeDurationMs by mutableStateOf(6000L) // Default 6s
    
    var showCuePoints by mutableStateOf(true)

    // Callbacks for UI
    var onPickFolder: (() -> Unit)? = null
    var onPickTraktorFile: (() -> Unit)? = null

    private val _primaryPlayer = MutableStateFlow(player1)
    val primaryPlayer = _primaryPlayer.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val p1Position = primaryPlayer.flatMapLatest { it.currentPosition }.stateIn(scope, SharingStarted.WhileSubscribed(), 0L)
    @OptIn(ExperimentalCoroutinesApi::class)
    val p1Duration = primaryPlayer.flatMapLatest { it.duration }.stateIn(scope, SharingStarted.WhileSubscribed(), 0L)
    @OptIn(ExperimentalCoroutinesApi::class)
    val p1IsPlaying = primaryPlayer.flatMapLatest { it.isPlaying }.stateIn(scope, SharingStarted.WhileSubscribed(), false)

    val totalSongs: Int get() = songList.size
    val totalDuration: Long get() = songList.sumOf { it.duration }

    init {
        loadSubfolders()
        scope.launch {
            p1IsPlaying.collect { isPlaying ->
                if (isPlaying && isAutoplayEnabled && !isCurrentlyAutomixing) {
                    checkAutoplay()
                }
            }
        }
    }

    private fun loadSubfolders() {
        scope.launch {
            subfolders = mediaLibrary.getSubfoldersWithAudio()
        }
    }

    private suspend fun checkAutoplay() {
        while (primaryPlayer.value.isPlaying.value && isAutoplayEnabled && !isCurrentlyAutomixing) {
            val currentPos = primaryPlayer.value.currentPosition.value
            val duration = primaryPlayer.value.duration.value
            if (duration > 0 && (duration - currentPos) <= (crossfadeDurationMs + 2000L) && currentPos > 5000) {
                initiateAutomix()
                break
            }
            delay(1000)
        }
    }

    fun loadMusic() {
        scope.launch {
            songList = mediaLibrary.getSongsFromDownloads()
        }
    }

    fun loadFromFolder(path: String) {
        scope.launch {
            songList = mediaLibrary.getSongsFromFolder(path)
            primaryPlayer.value.stop()
            currentPlayingSongIndex = -1
        }
    }

    fun loadTraktorFile(xmlData: String) {
        scope.launch {
            try {
                val (collection, playlistKeys) = nmlParser.parse(xmlData)
                println("TraktorParser: Found ${collection.size} tracks in collection and ${playlistKeys.size} in playlist")
                
                // Pre-process collection keys for faster matching
                // Traktor keys are often full paths, we want the filename part
                val processedCollection = collection.mapKeys { (key, _) ->
                    key.substringAfterLast("/:").substringAfterLast(":/").substringAfterLast("/")
                }

                // Reorder and enrich songList
                val newSongs = playlistKeys.mapNotNull { key ->
                    val cleanKey = key.substringAfterLast("/:").substringAfterLast(":/").substringAfterLast("/")
                    val trackInfo = processedCollection[cleanKey] ?: collection[key]
                    
                    if (trackInfo == null) {
                        println("TraktorParser: No info found in collection for key: $key (Cleaned: $cleanKey)")
                    }
                    
                    // Try to find matching song in current list by filename or title
                    val matchedSong = songList.find { song ->
                        val songFileName = song.fileName?.lowercase()
                        val trackFileName = trackInfo?.fileName?.lowercase()
                        
                        val fileNameMatch = songFileName != null && trackFileName != null && songFileName == trackFileName
                        
                        // Fallback: match by filename in the KEY if FILE attribute failed
                        val keyFileNameMatch = songFileName != null && cleanKey.lowercase() == songFileName
                        
                        val titleArtistMatch = trackInfo != null && 
                                               song.title.lowercase() == trackInfo.title?.lowercase() && 
                                               song.artist.lowercase() == trackInfo.artist?.lowercase()
                        
                        fileNameMatch || keyFileNameMatch || titleArtistMatch
                    }
                    
                    if (matchedSong == null) {
                        println("TraktorParser: Could not match song for key: $key (File: ${trackInfo?.fileName}, Title: ${trackInfo?.title})")
                    }

                    matchedSong?.copy(
                        isInTraktorPlaylist = true,
                        comment = trackInfo?.comment,
                        traktorBpm = trackInfo?.bpm,
                        traktorCuePoints = trackInfo?.cuePoints ?: emptyList()
                    )
                }
                
                println("TraktorParser: Successfully matched ${newSongs.size} out of ${playlistKeys.size} songs")
                
                if (newSongs.isNotEmpty()) {
                    songList = newSongs
                    primaryPlayer.value.stop()
                    currentPlayingSongIndex = -1
                } else {
                    println("TraktorParser: No songs were matched. Keeping current list.")
                }
            } catch (e: Exception) {
                println("TraktorParser: Error parsing or matching Traktor file: ${e.message}")
            }
        }
    }

    fun playNewSong(index: Int) {
        if (index !in songList.indices) return
        if (isCurrentlyAutomixing) return

        if (index == currentPlayingSongIndex) {
            togglePlayPause()
            return
        }

        currentPlayingSongIndex = index
        player2.stop()
        player1.stop()
        _primaryPlayer.value = player1
        player1.setVolume(1.0f)
        player1.prepare(songList[index].contentUri, true, 1.0f)
    }

    fun togglePlayPause() {
        if (primaryPlayer.value.isPlaying.value) {
            primaryPlayer.value.pause()
        } else {
            primaryPlayer.value.play()
        }
    }

    fun seekTo(positionMs: Long) {
        primaryPlayer.value.seekTo(positionMs)
    }

    fun jumpToCue(cue: TraktorCuePoint) {
        primaryPlayer.value.seekTo(cue.startTimeMs)
        if (!primaryPlayer.value.isPlaying.value) {
            primaryPlayer.value.play()
        }
    }

    fun adjustCrossfade(deltaMs: Long) {
        crossfadeDurationMs = (crossfadeDurationMs + deltaMs).coerceIn(0L, 20000L)
    }

    fun triggerManualAutomix() {
        if (!isCurrentlyAutomixing && currentPlayingSongIndex in 0 until songList.size - 1) {
            initiateAutomix()
        }
    }

    private fun initiateAutomix() {
        if (isCurrentlyAutomixing || currentPlayingSongIndex >= songList.size - 1) return
        
        isCurrentlyAutomixing = true
        val outgoingIndex = currentPlayingSongIndex
        val incomingIndex = outgoingIndex + 1
        
        val outgoingPlayer = _primaryPlayer.value
        val incomingPlayer = if (outgoingPlayer === player1) player2 else player1

        scope.launch {
            val nextSong = songList[incomingIndex]
            incomingPlayer.prepare(nextSong.contentUri, true, 0.0f)
            
            val steps = 20
            val stepDuration = crossfadeDurationMs / steps
            for (i in 1..steps) {
                val fraction = i.toFloat() / steps
                outgoingPlayer.setVolume(1.0f - fraction)
                incomingPlayer.setVolume(fraction)
                delay(stepDuration)
            }
            
            outgoingPlayer.stop()
            currentPlayingSongIndex = incomingIndex
            _primaryPlayer.value = incomingPlayer
            isCurrentlyAutomixing = false
            
            // If autoplay is still on, start monitoring next transition
            if (isAutoplayEnabled) {
                checkAutoplay()
            }
        }
    }
}
