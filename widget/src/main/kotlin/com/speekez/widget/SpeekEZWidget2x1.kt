package com.speekez.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.speekez.data.presetDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SpeekEZWidget2x1 : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
        for (appWidgetId in appWidgetIds) {
            prefs.remove(PREF_PREFIX_KEY + appWidgetId)
        }
        prefs.apply()
    }

    companion object {
        private const val PREFS_NAME = "SpeekEZWidgetPrefs"
        private const val PREF_PREFIX_KEY = "appwidget_"

        fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val presetId = prefs.getLong(PREF_PREFIX_KEY + appWidgetId, -1L)

            val views = RemoteViews(context.packageName, R.layout.speekez_widget_2x1)

            if (presetId != -1L) {
                CoroutineScope(Dispatchers.IO).launch {
                    val preset = context.presetDao().getPresetById(presetId)
                    withContext(Dispatchers.Main) {
                        if (preset != null) {
                            views.setTextViewText(R.id.preset_emoji, preset.iconEmoji)
                            views.setTextViewText(R.id.preset_name, preset.name)
                            views.setTextViewText(R.id.preset_subtitle, preset.inputLanguages.joinToString(", "))

                            // Mic button intent
                            val intent = Intent().apply {
                                setClassName(context.packageName, "com.speekez.app.VoiceShortcutActivity")
                                putExtra("preset_id", preset.id.toInt())
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                            }
                            val pendingIntent = PendingIntent.getActivity(
                                context,
                                appWidgetId,
                                intent,
                                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                            )
                            views.setOnClickPendingIntent(R.id.mic_button, pendingIntent)

                            // Also allow clicking the whole widget to reconfigure or something?
                            // The task doesn't specify. I'll just keep it for the mic button for now.
                        } else {
                            views.setTextViewText(R.id.preset_name, "Preset not found")
                        }
                        appWidgetManager.updateAppWidget(appWidgetId, views)
                    }
                }
            } else {
                views.setTextViewText(R.id.preset_name, "Tap to configure")
                // Config intent
                val intent = Intent(context, SpeekEZWidget2x1ConfigActivity::class.java).apply {
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                }
                val pendingIntent = PendingIntent.getActivity(
                    context,
                    appWidgetId,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.widget_container, pendingIntent)
                appWidgetManager.updateAppWidget(appWidgetId, views)
            }
        }

        fun savePresetId(context: Context, appWidgetId: Int, presetId: Long) {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
                .putLong(PREF_PREFIX_KEY + appWidgetId, presetId)
                .apply()
        }
    }
}
