package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.TrackEntity
import com.example.ui.MusicViewModel
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistDetailsScreen(
    playlistId: Int, // if -1, represents Liked Songs (Favorites)
    viewModel: MusicViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(playlistId) {
        if (playlistId != -1) {
            viewModel.selectPlaylist(playlistId)
        }
    }

    val favoritesList by viewModel.favoriteTracks.collectAsState()
    val activePlaylistDetails by viewModel.activePlaylistWithTracks.collectAsState()
    val currentTrack by viewModel.currentTrack.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()

    // Determine titles & contents depending on context ID
    val title = if (playlistId == -1) "Liked Songs" else (activePlaylistDetails?.playlist?.name ?: "Playlist")
    val subtitle = if (playlistId == -1) {
        "Your offline favorite tracks"
    } else {
        (activePlaylistDetails?.playlist?.description ?: "Custom set")
    }
    val tracks = if (playlistId == -1) favoritesList else (activePlaylistDetails?.tracks ?: emptyList())

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MidnightBlack),
        contentPadding = PaddingValues(bottom = 120.dp)
    ) {
        // High-Fidelity header banner
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                if (playlistId == -1) NeonMagenta.copy(alpha = 0.35f) else BrandGreen.copy(alpha = 0.35f),
                                MidnightBlack
                            )
                        )
                    )
                    .statusBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 12.dp)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Back Toolbar Row
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.3f))
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        // Playlist Icon Box
                        Box(
                            modifier = Modifier
                                .size(100.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(LightCoal),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (playlistId == -1) Icons.Filled.Favorite else Icons.Filled.FolderOpen,
                                contentDescription = "Playlist Vector Icon",
                                tint = if (playlistId == -1) NeonMagenta else BrandGreen,
                                modifier = Modifier.size(52.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column {
                            Text(
                                text = "OFFLINE PLAYLIST",
                                style = MaterialTheme.typography.labelSmall,
                                color = BrandAccentGreen,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 2.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = title,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        // Playback Action Controls bar (Shuffle click)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${tracks.size} tracks total",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    fontWeight = FontWeight.Bold
                )

                if (tracks.isNotEmpty()) {
                    // Spot-on Circular Giant Green Play button
                    FloatingActionButton(
                        onClick = {
                            // Enable shuffle and play first song
                            if (!viewModel.isShuffleEnabled.value) {
                                viewModel.toggleShuffle()
                            }
                            viewModel.playTrack(tracks.random(), tracks)
                        },
                        containerColor = BrandAccentGreen,
                        contentColor = Color.Black,
                        shape = CircleShape,
                        modifier = Modifier
                            .size(52.dp)
                            .testTag("playlist_shuffle_play_fab")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PlayArrow,
                            contentDescription = "Shuffle Play Songs",
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }
        }

        if (tracks.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 64.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Filled.Queue,
                            contentDescription = "Playlist Empty",
                            tint = TextMuted,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No songs here yet",
                            color = TextSecondary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Go to the Home tab and search beats to add here.",
                            color = TextMuted,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        } else {
            // Track Rows inside current playlist
            items(tracks) { track ->
                val isActive = currentTrack?.id == track.id
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.playTrack(track, tracks) }
                        .padding(horizontal = 24.dp, vertical = 8.dp)
                        .testTag("playlist_track_row_${track.id}"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AsyncImage(
                        model = track.coverArtUrl,
                        contentDescription = "Cover",
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(LightCoal),
                        contentScale = ContentScale.Crop
                    )

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = track.title,
                            color = if (isActive) BrandAccentGreen else Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = track.artist,
                            color = TextSecondary,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // For favorites list: can click to toggle fav. For other playlists: can click to delete coupling.
                    IconButton(
                        onClick = {
                            if (playlistId == -1) {
                                viewModel.toggleFavorite(track.id, true) // Toggle off since we are inside Liked list
                            } else {
                                viewModel.removeTrackFromPlaylist(playlistId, track.id)
                            }
                        }
                    ) {
                        Icon(
                            imageVector = if (playlistId == -1) Icons.Filled.Favorite else Icons.Outlined.Delete,
                            contentDescription = "Delete from playlist",
                            tint = if (playlistId == -1) NeonMagenta else TextMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}
