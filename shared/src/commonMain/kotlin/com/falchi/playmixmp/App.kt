package com.falchi.playmixmp

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlin.time.Clock

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App(viewModel: MusicPlayerViewModel) {
    var isLocked by remember { mutableStateOf(false) }
    var showSettingsPage by remember { mutableStateOf(false) }

    AppTheme(isGrayscale = viewModel.isGrayscaleTheme) {
        val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
        val scope = rememberCoroutineScope()

        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("PlayM1X Menu", modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.titleLarge)
                    HorizontalDivider()

                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Refresh, contentDescription = null) },
                        label = { Text("Main Downloads") },
                        selected = false,
                        onClick = {
                            viewModel.loadMusic()
                            scope.launch { drawerState.close() }
                        }
                    )

                    // Dynamic subfolder entries
                    viewModel.subfolders.forEach { (name, path) ->
                        NavigationDrawerItem(
                            icon = { Icon(Icons.Default.Folder, contentDescription = null) },
                            label = { Text(name) },
                            selected = false,
                            onClick = {
                                viewModel.loadFromFolder(path)
                                scope.launch { drawerState.close() }
                            }
                        )
                    }

                    HorizontalDivider()
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.UploadFile, contentDescription = null) },
                        label = { Text("Load Traktor Playlist") },
                        selected = false,
                        onClick = {
                            viewModel.onPickTraktorFile?.invoke()
                            scope.launch { drawerState.close() }
                        }
                    )
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                        label = { Text("Settings") },
                        selected = false,
                        onClick = {
                            showSettingsPage = true
                            scope.launch { drawerState.close() }
                        }
                    )
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Lock, contentDescription = null) },
                        label = { Text("Lock Screen") },
                        selected = false,
                        onClick = {
                            isLocked = true
                            scope.launch { drawerState.close() }
                        }
                    )
                }
            }
        ) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("PlayM1X MP") },
                        navigationIcon = {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(Icons.Default.Menu, contentDescription = "Menu")
                            }
                        },
                        actions = {
                            IconButton(onClick = { isLocked = true }) {
                                Icon(Icons.Default.Lock, contentDescription = "Lock")
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            titleContentColor = MaterialTheme.colorScheme.onPrimary,
                            navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                            actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }
            ) { paddingValues ->
                Box(modifier = Modifier.fillMaxSize()) {
                    BoxWithConstraints(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
                        val isLandscape = maxWidth > maxHeight

                        if (isLandscape) {
                            Row(modifier = Modifier.fillMaxSize()) {
                                Box(modifier = Modifier.weight(1f)) {
                                    SongList(viewModel, Modifier.fillMaxSize())
                                }
                                Box(modifier = Modifier.weight(0.6f)) {
                                    PlayerControls(viewModel, Modifier.fillMaxSize().padding(16.dp))
                                }
                            }
                        } else {
                            Column(modifier = Modifier.fillMaxSize()) {
                                Box(modifier = Modifier.weight(1f)) {
                                    SongList(viewModel, Modifier.fillMaxSize())
                                }
                                PlayerControls(viewModel, Modifier.fillMaxWidth().padding(16.dp))
                            }
                        }
                    }

                    if (isLocked) {
                        LockOverlay(onUnlock = { isLocked = false })
                    }

                    if (showSettingsPage) {
                        SettingsPage(viewModel, onDismiss = { showSettingsPage = false })
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsPage(viewModel: MusicPlayerViewModel, onDismiss: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier.padding(32.dp).verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Settings", style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(24.dp))
            
            // Settings section
            Column(modifier = Modifier.fillMaxWidth()) {
                SettingSwitchRow("Show Cue Points", viewModel.showCuePoints) { viewModel.showCuePoints = it }
                SettingSwitchRow("Black & White Theme", viewModel.isGrayscaleTheme) { viewModel.isGrayscaleTheme = it }
                SettingSwitchRow("Enable Reordering", viewModel.isReorderingEnabled) { viewModel.isReorderingEnabled = it }
                SettingSwitchRow("Always on Top (Foreground)", viewModel.isAlwaysOnTop) { viewModel.toggleAlwaysOnTop(it) }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Text("Automix Lead Time: ${viewModel.automixLeadTimeMs / 1000}s", style = MaterialTheme.typography.titleMedium)
                Slider(
                    value = (viewModel.automixLeadTimeMs / 1000).toFloat(),
                    onValueChange = { viewModel.automixLeadTimeMs = it.toLong() * 1000L },
                    valueRange = 0f..60f,
                    steps = 59
                )
                Text("Adjusts how many seconds before the end of the song the automix starts.", style = MaterialTheme.typography.labelSmall)
            }

            Spacer(modifier = Modifier.height(32.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(32.dp))

            // About section
            Text("About this app", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Playm1xMP is an audio player desgined for simple and effective mixing and can manage playlists create in Traktor.",
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text("Credits: Andrea Falchi 2026", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

            Spacer(modifier = Modifier.height(8.dp))
            Text("Version: ${getPlatform().version}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

            Spacer(modifier = Modifier.height(32.dp))
            
            Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text("Back")
            }

            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
fun SettingSwitchRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
fun LockOverlay(onUnlock: () -> Unit) {
    var lastTapTime by remember { mutableStateOf(0L) }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ColorLockOverlay)
            .clickable(
                interactionSource = null,
                indication = null
            ) {
                val currentTime = now()
                if (currentTime - lastTapTime < 300) {
                    onUnlock()
                }
                lastTapTime = currentTime
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            "Double-tap to unlock",
            color = Color.White,
            style = MaterialTheme.typography.headlineMedium
        )
    }
}

fun now(): Long = Clock.System.now().toEpochMilliseconds()


@Composable
fun SongList(viewModel: MusicPlayerViewModel, modifier: Modifier = Modifier) {
    val songs = viewModel.songList
    val currentIndex = viewModel.currentPlayingSongIndex
    val p1IsPlaying by viewModel.p1IsPlaying.collectAsState()
    val p1Position by viewModel.p1Position.collectAsState()
    val p1Duration by viewModel.p1Duration.collectAsState()
    val progress = if (p1Duration > 0) p1Position.toFloat() / p1Duration.toFloat() else 0f

    Column(modifier = modifier) {
        LazyColumn(modifier = Modifier.weight(1f)) {
            itemsIndexed(songs) { index, song ->
                SongItem(
                    index = index,
                    song = song,
                    isActive = index == currentIndex,
                    isPlaying = index == currentIndex && p1IsPlaying,
                    progress = progress,
                    isReorderEnabled = viewModel.isReorderingEnabled,
                    isMoved = index == viewModel.lastMovedIndex,
                    onMoveUp = { if (index > 0) viewModel.moveSong(index, index - 1) },
                    onMoveDown = { if (index < songs.size - 1) viewModel.moveSong(index, index + 1) },
                    onClick = { viewModel.playNewSong(index) }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = ColorDivider)
            }
        }
        
        // Playlist Metadata
        val playedSongs = if (currentIndex >= 0) currentIndex + 1 else 0
        val remainingPlaylistMs = if (currentIndex >= 0) {
            val currentSongRemaining = (p1Duration - p1Position).coerceAtLeast(0L)
            val futureSongsDuration = songs.drop(currentIndex + 1).sumOf { it.duration }
            currentSongRemaining + futureSongsDuration
        } else {
            viewModel.totalDuration
        }

        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Total: $playedSongs / ${viewModel.totalSongs}", style = MaterialTheme.typography.labelMedium)
                Text("Time: ${formatPlaylistDuration(remainingPlaylistMs)} / ${formatPlaylistDuration(viewModel.totalDuration)}", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
fun SongItem(
    index: Int,
    song: Song,
    isActive: Boolean,
    isPlaying: Boolean,
    progress: Float,
    isReorderEnabled: Boolean,
    isMoved: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onClick: () -> Unit
) {
    val titleColor = when {
        isActive && isPlaying -> ColorPlaying
        isActive -> ColorPaused
        song.isInTraktorPlaylist -> ColorTraktor
        else -> MaterialTheme.colorScheme.onSurface
    }

    val backgroundColor = if (isReorderEnabled && isMoved) ColorMoving.copy(alpha = 0.2f) else Color.Transparent

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp, horizontal = 4.dp),
        shape = MaterialTheme.shapes.small,
        border = androidx.compose.foundation.BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        color = backgroundColor
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${index + 1}. ${song.title}",
                            color = titleColor,
                            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 18.sp
                        )
                        Text(
                            text = if (song.comment.isNullOrEmpty()) song.artist else "${song.artist} [${song.comment}]",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 14.sp
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (!song.traktorKey.isNullOrEmpty()) {
                            Text(
                                text = song.traktorKey!!,
                                style = MaterialTheme.typography.labelSmall,
                                color = ColorTraktor,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                        }
                        if (song.traktorBpm != null) {
                            Text(
                                text = "${song.traktorBpm!!.toInt()} BPM",
                                style = MaterialTheme.typography.labelSmall,
                                color = ColorTraktor
                            )
                        }
                    }
                }
                
                if (isActive) {
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth(),
                        color = titleColor
                    )
                }
            }

            if (isReorderEnabled) {
                Row(
                    modifier = Modifier.padding(start = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onMoveUp) {
                        Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Move Up")
                    }
                    Icon(
                        Icons.Default.DragHandle, 
                        contentDescription = "Reorder",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    IconButton(onClick = onMoveDown) {
                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Move Down")
                    }
                }
            }
        }
    }
}

@Composable
fun PlayerControls(viewModel: MusicPlayerViewModel, modifier: Modifier = Modifier) {
    val currentSong = viewModel.songList.getOrNull(viewModel.currentPlayingSongIndex)
    val position by viewModel.p1Position.collectAsState()
    val duration by viewModel.p1Duration.collectAsState()

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = currentSong?.let { "Playing: ${viewModel.currentPlayingSongIndex + 1}. ${it.title}" } ?: "No song selected",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        if (duration > 0) {
            Slider(
                value = position.toFloat(),
                onValueChange = { viewModel.seekTo(it.toLong()) },
                valueRange = 0f..duration.toFloat(),
                modifier = Modifier.fillMaxWidth()
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(formatDuration(position), style = MaterialTheme.typography.labelSmall)
                Text("-${formatDuration(duration - position)}", style = MaterialTheme.typography.labelSmall)
            }
        }

        if (viewModel.showCuePoints && currentSong != null && currentSong.traktorCuePoints.any { it.hotcueIndex != -1 }) {
            Row(
                modifier = Modifier.padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                currentSong.traktorCuePoints
                    .filter { it.hotcueIndex != -1 }
                    .sortedBy { it.hotcueIndex }
                    .forEach { cue ->
                        Surface(
                            color = ColorCuePoint,
                            shape = MaterialTheme.shapes.extraSmall,
                            modifier = Modifier
                                .size(48.dp)
                                .clickable { viewModel.jumpToCue(cue) }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = cue.hotcueIndex.toString(),
                                    color = Color.Black,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = { viewModel.triggerManualAutomix() },
                enabled = !viewModel.isCurrentlyAutomixing && viewModel.currentPlayingSongIndex < viewModel.songList.size - 1,
                modifier = Modifier.fillMaxWidth().height(64.dp)
            ) {
                Text("AUTOMIX", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
        }
        
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { viewModel.adjustCrossfade(-2000L) }) {
                    Icon(Icons.Default.Remove, contentDescription = "Decrease XFade")
                }
                Text("XFade: ${viewModel.crossfadeDurationMs / 1000}s", modifier = Modifier.padding(horizontal = 8.dp))
                IconButton(onClick = { viewModel.adjustCrossfade(2000L) }) {
                    Icon(Icons.Default.Add, contentDescription = "Increase XFade")
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Autoplay", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(end = 8.dp))
                Switch(
                    checked = viewModel.isAutoplayEnabled,
                    onCheckedChange = { viewModel.isAutoplayEnabled = it }
                )
            }
        }
    }
}

fun formatDuration(ms: Long): String {
    val totalSeconds = ms / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    
    return if (hours > 0) {
        "${hours}h ${minutes.toString().padStart(2, '0')}m"
    } else {
        "${minutes}:${seconds.toString().padStart(2, '0')}"
    }
}

fun formatPlaylistDuration(ms: Long): String {
    val totalSeconds = ms / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    
    return if (hours > 0) {
        "${hours}h ${minutes}m"
    } else {
        "${minutes}m"
    }
}
