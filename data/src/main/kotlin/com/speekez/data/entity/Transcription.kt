package com.speekez.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "transcriptions",
    foreignKeys = [
        ForeignKey(
            entity = Preset::class,
            parentColumns = ["id"],
            childColumns = ["preset_id"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index(value = ["preset_id"])]
)
data class Transcription(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "preset_id")
    val presetId: Long?,
    @ColumnInfo(name = "raw_text")
    val rawText: String,
    @ColumnInfo(name = "refined_text")
    val refinedText: String,
    @ColumnInfo(name = "audio_duration_ms")
    val audioDurationMs: Long,
    @ColumnInfo(name = "word_count")
    val wordCount: Int,
    val wpm: Float,
    @ColumnInfo(name = "is_favorite")
    val isFavorite: Boolean = false,
    @ColumnInfo(name = "created_at")
    val createdAt: Long
)
