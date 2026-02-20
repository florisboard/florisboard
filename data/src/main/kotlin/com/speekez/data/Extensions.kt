package com.speekez.data

import android.content.Context
import com.speekez.data.dao.DailyStatsDao
import com.speekez.data.dao.PresetDao
import com.speekez.data.dao.TranscriptionDao

fun Context.speekEZDatabase(): SpeekEZDatabase {
    return SpeekEZDatabase.getInstance(this)
}

fun Context.presetDao(): PresetDao {
    return speekEZDatabase().presetDao()
}

fun Context.transcriptionDao(): TranscriptionDao {
    return speekEZDatabase().transcriptionDao()
}

fun Context.dailyStatsDao(): DailyStatsDao {
    return speekEZDatabase().dailyStatsDao()
}
