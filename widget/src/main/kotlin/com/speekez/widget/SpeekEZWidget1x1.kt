package com.speekez.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.RemoteViews
import com.speekez.data.presetDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SpeekEZWidget1x1 : AppWidgetProvider() {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val editor = prefs.edit()
        for (appWidgetId in appWidgetIds) {
            editor.remove(PREF_PREFIX_KEY + appWidgetId)
        }
        editor.apply()
    }

    private fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val presetId = getPresetId(context, appWidgetId)

        scope.launch {
            val emoji = withContext(Dispatchers.IO) {
                try {
                    context.presetDao().getPresetById(presetId)?.iconEmoji ?: "🎙️"
                } catch (e: Exception) {
                    Log.e("SpeekEZWidget", "Error fetching preset", e)
                    "🎙️"
                }
            }

            val views = RemoteViews(context.packageName, R.layout.widget_speekez_1x1)
            views.setTextViewText(R.id.widget_emoji, emoji)

            val intent = Intent(context, SpeekEZWidgetRecordingActivity::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
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

    companion object {
        private const val PREFS_NAME = "com.speekez.widget.SpeekEZWidget1x1"
        private const val PREF_PREFIX_KEY = "appwidget_"

        fun savePresetId(context: Context, appWidgetId: Int, presetId: Long) {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putLong(PREF_PREFIX_KEY + appWidgetId, presetId)
                .apply()
        }

        fun getPresetId(context: Context, appWidgetId: Int): Long {
            return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getLong(PREF_PREFIX_KEY + appWidgetId, 1L) // Default to 1L if not found
        }

        fun updateAllWidgets(context: Context) {
            val intent = Intent(context, SpeekEZWidget1x1::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            }
            val ids = AppWidgetManager.getInstance(context).getAppWidgetIds(
                android.content.ComponentName(context, SpeekEZWidget1x1::class.java)
            )
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            context.sendBroadcast(intent)
        }
    }
}
