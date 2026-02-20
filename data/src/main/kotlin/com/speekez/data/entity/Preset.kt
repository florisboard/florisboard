package com.speekez.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.speekez.core.ModelTier

@Entity(tableName = "presets")
data class Preset(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    @ColumnInfo(name = "icon_emoji")
    val iconEmoji: String,
    @ColumnInfo(name = "input_languages")
    val inputLanguages: List<String>,
    @ColumnInfo(name = "default_input_language")
    val defaultInputLanguage: String,
    @ColumnInfo(name = "output_languages")
    val outputLanguages: List<String>,
    @ColumnInfo(name = "default_output_language")
    val defaultOutputLanguage: String,
    @ColumnInfo(name = "refinement_level")
    val refinementLevel: RefinementLevel,
    @ColumnInfo(name = "model_tier")
    val modelTier: ModelTier,
    @ColumnInfo(name = "system_prompt")
    val systemPrompt: String,
    @ColumnInfo(name = "usage_count")
    val usageCount: Int = 0,
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long
)
