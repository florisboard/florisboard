package com.speekez.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.speekez.data.entity.Transcription
import kotlinx.coroutines.flow.Flow

@Dao
interface TranscriptionDao {
    @Query("SELECT * FROM transcriptions ORDER BY created_at DESC")
    fun getAllTranscriptions(): Flow<List<Transcription>>

    @Query("SELECT * FROM transcriptions WHERE preset_id = :presetId ORDER BY created_at DESC")
    fun getByPreset(presetId: Long): Flow<List<Transcription>>

    @Query("SELECT * FROM transcriptions WHERE is_favorite = 1 ORDER BY created_at DESC")
    fun getFavorites(): Flow<List<Transcription>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transcription: Transcription): Long

    @Delete
    suspend fun delete(transcription: Transcription)

    @Query("UPDATE transcriptions SET is_favorite = :isFavorite WHERE id = :id")
    suspend fun setFavorite(id: Long, isFavorite: Boolean)

    @Query("SELECT * FROM transcriptions WHERE raw_text LIKE '%' || :query || '%' OR refined_text LIKE '%' || :query || '%' ORDER BY created_at DESC")
    fun searchByText(query: String): Flow<List<Transcription>>

    @Query("SELECT AVG(wpm) FROM transcriptions WHERE wpm > 0")
    fun getOverallAvgWpm(): Flow<Float?>
}
