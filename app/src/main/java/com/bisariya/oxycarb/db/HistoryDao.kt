package com.bisariya.oxycarb.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrack(track: TrackHistoryEntity)

    @Query("SELECT * FROM track_history WHERE isPodcast = 0 ORDER BY lastPlayedAt DESC")
    fun getRecentSongs(): Flow<List<TrackHistoryEntity>>
    
    @Query("SELECT * FROM track_history WHERE isPodcast = 1 ORDER BY lastPlayedAt DESC")
    fun getRecentPodcasts(): Flow<List<TrackHistoryEntity>>

    @Query("DELETE FROM track_history WHERE lastPlayedAt < :timestamp")
    suspend fun deleteOldHistory(timestamp: Long)

    @Query("DELETE FROM track_history")
    suspend fun clearAllHistory()
}
