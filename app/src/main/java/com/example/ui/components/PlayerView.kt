package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.TrackEntity
import com.example.player.RepeatMode
import com.example.ui.MusicViewModel
import com.example.ui.theme.*

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MiniPlayer(
    viewModel: MusicViewModel,
    onExpand: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentTrack by viewModel.currentTrack.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val playbackPosition by viewModel.playbackPosition.collectAsState()
    val duration by viewModel.duration.collectAsState()
    val isDownloadingMap by viewModel.isDownloading.collectAsState()

    val track = currentTrack ?: return
    val progress = if (duration > 0) playbackPosition.toFloat() / duration.toFloat() else 0f
    val isDownloading = isDownloadingMap[track.id] ?: false

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable { onExpand() }
            .testTag("mini_player"),
        color = DeepCharcoal,
        tonalElevation = 6.dp,
        border = androidx.compose.foundation.BorderStroke(0.5.dp, BorderGray)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Linear microscopic progress indicator bar on top
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.5.dp),
                color = BrandGreen,
                trackColor = Color.Transparent
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Disk / Album art
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(LightCoal)
                ) {
                    AsyncImage(
                        model = track.coverArtUrl,
                        contentDescription = "Cover",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Scroll text marquee
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = track.title,
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        modifier = Modifier.basicMarquee()
                    )
                    Text(
                        text = track.artist,
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Download indicator icon next to title
                if (isDownloading) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .size(20.dp)
                            .padding(2.dp),
                        color = BrandGreen,
                        strokeWidth = 2.dp
                    )
                } else if (track.isDownloaded) {
                    Icon(
                        imageVector = Icons.Filled.OfflinePin,
                        contentDescription = "Cached",
                        tint = BrandGreen,
                        modifier = Modifier
                            .size(22.dp)
                            .padding(horizontal = 2.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Favorite heartbeat toggle
                IconButton(
                    onClick = { viewModel.toggleFavorite(track.id, track.isFavorite) },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = if (track.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = "Fav",
                        tint = if (track.isFavorite) SoftPink else TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Play / Pause FAB-like icon
                IconButton(
                    onClick = { viewModel.togglePlayPause() },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(LightCoal)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = "PlayPause",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Quick Skip Icon
                IconButton(
                    onClick = { viewModel.skipToNext() },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.SkipNext,
                        contentDescription = "SkipNext",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ExpandedPlayerView(
    viewModel: MusicViewModel,
    onCollapse: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentTrack by viewModel.currentTrack.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val playbackPosition by viewModel.playbackPosition.collectAsState()
    val duration by viewModel.duration.collectAsState()
    val repeatMode by viewModel.repeatMode.collectAsState()
    val isShuffleEnabled by viewModel.isShuffleEnabled.collectAsState()
    val playQueue by viewModel.playQueue.collectAsState()
    val isDownloadingMap by viewModel.isDownloading.collectAsState()

    var visualizerStyle by remember { mutableStateOf(VisualizerStyle.BARS) }
    var isShowQueue by remember { mutableStateOf(false) }

    val track = currentTrack ?: return
    val progress = if (duration > 0) playbackPosition.toFloat() / duration.toFloat() else 0f
    val isDownloading = isDownloadingMap[track.id] ?: false

    // Function to format ms into clean readable timestamp
    fun formatTime(ms: Long): String {
        val totalSecs = ms / 1000
        val mins = totalSecs / 60
        val secs = totalSecs % 60
        return String.format("%02d:%02d", mins, secs)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MidnightBlack,
                        MidnightBlack,
                        Color(0xFF030303)
                    )
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Action Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = onCollapse,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(DeepCharcoal)
                ) {
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowDown,
                        contentDescription = "Collapse",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "NOW PLAYING",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 3.sp
                        ),
                        color = TextSecondary
                    )
                    Text(
                        text = if (track.isDownloaded) "Offline Cache Library" else "Synced Audio Catalog",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }

                IconButton(
                    onClick = { isShowQueue = !isShowQueue },
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(DeepCharcoal)
                ) {
                    Icon(
                        imageVector = if (isShowQueue) Icons.Filled.QueueMusic else Icons.Outlined.QueueMusic,
                        contentDescription = "Queue",
                        tint = if (isShowQueue) BrandGreen else Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            AnimatedContent(
                targetState = isShowQueue,
                transitionSpec = {
                    fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(220))
                }, label = "QueueViewToggler",
                modifier = Modifier.weight(1f)
            ) { isQueueOpen ->
                if (isQueueOpen) {
                    // Queue Panel Display
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight()
                            .padding(vertical = 10.dp)
                    ) {
                        Text(
                            text = "Next In Queue",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            shape = RoundedCornerShape(16.dp),
                            color = DeepCharcoal.copy(alpha = 0.5f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, BorderGray)
                        ) {
                            if (playQueue.isEmpty()) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("Queue is empty", color = TextSecondary)
                                }
                            } else {
                                LazyColumn(
                                    contentPadding = PaddingValues(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(playQueue) { qTrack ->
                                        val isActive = qTrack.id == track.id
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(if (isActive) LightCoal else Color.Transparent)
                                                .clickable { viewModel.playTrack(qTrack, playQueue) }
                                                .padding(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            AsyncImage(
                                                model = qTrack.coverArtUrl,
                                                contentDescription = "Cover",
                                                modifier = Modifier
                                                    .size(40.dp)
                                                    .clip(RoundedCornerShape(4.dp)),
                                                contentScale = ContentScale.Crop
                                            )
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    qTrack.title,
                                                    color = if (isActive) BrandGreen else Color.White,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    qTrack.artist,
                                                    color = TextSecondary,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                            if (isActive) {
                                                Icon(
                                                    imageVector = Icons.Filled.VolumeUp,
                                                    contentDescription = "Active",
                                                    tint = BrandGreen,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Central Artistic Playback Controls
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceEvenly
                    ) {
                        // High-Fidelity Album Art Frame with Immersive Glow Underlay
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.85f)
                                .aspectRatio(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            // Immersive ambient soft blur glow (purple/cyan) behind card
                            Box(
                                modifier = Modifier
                                    .fillMaxSize(1.05f)
                                    .background(
                                        Brush.radialGradient(
                                            colors = listOf(
                                                BrandGreen.copy(alpha = 0.35f),
                                                NeonCyan.copy(alpha = 0.20f),
                                                Color.Transparent
                                            )
                                        )
                                    )
                            )

                            Card(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .testTag("expanded_album_cover"),
                                shape = RoundedCornerShape(40.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                                colors = CardDefaults.cardColors(containerColor = DeepCharcoal)
                            ) {
                                Box(modifier = Modifier.fillMaxSize()) {
                                    AsyncImage(
                                        model = track.coverArtUrl,
                                        contentDescription = "Song cover art",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    // Ambient gradient on album bottom edge
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .align(Alignment.BottomCenter)
                                            .height(90.dp)
                                            .background(
                                                Brush.verticalGradient(
                                                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.65f))
                                                )
                                            )
                                    )
                                }
                            }
                        }

                        // Text Info details + quick actions
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = track.title,
                                    style = MaterialTheme.typography.headlineMedium.copy(
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = Color.White,
                                    maxLines = 1,
                                    modifier = Modifier.basicMarquee()
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = track.artist,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            // Download Cache click action
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(end = 4.dp)
                            ) {
                                if (isDownloading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        color = BrandGreen,
                                        strokeWidth = 2.5.dp
                                    )
                                } else {
                                    IconButton(
                                        onClick = {
                                            if (track.isDownloaded) {
                                                viewModel.removeDownloadedTrack(track.id)
                                            } else {
                                                viewModel.downloadTrack(track.id)
                                            }
                                        },
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(LightCoal)
                                    ) {
                                        Icon(
                                            imageVector = if (track.isDownloaded) Icons.Filled.OfflinePin else Icons.Outlined.FileDownload,
                                            contentDescription = "Download Cache",
                                            tint = if (track.isDownloaded) BrandGreen else Color.White,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
                            }

                            IconButton(
                                onClick = { viewModel.toggleFavorite(track.id, track.isFavorite) },
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(LightCoal)
                            ) {
                                Icon(
                                    imageVector = if (track.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                    contentDescription = "Favorite Toggle",
                                    tint = if (track.isFavorite) SoftPink else Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        // Realistic Seekbar slider
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Slider(
                                value = progress,
                                onValueChange = { valPos ->
                                    val newPos = (valPos * duration).toLong()
                                    viewModel.seekTo(newPos)
                                },
                                colors = SliderDefaults.colors(
                                    activeTrackColor = BrandGreen,
                                    inactiveTrackColor = BorderGray,
                                    thumbColor = Color.White
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("track_seeker")
                            )

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = formatTime(playbackPosition),
                                    color = TextSecondary,
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = formatTime(duration),
                                    color = TextSecondary,
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                        }

                        // Central playback dashboard circles Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Shuffle controller
                            IconButton(onClick = { viewModel.toggleShuffle() }) {
                                Icon(
                                    imageVector = Icons.Filled.Shuffle,
                                    contentDescription = "Shuffle",
                                    tint = if (isShuffleEnabled) BrandGreen else TextMuted,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            // Skip back
                            IconButton(
                                onClick = { viewModel.skipToPrevious() },
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(DeepCharcoal)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.SkipPrevious,
                                    contentDescription = "Previous Track",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            // Central Play Pause Orb
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .background(Color.White)
                                    .clickable { viewModel.togglePlayPause() }
                                    .testTag("expanded_play_button"),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                    contentDescription = "Play/Pause",
                                    tint = Color.Black,
                                    modifier = Modifier.size(36.dp)
                                )
                            }

                            // Skip next
                            IconButton(
                                onClick = { viewModel.skipToNext() },
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(DeepCharcoal)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.SkipNext,
                                    contentDescription = "Next Track",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            // Repeat loop controller
                            IconButton(onClick = { viewModel.toggleRepeat() }) {
                                val tint = if (repeatMode != RepeatMode.NONE) BrandGreen else TextMuted
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Filled.Repeat,
                                        contentDescription = "Repeat",
                                        tint = tint,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    if (repeatMode == RepeatMode.ONE) {
                                        Text(
                                            "1",
                                            color = Color.Black,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier
                                                .background(BrandGreen, CircleShape)
                                                .padding(horizontal = 3.dp)
                                                .align(Alignment.BottomEnd)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Interactive Live Visualizer Board
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                shape = RoundedCornerShape(16.dp),
                color = DeepCharcoal,
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderGray)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        MusicVisualizer(
                            isPlaying = isPlaying,
                            style = visualizerStyle,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp)
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            accentColor = if (visualizerStyle == VisualizerStyle.COSMIC_RING) NeonCyan else BrandGreen
                        )
                    }

                    // Selector Segment buttons
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        VisualizerStyle.values().forEach { style ->
                            val isSelected = style == visualizerStyle
                            Button(
                                onClick = { visualizerStyle = style },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSelected) BrandGreen else Color.Transparent,
                                    contentColor = if (isSelected) Color.Black else TextSecondary
                                ),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                                modifier = Modifier.height(28.dp)
                            ) {
                                Text(
                                    text = when (style) {
                                        VisualizerStyle.BARS -> "EQ BARS"
                                        VisualizerStyle.WAVE_OSCILLATOR -> "WAVE"
                                        VisualizerStyle.COSMIC_RING -> "COSMIC"
                                    },
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // Status Indicator (Offline Mode Active) block
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(bottom = 16.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(DeepCharcoal)
                    .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(if (track.isDownloaded) Color(0xFF4CAF50) else BrandGreen)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (track.isDownloaded) "Offline Cache Active" else "Immersive Stream Active",
                    color = TextSecondary,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                )
            }
        }
    }
}
