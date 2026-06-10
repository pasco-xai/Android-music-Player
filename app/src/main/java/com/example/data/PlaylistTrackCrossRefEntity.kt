package com.example.data

import androidx.room.Entity

@Entity(
    tableName = "playlist_track_cross_ref",
    primaryKeys = ["playlistId", "trackId"]
)
data class PlaylistTrackCrossRefEntity(
    val playlistId: Int,
    val trackId: String
)
