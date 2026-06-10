package com.example.data

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

class TrackRepository(
    private val musicDao: MusicDao,
    private val context: Context
) {
    val allTracks: Flow<List<TrackEntity>> = musicDao.getAllTracks()
    val favoriteTracks: Flow<List<TrackEntity>> = musicDao.getFavoriteTracks()
    val allPlaylists: Flow<List<PlaylistEntity>> = musicDao.getAllPlaylists()
    val allPlaylistsWithTracks: Flow<List<PlaylistWithTracks>> = musicDao.getAllPlaylistsWithTracks()

    fun getPlaylistWithTracks(playlistId: Int): Flow<PlaylistWithTracks?> {
        return musicDao.getPlaylistWithTracksById(playlistId)
    }

    suspend fun getTrackByIdDirect(trackId: String): TrackEntity? {
        return musicDao.getTrackByIdDirect(trackId)
    }

    suspend fun toggleFavorite(trackId: String, isFav: Boolean) {
        musicDao.updateFavoriteStatus(trackId, isFav)
    }

    suspend fun createPlaylist(name: String, description: String = ""): Int {
        val playlist = PlaylistEntity(name = name, description = description)
        return musicDao.insertPlaylist(playlist).toInt()
    }

    suspend fun deletePlaylist(playlistId: Int) {
        musicDao.clearPlaylistTracks(playlistId)
        musicDao.deletePlaylist(playlistId)
    }

    suspend fun updatePlaylistDetails(playlistId: Int, name: String, description: String) {
        musicDao.updatePlaylist(playlistId, name, description)
    }

    suspend fun addTrackToPlaylist(playlistId: Int, trackId: String) {
        musicDao.insertPlaylistTrack(PlaylistTrackCrossRefEntity(playlistId, trackId))
    }

    suspend fun removeTrackFromPlaylist(playlistId: Int, trackId: String) {
        musicDao.deletePlaylistTrack(playlistId, trackId)
    }

    // Real offline download execution: downloads MP3 file, saves locally, sets Room downloaded flag
    suspend fun downloadTrack(
        trackId: String,
        onProgress: (Float) -> Unit,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        withContext(Dispatchers.IO) {
            val track = musicDao.getTrackByIdDirect(trackId)
            if (track == null) {
                onError("Track not found in DB")
                return@withContext
            }
            if (track.isDownloaded && track.localFilePath != null && File(track.localFilePath).exists()) {
                onSuccess()
                return@withContext
            }

            try {
                val url = URL(track.streamUrl)
                val connection = url.openConnection() as HttpURLConnection
                connection.connectTimeout = 10000
                connection.readTimeout = 15000
                connection.connect()

                if (connection.responseCode !in 200..299) {
                    onError("HTTP error ${connection.responseCode}")
                    return@withContext
                }

                val fileLength = connection.contentLength
                val musicDir = File(context.filesDir, "cached_music")
                if (!musicDir.exists()) {
                    musicDir.mkdirs()
                }

                val outputFile = File(musicDir, "${track.id}.mp3")
                val inputStream = connection.inputStream
                val outputStream = FileOutputStream(outputFile)

                val data = ByteArray(4096)
                var total: Long = 0
                var count: Int
                while (inputStream.read(data).also { count = it } != -1) {
                    total += count
                    if (fileLength > 0) {
                        onProgress(total.toFloat() / fileLength.toFloat())
                    }
                    outputStream.write(data, 0, count)
                }

                outputStream.flush()
                outputStream.close()
                inputStream.close()

                // Update Database
                musicDao.updateDownloadStatus(trackId, true, outputFile.absolutePath)
                Log.d("TrackRepository", "Downloaded ${track.title} to ${outputFile.absolutePath}")
                onSuccess()
            } catch (e: Exception) {
                Log.e("TrackRepository", "Error downloading ${track.title}", e)
                onError(e.localizedMessage ?: "Unknown download error")
            }
        }
    }

    suspend fun removeDownloadedTrack(trackId: String) {
        withContext(Dispatchers.IO) {
            val track = musicDao.getTrackByIdDirect(trackId)
            if (track != null && track.isDownloaded && track.localFilePath != null) {
                val file = File(track.localFilePath)
                if (file.exists()) {
                    file.delete()
                }
                musicDao.updateDownloadStatus(trackId, false, null)
            }
        }
    }

    suspend fun initializePrepopulatedTracksIfNeeded() {
        val count = musicDao.getAllTracks().first().size
        if (count == 0) {
            Log.d("TrackRepository", "Prepopulating default music catalog")
            val defaultTracks = listOf(
                TrackEntity(
                    id = "track_1",
                    title = "Midnight Horizon",
                    artist = "Neon Dreamer",
                    album = "Outrun Chill",
                    streamUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3",
                    durationMs = 372000,
                    coverArtUrl = "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=400&q=80",
                    genre = "Synthwave"
                ),
                TrackEntity(
                    id = "track_2",
                    title = "Golden Hour Beats",
                    artist = "Lofi Station",
                    album = "Sunset Lounge",
                    streamUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-3.mp3",
                    durationMs = 344000,
                    coverArtUrl = "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=400&q=80",
                    genre = "Lofi"
                ),
                TrackEntity(
                    id = "track_3",
                    title = "Cybernetic Glitch",
                    artist = "Overdrive",
                    album = "Digital Drift",
                    streamUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-5.mp3",
                    durationMs = 362000,
                    coverArtUrl = "https://images.unsplash.com/photo-1508700115892-45ecd05ae2ad?w=400&q=80",
                    genre = "Electro"
                ),
                TrackEntity(
                    id = "track_4",
                    title = "Aetherial Whispers",
                    artist = "Celestia",
                    album = "Nebula Dream",
                    streamUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-7.mp3",
                    durationMs = 422000,
                    coverArtUrl = "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=400&q=80",
                    genre = "Ambient"
                ),
                TrackEntity(
                    id = "track_5",
                    title = "Sunlight Echoes",
                    artist = "Summer Vibes",
                    album = "Island Breeze",
                    streamUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-10.mp3",
                    durationMs = 468000,
                    coverArtUrl = "https://images.unsplash.com/photo-1459749411175-04bf5292ceea?w=400&q=80",
                    genre = "Chill"
                )
            )
            musicDao.insertTracks(defaultTracks)

            // Create standard default playlists
            val playlistId = createPlaylist("My Summer Chill", "Perfect beats for hot summer evenings.")
            addTrackToPlaylist(playlistId, "track_1")
            addTrackToPlaylist(playlistId, "track_4")
            addTrackToPlaylist(playlistId, "track_5")

            val electroId = createPlaylist("Fast BPM Drive", "Adrenaline rushing synthwave and electro tracks.")
            addTrackToPlaylist(electroId, "track_1")
            addTrackToPlaylist(electroId, "track_3")
        }
    }
}
