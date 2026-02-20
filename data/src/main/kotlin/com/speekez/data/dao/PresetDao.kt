package com.speekez.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.speekez.data.entity.Preset
import kotlinx.coroutines.flow.Flow

@Dao
interface PresetDao {
    @Query("SELECT * FROM presets ORDER BY usage_count DESC")
    fun getAllPresets(): Flow<List<Preset>>

    @Query("SELECT * FROM presets WHERE id = :id")
    suspend fun getPresetById(id: Long): Preset?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPreset(preset: Preset): Long

    @Update
    suspend fun updatePreset(preset: Preset)

    @Delete
    suspend fun deletePreset(preset: Preset)

    @Query("UPDATE presets SET usage_count = usage_count + 1 WHERE id = :id")
    suspend fun incrementUsageCount(id: Long)

    @Query("SELECT COUNT(*) FROM presets")
    suspend fun count(): Int
}
