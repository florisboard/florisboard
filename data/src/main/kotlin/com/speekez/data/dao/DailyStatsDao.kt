package com.speekez.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.speekez.data.entity.DailyStats
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyStatsDao {
    @Query("SELECT * FROM daily_stats ORDER BY date DESC")
    fun getAllStats(): Flow<List<DailyStats>>

    @Query("SELECT * FROM daily_stats WHERE date = :date")
    suspend fun getByDate(date: String): DailyStats?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(stats: DailyStats)

    @Query("SELECT SUM(word_count) FROM daily_stats")
    fun getTotalWordCount(): Flow<Int?>

    @Query("SELECT SUM(recording_count) FROM daily_stats")
    fun getTotalRecordingCount(): Flow<Int?>

    @Query("SELECT * FROM daily_stats WHERE date BETWEEN :startDate AND :endDate ORDER BY date ASC")
    fun getWeeklyStats(startDate: String, endDate: String): Flow<List<DailyStats>>
}
