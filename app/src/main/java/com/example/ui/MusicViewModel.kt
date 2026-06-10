package com.example.ui

import android.app.Application
import android.util.Log
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.PlaylistEntity
import com.example.data.PlaylistWithTracks
import com.example.data.TrackEntity
import com.example.data.TrackRepository
import com.example.player.AudioPlayerManager
import com.example.player.RepeatMode
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MusicViewModel(
    application: Application,
    private val repository: TrackRepository,
    val playerManager: AudioPlayerManager
) : AndroidViewModel(application) {

    // Streams from database/repository
    val allTracks: StateFlow<List<TrackEntity>> = repository.allTracks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoriteTracks: StateFlow<List<TrackEntity>> = repository.favoriteTracks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val playlists: StateFlow<List<PlaylistEntity>> = repository.allPlaylists
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val playlistsWithTracks: StateFlow<List<PlaylistWithTracks>> = repository.allPlaylistsWithTracks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Player and UI controls
    val currentTrack: StateFlow<TrackEntity?> = playerManager.currentTrack
    val isPlaying: StateFlow<Boolean> = playerManager.isPlaying
    val playbackPosition: StateFlow<Long> = playerManager.playbackPosition
    val duration: StateFlow<Long> = playerManager.duration
    val repeatMode: StateFlow<RepeatMode> = playerManager.repeatMode
    val isShuffleEnabled: StateFlow<Boolean> = playerManager.isShuffleEnabled
    val playQueue: StateFlow<List<TrackEntity>> = playerManager.playQueue

    // Device Network Connectivity & Offline manual override
    private val _isOfflineMode = MutableStateFlow(false)
    val isOfflineMode: StateFlow<Boolean> = _isOfflineMode.asStateFlow()

    private val _isNetworkAvailable = MutableStateFlow(true)
    val isNetworkAvailable: StateFlow<Boolean> = _isNetworkAvailable.asStateFlow()

    val isLocalCacheActive: StateFlow<Boolean> = combine(_isOfflineMode, _isNetworkAvailable) { offlineOverride, hasNetwork ->
        offlineOverride || !hasNetwork
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    // Search and filtering state
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedGenre = MutableStateFlow("All")
    val selectedGenre: StateFlow<String> = _selectedGenre.asStateFlow()

    val filteredTracks: StateFlow<List<TrackEntity>> = combine(allTracks, searchQuery, selectedGenre, isLocalCacheActive) { tracks, query, genre, offlineActive ->
        tracks.filter { track ->
            val matchesQuery = track.title.contains(query, ignoreCase = true) ||
                    track.artist.contains(query, ignoreCase = true) ||
                    track.album.contains(query, ignoreCase = true)
            val matchesGenre = (genre == "All" || track.genre.equals(genre, ignoreCase = true))
            val matchesOffline = !offlineActive || track.isDownloaded
            matchesQuery && matchesGenre && matchesOffline
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Real-time Download progress maps
    private val _downloadProgress = MutableStateFlow<Map<String, Float>>(emptyMap())
    val downloadProgress: StateFlow<Map<String, Float>> = _downloadProgress.asStateFlow()

    private val _isDownloading = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val isDownloading: StateFlow<Map<String, Boolean>> = _isDownloading.asStateFlow()

    private val _activePlaylistId = MutableStateFlow<Int?>(null)
    val activePlaylistId: StateFlow<Int?> = _activePlaylistId.asStateFlow()

    val activePlaylistWithTracks: StateFlow<PlaylistWithTracks?> = _activePlaylistId
        .flatMapLatest { id ->
            if (id != null) repository.getPlaylistWithTracks(id) else flowOf(null)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Layout configuration
    var isPlayerSheetExpanded = MutableStateFlow(false)

    init {
        // Observe and register network status
        val connectivityManager = application.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        if (connectivityManager != null) {
            val activeNetwork = connectivityManager.activeNetwork
            val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
            _isNetworkAvailable.value = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true

            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
            try {
                connectivityManager.registerNetworkCallback(request, object : ConnectivityManager.NetworkCallback() {
                    override fun onAvailable(network: Network) {
                        _isNetworkAvailable.value = true
                    }

                    override fun onLost(network: Network) {
                        _isNetworkAvailable.value = false
                    }
                })
            } catch (e: Exception) {
                Log.e("MusicViewModel", "Failed to register network callback", e)
            }
        }

        // Prepopulate core songs out of the box on first launch
        viewModelScope.launch {
            repository.initializePrepopulatedTracksIfNeeded()
        }
    }

    fun toggleOfflineMode() {
        _isOfflineMode.update { !it }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun updateSelectedGenre(genre: String) {
        _selectedGenre.value = genre
    }

    fun playTrack(track: TrackEntity, contextList: List<TrackEntity>) {
        playerManager.setQueue(contextList, startWith = track)
    }

    fun togglePlayPause() {
        playerManager.togglePlayPause()
    }

    fun skipToNext() {
        playerManager.skipToNext()
    }

    fun skipToPrevious() {
        playerManager.skipToPrevious()
    }

    fun seekTo(position: Long) {
        playerManager.seekTo(position)
    }

    fun toggleShuffle() {
        playerManager.toggleShuffle()
    }

    fun toggleRepeat() {
        playerManager.toggleRepeat()
    }

    fun toggleFavorite(trackId: String, currentStatus: Boolean) {
        viewModelScope.launch {
            repository.toggleFavorite(trackId, !currentStatus)
        }
    }

    fun createPlaylist(name: String, description: String = "") {
        viewModelScope.launch {
            repository.createPlaylist(name, description)
        }
    }

    fun selectPlaylist(playlistId: Int?) {
        _activePlaylistId.value = playlistId
    }

    fun deletePlaylist(playlistId: Int) {
        viewModelScope.launch {
            repository.deletePlaylist(playlistId)
            if (_activePlaylistId.value == playlistId) {
                _activePlaylistId.value = null
            }
        }
    }

    fun addTrackToPlaylist(playlistId: Int, trackId: String) {
        viewModelScope.launch {
            repository.addTrackToPlaylist(playlistId, trackId)
        }
    }

    fun removeTrackFromPlaylist(playlistId: Int, trackId: String) {
        viewModelScope.launch {
            repository.removeTrackFromPlaylist(playlistId, trackId)
        }
    }

    fun downloadTrack(trackId: String) {
        _isDownloading.update { it + (trackId to true) }
        _downloadProgress.update { it + (trackId to 0f) }

        viewModelScope.launch {
            repository.downloadTrack(
                trackId = trackId,
                onProgress = { progress ->
                    _downloadProgress.update { it + (trackId to progress) }
                },
                onSuccess = {
                    _isDownloading.update { it + (trackId to false) }
                    _downloadProgress.update { it - trackId }
                },
                onError = { error ->
                    Log.e("MusicViewModel", "Failed to download $trackId: $error")
                    _isDownloading.update { it + (trackId to false) }
                    _downloadProgress.update { it - trackId }
                }
            )
        }
    }

    fun removeDownloadedTrack(trackId: String) {
        viewModelScope.launch {
            repository.removeDownloadedTrack(trackId)
        }
    }

    override fun onCleared() {
        super.onCleared()
        playerManager.release()
    }

    // Static Factory standard pattern to construct ViewModel with context
    companion object {
        fun provideFactory(application: Application): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val database = AppDatabase.getDatabase(application)
                    val repository = TrackRepository(database.musicDao(), application)
                    val playerManager = AudioPlayerManager(application)
                    return MusicViewModel(application, repository, playerManager) as T
                }
            }
        }
    }
}
