package com.bisariya.oxycarb.db

import androidx.room.Entity
import com.bisariya.oxycarb.TrackInfo

@Entity(tableName = "playlist_track_cross_ref", primaryKeys = ["playlistId", "trackId"])
data class PlaylistTrackCrossRef(
    val playlistId: Long,
    val trackId: String,
    val title: String,
    val artist: String,
    val durationMs: Long,
    val addedAt: Long = System.currentTimeMillis()
) {
    fun toTrackInfo() = TrackInfo(trackId, title, artist, durationMs)
}
