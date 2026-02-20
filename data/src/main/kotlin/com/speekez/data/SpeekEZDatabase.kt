package com.speekez.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.speekez.data.converter.Converters
import com.speekez.data.dao.DailyStatsDao
import com.speekez.data.dao.PresetDao
import com.speekez.data.dao.TranscriptionDao
import com.speekez.data.entity.DailyStats
import com.speekez.data.entity.Preset
import com.speekez.data.entity.Transcription

@Database(
    entities = [Preset::class, Transcription::class, DailyStats::class],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class SpeekEZDatabase : RoomDatabase() {
    abstract fun presetDao(): PresetDao
    abstract fun transcriptionDao(): TranscriptionDao
    abstract fun dailyStatsDao(): DailyStatsDao

    companion object {
        @Volatile
        private var INSTANCE: SpeekEZDatabase? = null

        fun getInstance(context: Context): SpeekEZDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SpeekEZDatabase::class.java,
                    "speekez_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
