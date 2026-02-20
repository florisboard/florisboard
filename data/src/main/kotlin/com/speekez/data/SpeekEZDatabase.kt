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
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [Preset::class, Transcription::class, DailyStats::class],
    version = 2,
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

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE presets ADD COLUMN default_input_language TEXT NOT NULL DEFAULT 'en'")
                db.execSQL("ALTER TABLE presets RENAME COLUMN output_language TO output_languages")
                db.execSQL("ALTER TABLE presets ADD COLUMN default_output_language TEXT NOT NULL DEFAULT 'en'")
            }
        }

        fun getInstance(context: Context): SpeekEZDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SpeekEZDatabase::class.java,
                    "speekez_database"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
