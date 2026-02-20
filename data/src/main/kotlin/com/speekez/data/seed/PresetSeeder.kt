package com.speekez.data.seed

import com.speekez.core.ModelTier
import com.speekez.data.dao.PresetDao
import com.speekez.data.entity.Preset
import com.speekez.data.entity.RefinementLevel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object PresetSeeder {
    suspend fun seedDefaultPresetsIfEmpty(presetDao: PresetDao) = withContext(Dispatchers.IO) {
        if (presetDao.count() == 0) {
            val now = System.currentTimeMillis()
            val defaultPresets = listOf(
                Preset(
                    name = "AI Mode",
                    iconEmoji = "\uD83E\uDD16",
                    inputLanguages = listOf("en"),
                    defaultInputLanguage = "en",
                    outputLanguages = listOf("en"),
                    defaultOutputLanguage = "en",
                    refinementLevel = RefinementLevel.FULL,
                    modelTier = ModelTier.CHEAP,
                    systemPrompt = "Improve grammar and clarity while preserving the original meaning.",
                    usageCount = 0,
                    createdAt = now,
                    updatedAt = now
                ),
                Preset(
                    name = "Personal",
                    iconEmoji = "\uD83D\uDCAC",
                    inputLanguages = listOf("en", "te"),
                    defaultInputLanguage = "en",
                    outputLanguages = listOf("en"),
                    defaultOutputLanguage = "en",
                    refinementLevel = RefinementLevel.LIGHT,
                    modelTier = ModelTier.CHEAP,
                    systemPrompt = "",
                    usageCount = 0,
                    createdAt = now,
                    updatedAt = now
                ),
                Preset(
                    name = "Work",
                    iconEmoji = "\uD83D\uDCBC",
                    inputLanguages = listOf("en"),
                    defaultInputLanguage = "en",
                    outputLanguages = listOf("en"),
                    defaultOutputLanguage = "en",
                    refinementLevel = RefinementLevel.FULL,
                    modelTier = ModelTier.BEST,
                    systemPrompt = "Rewrite in a professional tone suitable for workplace communication.",
                    usageCount = 0,
                    createdAt = now,
                    updatedAt = now
                )
            )

            defaultPresets.forEach { preset ->
                presetDao.insertPreset(preset)
            }
        }
    }
}
