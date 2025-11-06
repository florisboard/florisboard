package dev.patrickgold.florisboard.diagnostics

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.content.FileProvider
import dev.patrickgold.florisboard.BuildConfig
import dev.patrickgold.florisboard.R
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.collections.ArrayDeque

object WhisperLogger {
    private const val TAG = "Whisper"
    private const val MAX_LINES = 500
    private val buf = ArrayDeque<String>(MAX_LINES)
    private val dateFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)
    private val cacheFileName = "whisper-current.log"

    fun maskForUi(message: String): String = maskKey(message)

    fun log(ctx: Context, msg: String) {
        val line = "[${timestamp()}] ${maskKey(msg)}"
        android.util.Log.d(TAG, line)
        synchronized(buf) {
            buf.addLast(line)
            if (buf.size > MAX_LINES) {
                buf.removeFirst()
            }
        }
        val file = File(ctx.cacheDir, cacheFileName)
        runCatching {
            file.appendText("$line\n")
        }
    }

    fun exportLogs(ctx: Context): Uri? {
        return if (Build.VERSION.SDK_INT >= 29) {
            exportViaMediaStore(ctx)
        } else {
            exportViaFileProvider(ctx)
        }
    }

    @RequiresApi(29)
    private fun exportViaMediaStore(ctx: Context): Uri? = runCatching {
        val src = File(ctx.cacheDir, cacheFileName)
        if (!src.exists() || src.length() == 0L) return@runCatching null
        val resolver = ctx.contentResolver
        val day = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())
        val name = "whisper-$day.log"

        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, name)
            put(MediaStore.Downloads.MIME_TYPE, "text/plain")
            put(MediaStore.Downloads.RELATIVE_PATH, "Download/FlorisBoard")
        }
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: return@runCatching null
        resolver.openOutputStream(uri, "w")!!.use { out ->
            src.inputStream().use { it.copyTo(out) }
        }
        uri
    }.getOrNull()

    private fun exportViaFileProvider(ctx: Context): Uri? = runCatching {
        val src = File(ctx.cacheDir, cacheFileName)
        if (!src.exists() || src.length() == 0L) return@runCatching null

        val day = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())
        val name = "whisper-$day.log"
        val dstDir = ctx.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: return@runCatching null
        val dst = File(dstDir, name)
        src.copyTo(dst, overwrite = true)

        FileProvider.getUriForFile(ctx, "${BuildConfig.APPLICATION_ID}.fileprovider", dst)
    }.getOrNull()

    private fun timestamp(): String = synchronized(dateFormat) { dateFormat.format(Date()) }

    private fun maskKey(s: String): String =
        s.replace(Regex("sk-[A-Za-z0-9]{10,}")) { m ->
            val t = m.value
            t.take(7) + "…" + t.takeLast(4)
        }
}

class ShareWhisperLogReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        when (intent.action) {
            WhisperNotify.ACTION_SHARE -> {
                val uri = WhisperLogger.exportLogs(ctx)
                if (uri == null) {
                    WhisperLogger.log(ctx, "Share logs requested but no log file available")
                    Toast.makeText(ctx, R.string.whisper_logs_unavailable, Toast.LENGTH_SHORT).show()
                } else {
                    WhisperLogger.log(ctx, "Share logs requested")
                    val share = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    val chooser = Intent.createChooser(share, ctx.getString(R.string.whisper_logs_share_title))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    ctx.startActivity(chooser)
                }
            }
            WhisperNotify.ACTION_SAVE -> {
                val uri = WhisperLogger.exportLogs(ctx)
                if (uri == null) {
                    WhisperLogger.log(ctx, "Save logs requested but export failed")
                    Toast.makeText(ctx, R.string.whisper_logs_export_failed, Toast.LENGTH_SHORT).show()
                } else {
                    WhisperLogger.log(ctx, "Logs exported")
                    val message = if (Build.VERSION.SDK_INT >= 29) {
                        R.string.whisper_logs_export_success
                    } else {
                        R.string.whisper_log_saved
                    }
                    Toast.makeText(ctx, message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}

object WhisperNotify {
    private const val CHANNEL_ID = "whisper_debug"
    const val ACTION_SHARE = "dev.patrickgold.florisboard.diagnostics.action.SHARE"
    const val ACTION_SAVE = "dev.patrickgold.florisboard.diagnostics.action.SAVE"
    private const val NOTIFICATION_ID = 1337

    fun showError(ctx: Context, title: String, text: String) {
        ensureChannel(ctx)
        val manager = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val shareIntent = Intent(ctx, ShareWhisperLogReceiver::class.java).apply {
            action = ACTION_SHARE
        }
        val sharePi = PendingIntent.getBroadcast(
            ctx,
            1,
            shareIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val saveIntent = Intent(ctx, ShareWhisperLogReceiver::class.java).apply {
            action = ACTION_SAVE
        }
        val savePi = PendingIntent.getBroadcast(
            ctx,
            2,
            saveIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val masked = WhisperLogger.maskForUi(text)
        val notification = NotificationCompat.Builder(ctx, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_info_details)
            .setContentTitle(title)
            .setContentText(masked.take(120))
            .setStyle(NotificationCompat.BigTextStyle().bigText(masked.take(500)))
            .addAction(0, ctx.getString(R.string.whisper_logs_share_action), sharePi)
            .addAction(0, ctx.getString(R.string.whisper_logs_save_action), savePi)
            .setAutoCancel(true)
            .build()

        manager.notify(NOTIFICATION_ID, notification)
    }

    private fun ensureChannel(ctx: Context) {
        val manager = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (manager.getNotificationChannel(CHANNEL_ID) == null) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                ctx.getString(R.string.whisper_logs_channel_name),
                NotificationManager.IMPORTANCE_LOW,
            )
            manager.createNotificationChannel(channel)
        }
    }
}

