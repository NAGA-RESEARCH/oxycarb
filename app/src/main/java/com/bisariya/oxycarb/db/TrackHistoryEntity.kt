package com.bisariya.oxycarb.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.bisariya.oxycarb.TrackInfo

@Entity(tableName = "track_history")
data class TrackHistoryEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val artist: String,
    val durationMs: Long,
    val isPodcast: Boolean,
    val lastPlayedAt: Long = System.currentTimeMillis()
) {
    fun toTrackInfo() = TrackInfo(id, title, artist, durationMs)
}
