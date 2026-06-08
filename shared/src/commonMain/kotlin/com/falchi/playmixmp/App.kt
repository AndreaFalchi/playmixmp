package com.falchi.playmixmp

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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

val ColorPlaying = Color(0xFF4CAF50)
val ColorPaused = Color(0xFF2196F3)
val ColorTraktor = Color(0xFFE91E63) // Original app pink/red for Traktor matches

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App(viewModel: MusicPlayerViewModel) {
    var isLocked by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showAboutPage by remember { mutableStateOf(false) }

    AppTheme {
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
                            showSettingsDialog = true
                            scope.launch { drawerState.close() }
                        }
                    )
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Info, contentDescription = null) },
                        label = { Text("About") },
                        selected = false,
                        onClick = {
                            showAboutPage = true
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
                    
                    if (showSettingsDialog) {
                        SettingsDialog(viewModel, onDismiss = { showSettingsDialog = false })
                    }

                    if (showAboutPage) {
                        AboutPage(onDismiss = { showAboutPage = false })
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsDialog(viewModel: MusicPlayerViewModel, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Settings") },
        text = {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = viewModel.showCuePoints, onCheckedChange = { viewModel.showCuePoints = it })
                    Text("Show Cue Points")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
fun AboutPage(onDismiss: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("About this App", style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "PlayM1X MP is a professional music player designed for seamless automixing and Traktor integration. It allows you to organize your music using Traktor NML playlists and perform smooth transitions with customizable crossfade.",
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(modifier = Modifier.height(32.dp))
            Button(onClick = onDismiss) {
                Text("Back")
            }
        }
    }
}

@Composable
fun LockOverlay(onUnlock: () -> Unit) {
    var lastTapTime by remember { mutableStateOf(0L) }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.8f))
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

fun now(): Long = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()


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
                    song = song,
                    isActive = index == currentIndex,
                    isPlaying = index == currentIndex && p1IsPlaying,
                    progress = progress,
                    onClick = { viewModel.playNewSong(index) }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = Color.Gray.copy(alpha = 0.3f))
            }
        }
        
        // Playlist Metadata
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Total: ${viewModel.totalSongs} songs", style = MaterialTheme.typography.labelMedium)
                Text("Time: ${formatPlaylistDuration(viewModel.totalDuration)}", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
fun SongItem(
    song: Song,
    isActive: Boolean,
    isPlaying: Boolean,
    progress: Float,
    onClick: () -> Unit
) {
    val titleColor = when {
        isActive && isPlaying -> ColorPlaying
        isActive -> ColorPaused
        song.isInTraktorPlaylist -> ColorTraktor
        else -> MaterialTheme.colorScheme.onSurface
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
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
            if (song.traktorBpm != null) {
                Text(
                    text = "${song.traktorBpm!!.toInt()} BPM",
                    style = MaterialTheme.typography.labelSmall,
                    color = ColorTraktor
                )
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
}

@Composable
fun PlayerControls(viewModel: MusicPlayerViewModel, modifier: Modifier = Modifier) {
    val currentSong = viewModel.songList.getOrNull(viewModel.currentPlayingSongIndex)
    val position by viewModel.p1Position.collectAsState()
    val duration by viewModel.p1Duration.collectAsState()

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = currentSong?.let { "Playing: ${it.title}" } ?: "No song selected",
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
                            color = Color.Green,
                            shape = MaterialTheme.shapes.extraSmall,
                            modifier = Modifier
                                .size(32.dp)
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
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = { viewModel.triggerManualAutomix() },
                enabled = !viewModel.isCurrentlyAutomixing && viewModel.currentPlayingSongIndex < viewModel.songList.size - 1,
                modifier = Modifier.height(56.dp)
            ) {
                Text("AUTOMIX")
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Autoplay", style = MaterialTheme.typography.labelMedium)
                Switch(
                    checked = viewModel.isAutoplayEnabled,
                    onCheckedChange = { viewModel.isAutoplayEnabled = it }
                )
            }
        }
        
        Row(
            modifier = Modifier.padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { viewModel.adjustCrossfade(-2000L) }) {
                Icon(Icons.Default.Remove, contentDescription = "Decrease XFade")
            }
            Text("XFade: ${viewModel.crossfadeDurationMs / 1000}s", modifier = Modifier.padding(horizontal = 8.dp))
            IconButton(onClick = { viewModel.adjustCrossfade(2000L) }) {
                Icon(Icons.Default.Add, contentDescription = "Increase XFade")
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
