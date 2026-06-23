package com.falchi.playmixmp

import androidx.compose.runtime.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Duration.Companion.milliseconds

class MusicPlayerViewModel(
    private val player1: AudioPlayer,
    private val player2: AudioPlayer,
    private val mediaLibrary: MediaLibrary,
    private val nmlParser: NmlParser,
    private val platformActions: PlatformActions,
    private val settingsRepository: SettingsRepository,
    private val scope: CoroutineScope
) {
    var songList by mutableStateOf<List<Song>>(emptyList())
        private set

    var subfolders by mutableStateOf<List<Pair<String, String>>>(emptyList())
        private set

    var currentPlayingSongIndex by mutableStateOf(-1)
        private set

    private var _isAutoplayEnabled by mutableStateOf(settingsRepository.getBoolean("isAutoplayEnabled", false))
    var isAutoplayEnabled: Boolean
        get() = _isAutoplayEnabled
        set(value) {
            _isAutoplayEnabled = value
            settingsRepository.putBoolean("isAutoplayEnabled", value)
        }

    var isCurrentlyAutomixing by mutableStateOf(false)

    private var _crossfadeDurationMs by mutableStateOf(settingsRepository.getLong("crossfadeDurationMs", 6000L))
    var crossfadeDurationMs: Long
        get() = _crossfadeDurationMs
        set(value) {
            _crossfadeDurationMs = value
            settingsRepository.putLong("crossfadeDurationMs", value)
        }

    private var _automixLeadTimeMs by mutableStateOf(settingsRepository.getLong("automixLeadTimeMs", 15000L))
    var automixLeadTimeMs: Long
        get() = _automixLeadTimeMs
        set(value) {
            _automixLeadTimeMs = value
            settingsRepository.putLong("automixLeadTimeMs", value)
        }
    
    private var _showCuePoints by mutableStateOf(settingsRepository.getBoolean("showCuePoints", true))
    var showCuePoints: Boolean
        get() = _showCuePoints
        set(value) {
            _showCuePoints = value
            settingsRepository.putBoolean("showCuePoints", value)
        }

    private var _isGrayscaleTheme by mutableStateOf(settingsRepository.getBoolean("isGrayscaleTheme", false))
    var isGrayscaleTheme: Boolean
        get() = _isGrayscaleTheme
        set(value) {
            _isGrayscaleTheme = value
            settingsRepository.putBoolean("isGrayscaleTheme", value)
        }

    private var _isReorderingEnabled by mutableStateOf(settingsRepository.getBoolean("isReorderingEnabled", false))
    var isReorderingEnabled: Boolean
        get() = _isReorderingEnabled
        set(value) {
            _isReorderingEnabled = value
            settingsRepository.putBoolean("isReorderingEnabled", value)
        }

    private var _isRandomOrderEnabled by mutableStateOf(settingsRepository.getBoolean("isRandomOrderEnabled", false))
    var isRandomOrderEnabled: Boolean
        get() = _isRandomOrderEnabled
        set(value) {
            _isRandomOrderEnabled = value
            settingsRepository.putBoolean("isRandomOrderEnabled", value)
        }

    var lastMovedIndex by mutableStateOf(-1)

    private var _isAlwaysOnTop by mutableStateOf(settingsRepository.getBoolean("isAlwaysOnTop", false))
    var isAlwaysOnTop: Boolean
        get() = _isAlwaysOnTop
        set(value) {
            _isAlwaysOnTop = value
            settingsRepository.putBoolean("isAlwaysOnTop", value)
        }

    private var _isLoggingEnabled by mutableStateOf(settingsRepository.getBoolean("isLoggingEnabled", true))
    var isLoggingEnabled: Boolean
        get() = _isLoggingEnabled
        set(value) {
            _isLoggingEnabled = value
            settingsRepository.putBoolean("isLoggingEnabled", value)
            Logger.isEnabled = value
        }

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
        Logger.isEnabled = isLoggingEnabled
        Logger.i("MusicPlayerViewModel initialized")

        // Initial state for always on top
        if (isAlwaysOnTop && platformActions.isAlwaysOnTopPermissionGranted()) {
            platformActions.setAlwaysOnTop(true)
        } else if (isAlwaysOnTop) {
            // Permission might have been revoked
            isAlwaysOnTop = false
        }

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
            if (duration > 0 && (duration - currentPos) <= (automixLeadTimeMs) && currentPos > 5000) {
                initiateAutomix()
                break
            }
            delay(1.seconds)
        }
    }

    fun loadMusic() {
        Logger.i("loadMusic() started. Current songList size: ${songList.size}")
        scope.launch {
            try {
                val list = mediaLibrary.getSongsFromDownloads()
                Logger.i("loadMusic() MediaLibrary returned ${list.size} songs")
                songList = if (isRandomOrderEnabled) list.shuffled() else list
                Logger.i("loadMusic() completed. New songList size: ${songList.size}")
            } catch (e: Exception) {
                Logger.e("Error in loadMusic()", e)
            }
        }
    }

    fun loadFromFolder(path: String) {
        Logger.i("loadFromFolder() started for path: $path. Current songList size: ${songList.size}")
        scope.launch {
            try {
                val list = mediaLibrary.getSongsFromFolder(path)
                Logger.i("loadFromFolder() MediaLibrary returned ${list.size} songs")
                songList = if (isRandomOrderEnabled) list.shuffled() else list
                primaryPlayer.value.stop()
                currentPlayingSongIndex = -1
                Logger.i("loadFromFolder() completed. New songList size: ${songList.size}")
            } catch (e: Exception) {
                Logger.e("Error in loadFromFolder()", e)
            }
        }
    }

    fun loadTraktorFile(xmlData: String) {
        Logger.i("loadTraktorFile() started. Data size: ${xmlData.length}. Current songList size: ${songList.size}")
        scope.launch {
            try {
                val (collection, playlistKeys) = nmlParser.parse(xmlData)
                Logger.i("TraktorParser: Found ${collection.size} tracks in collection and ${playlistKeys.size} in playlist")
                
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
                                               song.title.equals(trackInfo.title, ignoreCase = true) && 
                                               song.artist.equals(trackInfo.artist, ignoreCase = true)
                        
                        fileNameMatch || keyFileNameMatch || titleArtistMatch
                    }
                    
                    if (matchedSong == null) {
                        println("TraktorParser: Could not match song for key: $key (File: ${trackInfo?.fileName}, Title: ${trackInfo?.title})")
                    }

                    matchedSong?.copy(
                        isInTraktorPlaylist = true,
                        comment = trackInfo?.comment,
                        traktorBpm = trackInfo?.bpm,
                        traktorKey = trackInfo?.traktorKey,
                        traktorCuePoints = trackInfo?.cuePoints ?: emptyList()
                    )
                }
                
                Logger.i("TraktorParser: Successfully matched ${newSongs.size} out of ${playlistKeys.size} songs")
                
                if (newSongs.isNotEmpty()) {
                    songList = newSongs
                    primaryPlayer.value.stop()
                    currentPlayingSongIndex = -1
                    Logger.i("loadTraktorFile() completed. New songList size: ${songList.size}")
                } else {
                    Logger.i("TraktorParser: No songs were matched. Keeping current list (size: ${songList.size})")
                }
            } catch (e: Exception) {
                Logger.e("TraktorParser: Error parsing or matching Traktor file", e)
            }
        }
    }

    fun playNewSong(index: Int) {
        if (index !in songList.indices) {
            Logger.e("Cannot play song at index $index: out of bounds (size: ${songList.size})")
            return
        }
        if (isCurrentlyAutomixing) {
            Logger.i("Cannot play new song: automix in progress")
            return
        }

        if (index == currentPlayingSongIndex) {
            Logger.i("Toggling play/pause for current song at index $index")
            togglePlayPause()
            return
        }

        Logger.i("Playing new song: ${songList[index].title} (Index: $index)")
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

    fun moveSong(fromIndex: Int, toIndex: Int) {
        if (fromIndex !in songList.indices || toIndex !in songList.indices || fromIndex == toIndex) return
        
        val mutableList = songList.toMutableList()
        val song = mutableList.removeAt(fromIndex)
        mutableList.add(toIndex, song)
        
        // Update currentPlayingSongIndex to track the same song
        val newPlayingIndex = when {
            currentPlayingSongIndex == fromIndex -> toIndex
            currentPlayingSongIndex in fromIndex..toIndex -> currentPlayingSongIndex - 1
            currentPlayingSongIndex in toIndex..fromIndex -> currentPlayingSongIndex + 1
            else -> currentPlayingSongIndex
        }
        
        currentPlayingSongIndex = newPlayingIndex
        songList = mutableList
        lastMovedIndex = toIndex
    }

    fun toggleAlwaysOnTop(enabled: Boolean) {
        if (enabled) {
            if (platformActions.isAlwaysOnTopPermissionGranted()) {
                isAlwaysOnTop = true
                platformActions.setAlwaysOnTop(true)
            } else {
                platformActions.requestAlwaysOnTopPermission()
            }
        } else {
            isAlwaysOnTop = false
            platformActions.setAlwaysOnTop(false)
        }
    }

    fun shareLogs() {
        platformActions.shareLogFile(Logger.getLogFilePath())
    }

    private fun initiateAutomix() {
        if (isCurrentlyAutomixing || currentPlayingSongIndex >= songList.size - 1) {
            Logger.i("Automix aborted: isCurrentlyAutomixing=$isCurrentlyAutomixing, index=$currentPlayingSongIndex")
            return
        }
        
        isCurrentlyAutomixing = true
        val outgoingIndex = currentPlayingSongIndex
        val incomingIndex = outgoingIndex + 1
        
        val outgoingPlayer = _primaryPlayer.value
        val incomingPlayer = if (outgoingPlayer === player1) player2 else player1

        Logger.i("Initiating automix: $outgoingIndex -> $incomingIndex")

        scope.launch {
            try {
                val nextSong = songList[incomingIndex]
                Logger.i("Preparing incoming song: ${nextSong.title}")
                incomingPlayer.prepare(nextSong.contentUri, true, 0.0f)
                
                val steps = 20
                val stepDuration = crossfadeDurationMs / steps
                for (i in 1..steps) {
                    val fraction = i.toFloat() / steps
                    outgoingPlayer.setVolume(1.0f - fraction)
                    incomingPlayer.setVolume(fraction)
                    delay(stepDuration.milliseconds)
                }
                
                outgoingPlayer.stop()
                currentPlayingSongIndex = incomingIndex
                _primaryPlayer.value = incomingPlayer
                isCurrentlyAutomixing = false
                Logger.i("Automix completed. Now playing: ${songList[incomingIndex].title}")
                
                // If autoplay is still on, start monitoring next transition
                if (isAutoplayEnabled) {
                    checkAutoplay()
                }
            } catch (e: Exception) {
                Logger.e("Error during automix", e)
                isCurrentlyAutomixing = false
            }
        }
    }
}
