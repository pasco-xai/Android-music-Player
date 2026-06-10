package com.example.player

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.util.Log
import com.example.data.TrackEntity
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

enum class RepeatMode {
    NONE, ONE, ALL
}

class AudioPlayerManager(private val context: Context) {
    private var mediaPlayer: MediaPlayer? = null
    private val coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var progressJob: Job? = null

    private val _currentTrack = MutableStateFlow<TrackEntity?>(null)
    val currentTrack: StateFlow<TrackEntity?> = _currentTrack.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _playbackPosition = MutableStateFlow(0L)
    val playbackPosition: StateFlow<Long> = _playbackPosition.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()

    private val _repeatMode = MutableStateFlow(RepeatMode.ALL)
    val repeatMode: StateFlow<RepeatMode> = _repeatMode.asStateFlow()

    private val _isShuffleEnabled = MutableStateFlow(false)
    val isShuffleEnabled: StateFlow<Boolean> = _isShuffleEnabled.asStateFlow()

    private var originalQueue: List<TrackEntity> = emptyList()
    private val _playQueue = MutableStateFlow<List<TrackEntity>>(emptyList())
    val playQueue: StateFlow<List<TrackEntity>> = _playQueue.asStateFlow()

    init {
        // Initialize MediaPlayer
        createMediaPlayer()
    }

    private fun createMediaPlayer() {
        if (mediaPlayer == null) {
            mediaPlayer = MediaPlayer().apply {
                setOnCompletionListener {
                    handleCompletion()
                }
                setOnPreparedListener { mp ->
                    _duration.value = mp.duration.toLong()
                    mp.start()
                    _isPlaying.value = true
                    startProgressTracker()
                }
                setOnErrorListener { _, what, extra ->
                    Log.e("AudioPlayerManager", "MediaPlayer error: what=$what, extra=$extra")
                    _isPlaying.value = false
                    stopProgressTracker()
                    true
                }
            }
        }
    }

    fun setQueue(tracks: List<TrackEntity>, startWith: TrackEntity? = null) {
        originalQueue = tracks
        if (_isShuffleEnabled.value) {
            val shuffled = tracks.shuffled().toMutableList()
            if (startWith != null) {
                shuffled.remove(startWith)
                shuffled.add(0, startWith)
            }
            _playQueue.value = shuffled
        } else {
            _playQueue.value = tracks
        }

        val trackToPlay = startWith ?: _playQueue.value.firstOrNull()
        if (trackToPlay != null) {
            playTrack(trackToPlay)
        }
    }

    fun playTrack(track: TrackEntity) {
        _currentTrack.value = track
        stopProgressTracker()
        _playbackPosition.value = 0L

        try {
            mediaPlayer?.reset() ?: createMediaPlayer()

            val hasLocalFile = track.isDownloaded && track.localFilePath != null && File(track.localFilePath).exists()
            if (hasLocalFile) {
                // Play 100% offline local cached file
                Log.d("AudioPlayerManager", "Playing offline local file: ${track.localFilePath}")
                val file = File(track.localFilePath!!)
                mediaPlayer?.setDataSource(context, Uri.fromFile(file))
            } else {
                // Play online streaming source
                Log.d("AudioPlayerManager", "Streaming track online: ${track.streamUrl}")
                mediaPlayer?.setDataSource(track.streamUrl)
            }

            mediaPlayer?.prepareAsync()
        } catch (e: Exception) {
            Log.e("AudioPlayerManager", "Error preparing media player", e)
            _isPlaying.value = false
        }
    }

    fun togglePlayPause() {
        val player = mediaPlayer ?: return
        if (player.isPlaying) {
            player.pause()
            _isPlaying.value = false
            stopProgressTracker()
        } else {
            if (_currentTrack.value != null) {
                player.start()
                _isPlaying.value = true
                startProgressTracker()
            } else {
                // Play first track if any
                _playQueue.value.firstOrNull()?.let { playTrack(it) }
            }
        }
    }

    fun skipToNext() {
        val queue = _playQueue.value
        val current = _currentTrack.value
        if (queue.isEmpty() || current == null) return

        val currentIndex = queue.indexOfFirst { it.id == current.id }
        if (currentIndex < 0) return

        if (currentIndex < queue.size - 1) {
            val nextTrack = queue[currentIndex + 1]
            playTrack(nextTrack)
        } else {
            // End of queue
            if (_repeatMode.value == RepeatMode.ALL) {
                playTrack(queue.first())
            } else {
                // Stop playback
                mediaPlayer?.stop()
                _isPlaying.value = false
                _playbackPosition.value = 0L
                stopProgressTracker()
            }
        }
    }

    fun skipToPrevious() {
        val queue = _playQueue.value
        val current = _currentTrack.value
        if (queue.isEmpty() || current == null) return

        val currentIndex = queue.indexOfFirst { it.id == current.id }
        if (currentIndex < 0) return

        // If played more than 3 seconds, restart song. Otherwise, go to prev song.
        if (_playbackPosition.value > 3000) {
            seekTo(0)
        } else {
            if (currentIndex > 0) {
                val prevTrack = queue[currentIndex - 1]
                playTrack(prevTrack)
            } else {
                if (_repeatMode.value == RepeatMode.ALL) {
                    playTrack(queue.last())
                } else {
                    seekTo(0)
                }
            }
        }
    }

    fun seekTo(position: Long) {
        mediaPlayer?.let { mp ->
            try {
                mp.seekTo(position.toInt())
                _playbackPosition.value = position
            } catch (e: Exception) {
                Log.e("AudioPlayerManager", "Error seeking playback", e)
            }
        }
    }

    fun toggleShuffle() {
        val wasEnabled = _isShuffleEnabled.value
        _isShuffleEnabled.value = !wasEnabled
        val current = _currentTrack.value

        if (!wasEnabled) {
            // Enabling shuffle: Shuffle existing queue and move current track to first
            val shuffled = originalQueue.shuffled().toMutableList()
            if (current != null) {
                shuffled.remove(current)
                shuffled.add(0, current)
            }
            _playQueue.value = shuffled
        } else {
            // Disabling shuffle: Restore original queue
            _playQueue.value = originalQueue
        }
    }

    fun toggleRepeat() {
        _repeatMode.value = when (_repeatMode.value) {
            RepeatMode.NONE -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.NONE
        }
    }

    private fun handleCompletion() {
        when (_repeatMode.value) {
            RepeatMode.ONE -> {
                _playbackPosition.value = 0L
                mediaPlayer?.seekTo(0)
                mediaPlayer?.start()
                _isPlaying.value = true
                startProgressTracker()
            }
            RepeatMode.ALL, RepeatMode.NONE -> {
                skipToNext()
            }
        }
    }

    private fun startProgressTracker() {
        progressJob?.cancel()
        progressJob = coroutineScope.launch {
            while (isActive) {
                mediaPlayer?.let { mp ->
                    if (mp.isPlaying) {
                        _playbackPosition.value = mp.currentPosition.toLong()
                    }
                }
                delay(250)
            }
        }
    }

    private fun stopProgressTracker() {
        progressJob?.cancel()
        progressJob = null
    }

    fun release() {
        stopProgressTracker()
        mediaPlayer?.apply {
            try {
                if (isPlaying) {
                    stop()
                }
                release()
            } catch (e: Exception) {
                Log.e("AudioPlayerManager", "Error releasing MediaPlayer", e)
            }
        }
        mediaPlayer = null
        coroutineScope.cancel()
    }
}
