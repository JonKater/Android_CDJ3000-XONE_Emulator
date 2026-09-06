package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TrackDao {
    @Query("SELECT * FROM tracks ORDER BY title ASC")
    fun getAllTracks(): Flow<List<TrackEntity>>

    @Query("SELECT * FROM tracks WHERE id = :id LIMIT 1")
    suspend fun getTrackById(id: Long): TrackEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrack(track: TrackEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTracks(tracks: List<TrackEntity>): List<Long>

    @Update
    suspend fun updateTrack(track: TrackEntity)

    @Delete
    suspend fun deleteTrack(track: TrackEntity)

    @Query("DELETE FROM tracks WHERE id = :id")
    suspend fun deleteTrackById(id: Long)

    @Query("SELECT COUNT(*) FROM tracks")
    suspend fun getTrackCount(): Int
}

@Dao
interface CuePointDao {
    @Query("SELECT * FROM cue_points WHERE trackId = :trackId ORDER BY cueIndex ASC")
    fun getCuePointsForTrack(trackId: Long): Flow<List<CuePointEntity>>

    @Query("SELECT * FROM cue_points WHERE trackId = :trackId ORDER BY cueIndex ASC")
    suspend fun getCuePointsForTrackSync(trackId: Long): List<CuePointEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setCuePoint(cuePoint: CuePointEntity): Long

    @Query("DELETE FROM cue_points WHERE trackId = :trackId AND cueIndex = :cueIndex")
    suspend fun deleteCuePoint(trackId: Long, cueIndex: Int)

    @Query("DELETE FROM cue_points WHERE trackId = :trackId")
    suspend fun clearAllCuesForTrack(trackId: Long)
}
