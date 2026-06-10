package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.FileDownload
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
import com.example.data.PlaylistEntity
import com.example.data.TrackEntity
import com.example.ui.MusicViewModel
import com.example.ui.theme.*
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: MusicViewModel,
    onNavigateToPlaylist: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val filteredTracks by viewModel.filteredTracks.collectAsState()
    val allTracks by viewModel.allTracks.collectAsState()
    val playlists by viewModel.playlists.collectAsState()
    val currentTrack by viewModel.currentTrack.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val selectedGenre by viewModel.selectedGenre.collectAsState()
    val isDownloadingMap by viewModel.isDownloading.collectAsState()

    var showAddPlaylistDialog by remember { mutableStateOf<TrackEntity?>(null) }

    // Greet user on current local time
    val greeting = remember {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        when (hour) {
            in 0..11 -> "Good Morning"
            in 12..16 -> "Good Afternoon"
            else -> "Good Evening"
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MidnightBlack),
        contentPadding = PaddingValues(bottom = 120.dp) // Avoid cutting off behind floating players
    ) {
        // Aesthetic Top greeting cover card
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                BrandGreen.copy(alpha = 0.25f),
                                MidnightBlack
                            )
                        )
                    )
                    .padding(horizontal = 24.dp, vertical = 24.dp),
                contentAlignment = Alignment.BottomStart
            ) {
                Column {
                    Text(
                        text = greeting,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                    Text(
                        text = "Listen offline and stream high-tech beats.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            }
        }

        // Section: Genre filtering chips
        item {
            val genres = listOf("All", "Synthwave", "Lofi", "Electro", "Ambient", "Chill")
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                contentPadding = PaddingValues(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(genres) { genre ->
                    val isSelected = selectedGenre == genre
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.updateSelectedGenre(genre) },
                        label = { Text(genre, fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = BrandGreen,
                            selectedLabelColor = Color.Black,
                            containerColor = LightCoal,
                            labelColor = TextSecondary
                        ),
                        border = null,
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.testTag("genre_chip_$genre")
                    )
                }
            }
        }

        // Section: Featured dynamic streams carousels
        if (selectedGenre == "All") {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp, bottom = 8.dp)
                ) {
                    Text(
                        text = "Featured Spotlight",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                    )
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(allTracks.take(4)) { track ->
                            val isActive = currentTrack?.id == track.id
                            Card(
                                modifier = Modifier
                                    .width(160.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { viewModel.playTrack(track, allTracks) }
                                    .testTag("featured_card_${track.id}"),
                                colors = CardDefaults.cardColors(containerColor = DeepCharcoal)
                            ) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .aspectRatio(1f)
                                    ) {
                                        AsyncImage(
                                            model = track.coverArtUrl,
                                            contentDescription = track.title,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )

                                        // Play orb overlay on high-hover cards
                                        Box(
                                            modifier = Modifier
                                                .padding(8.dp)
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .background(if (isActive && isPlaying) BrandGreen else Color.Black.copy(alpha = 0.6f))
                                                .align(Alignment.BottomEnd),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = if (isActive && isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                                contentDescription = "Play Spotlight",
                                                tint = if (isActive && isPlaying) Color.Black else Color.White,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text(
                                            text = track.title,
                                            color = Color.White,
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
                                }
                            }
                        }
                    }
                }
            }

            // Section: Your Offline Playlists
            if (playlists.isNotEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp, bottom = 8.dp)
                    ) {
                        Text(
                            text = "Your Offline Playlists",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                        )
                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(horizontal = 24.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(playlists) { ply ->
                                Surface(
                                    modifier = Modifier
                                        .width(220.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable { onNavigateToPlaylist(ply.id) }
                                        .testTag("playlist_quick_card_${ply.id}"),
                                    color = DeepCharcoal,
                                    border = androidx.compose.foundation.BorderStroke(0.5.dp, BorderGray)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(50.dp)
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(LightCoal),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.MusicNote,
                                                contentDescription = "Note",
                                                tint = BrandGreen,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = ply.name,
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = if (ply.description.isEmpty()) "Playlist" else ply.description,
                                                color = TextSecondary,
                                                fontSize = 11.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Section: Core catalog listing
        item {
            Text(
                text = if (selectedGenre == "All") "Discover Music Library" else "$selectedGenre Collection",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(start = 24.dp, top = 24.dp, end = 24.dp, bottom = 12.dp)
            )
        }

        if (filteredTracks.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "No tracks found matching query", color = TextSecondary)
                }
            }
        } else {
            items(filteredTracks) { track ->
                val isActive = currentTrack?.id == track.id
                val isDownloading = isDownloadingMap[track.id] ?: false

                com.example.ui.components.TrackRowItem(
                    track = track,
                    isActive = isActive,
                    isPlaying = isActive && isPlaying,
                    isDownloading = isDownloading,
                    onClick = { viewModel.playTrack(track, filteredTracks) },
                    onFavoriteToggle = { viewModel.toggleFavorite(track.id, track.isFavorite) },
                    onDownloadClick = {
                        if (track.isDownloaded) {
                            viewModel.removeDownloadedTrack(track.id)
                        } else {
                            viewModel.downloadTrack(track.id)
                        }
                    },
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .testTag("track_row_${track.id}"),
                    trailingContent = {
                        IconButton(
                            onClick = { showAddPlaylistDialog = track },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.AddCircleOutline,
                                contentDescription = "Add Playlist",
                                tint = TextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                )
            }
        }
    }

    // Interactive Playlist Selector Dialog Box
    if (showAddPlaylistDialog != null) {
        val selectedTrack = showAddPlaylistDialog!!
        AlertDialog(
            onDismissRequest = { showAddPlaylistDialog = null },
            title = {
                Text(
                    text = "Add to playlist",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Adding \'${selectedTrack.title}\' to customizable playlist directory:",
                        color = TextSecondary,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    if (playlists.isEmpty()) {
                        Text(
                            text = "No playlists found. Create one in the Library tab first!",
                            color = NeonMagenta,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    } else {
                        playlists.forEach { ply ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        viewModel.addTrackToPlaylist(ply.id, selectedTrack.id)
                                        showAddPlaylistDialog = null
                                    }
                                    .padding(vertical = 10.dp, horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Filled.PlaylistAdd, contentDescription = "Add", tint = BrandAccentGreen)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = ply.name,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAddPlaylistDialog = null }) {
                    Text("Close", color = BrandGreen)
                }
            },
            containerColor = DeepCharcoal
        )
    }
}
