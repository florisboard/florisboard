package com.speekez.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_stats")
data class DailyStats(
    @PrimaryKey
    val date: String, // YYYY-MM-DD
    @ColumnInfo(name = "recording_count")
    val recordingCount: Int,
    @ColumnInfo(name = "word_count")
    val wordCount: Int,
    @ColumnInfo(name = "avg_wpm")
    val avgWpm: Float,
    @ColumnInfo(name = "time_saved_seconds")
    val timeSavedSeconds: Long
)
