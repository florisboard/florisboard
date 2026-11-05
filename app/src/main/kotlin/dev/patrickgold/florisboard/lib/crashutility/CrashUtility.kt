/*
 * Copyright (C) 2020-2025 The FlorisBoard Contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.patrickgold.florisboard.lib.crashutility

import android.app.Activity
import android.app.Application
import android.app.Application.ActivityLifecycleCallbacks
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Process
import android.util.Log
import androidx.core.content.edit
import dev.patrickgold.florisboard.BuildConfig
import dev.patrickgold.florisboard.FlorisImeService
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.lib.devtools.LogTopic
import dev.patrickgold.florisboard.lib.devtools.flogError
import dev.patrickgold.florisboard.lib.devtools.flogInfo
import java.lang.ref.WeakReference
import org.florisboard.lib.kotlin.io.FsDir
import org.florisboard.lib.kotlin.io.FsFile
import org.florisboard.lib.kotlin.io.subDir
import org.florisboard.lib.kotlin.io.subFile
import kotlin.system.exitProcess

/**
 * Abstract class which holds several static methods used for handling unexpected errors.
 *
 * Parts of this class (especially the install() function and the uncaughtException() handler) have
 * been inspired by the great CustomActivityOnCrash library:
 *  https://github.com/Ereza/CustomActivityOnCrash (licensed under Apache 2.0)
 *  https://github.com/Ereza/CustomActivityOnCrash/blob/master/library/src/main/java/cat/ereza/customactivityoncrash/CustomActivityOnCrash.java
 */
object CrashUtility {
    /**
     * A simple stacktrace data class capable of holding a [name] and the [details] of a stacktrace.
     */
    data class Stacktrace(
        val name: String,
        val details: String,
    )

    private var lastActivityCreated: WeakReference<Activity?> = WeakReference(null)
    private var stagedException: Throwable? = null

    /**
     * Installs the CrashUtility crash handler for the given package [context]. Also registers
     * a notification channel for devices with Android 8.0+.
     *
     * @param context The current package context. If null is supplied, this function does
     *  nothing.
     * @return True if the installation was successful, false otherwise.
     */
    fun install(context: Context?): Boolean {
        if (context == null) {
            flogError(LogTopic.CRASH_UTILITY) {
                "Can't install crash handler with a null Context object, doing nothing!"
            }
            return false
        }
        val oldHandler = Thread.getDefaultUncaughtExceptionHandler()
        if (oldHandler is UncaughtExceptionHandler) {
            flogInfo(LogTopic.CRASH_UTILITY) {
                "Crash handler is already installed, doing nothing!"
            }
            return true
        }

        val application = context.applicationContext as? Application
        if (application == null) {
            flogError(LogTopic.CRASH_UTILITY) {
                "Can't install crash handler with a null Application object, doing nothing!"
            }
            return false
        }
        try {
            Thread.setDefaultUncaughtExceptionHandler(
                UncaughtExceptionHandler(application, CrashStorage(application))
            )
        } catch (e: SecurityException) {
            flogError(LogTopic.CRASH_UTILITY) {
                "Failed to install crash handler, probably due to missing runtime permission 'setDefaultUncaughtExceptionHandler':\n$e"
            }
            return false
        } catch (e: Exception) {
            flogError(LogTopic.CRASH_UTILITY) {
                "Failed to install crash handler due to an unspecified error:\n$e"
            }
            return false
        }
        flogInfo(LogTopic.CRASH_UTILITY) {
            "Successfully installed crash handler for this application!"
        }
        application.registerActivityLifecycleCallbacks(ActivityTracker)
        CrashNotificationHandler.createNotificationChannel(application)

        return true
    }

    /**
     * Utility object to update the latest created activity.
     */
    private object ActivityTracker : ActivityLifecycleCallbacks {
        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
            if (activity !is CrashDialogActivity) {
                lastActivityCreated = WeakReference(activity)
            }
        }
        override fun onActivityDestroyed(activity: Activity) {}
        override fun onActivityPaused(activity: Activity) {}
        override fun onActivityResumed(activity: Activity) {}
        override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
        override fun onActivityStarted(activity: Activity) {}
        override fun onActivityStopped(activity: Activity) {}

    }

    private object CrashNotificationHandler {
        private const val NOTIFICATION_CHANNEL_ID = "${BuildConfig.APPLICATION_ID}.lib.crashutility"
        private const val NOTIFICATION_ID = 0xFBAD0100.toInt()

        /**
         * Create the notification channel for sending crash reports.
         *
         * @param context The current package context.
         */
        fun createNotificationChannel(context: Context) {
            try {
                val notificationChannel = NotificationChannel(
                    NOTIFICATION_CHANNEL_ID,
                    context.resources.getString(R.string.crash_notification_channel__title),
                    NotificationManager.IMPORTANCE_HIGH
                )
                context.getSystemService(NotificationManager::class.java)?.createNotificationChannel(notificationChannel)
                flogInfo(LogTopic.CRASH_UTILITY) {
                    "Successfully created crash handler notification channel!"
                }
            } catch (e: Exception) {
                flogError(LogTopic.CRASH_UTILITY) {
                    "Failed to create crash handler notification channel due to an unspecified error:\n$e"
                }
            }
        }

        /**
         * Push a notification which opens [CrashDialogActivity] with given parameters.
         *
         * @param context The current package context.
         * @param repeated Flag for switching between the repeated and once notifications
         */
        fun pushNotification(context: Context, repeated: Boolean) {
            val notificationManager = context.getSystemService(NotificationManager::class.java) ?: return
            val title = context.getString(
                if (repeated) {
                    R.string.crash_multiple_notification__title
                } else {
                    R.string.crash_once_notification__title
                }
            )
            val body = context.getString(
                if (repeated) {
                    R.string.crash_multiple_notification__body
                } else {
                    R.string.crash_once_notification__body
                }
            )
            val crashDialogIntent = Intent(context, CrashDialogActivity::class.java)
            val notification = Notification.Builder(context.applicationContext, NOTIFICATION_CHANNEL_ID)
                .setContentTitle(title)
                .setStyle(Notification.BigTextStyle().bigText(body))
                .setContentText(body)
                .setSmallIcon(android.R.drawable.stat_notify_error)
                .setContentIntent(
                    PendingIntent.getActivity(context, 0, crashDialogIntent, PendingIntent.FLAG_IMMUTABLE)
                ).setAutoCancel(true)
                .build()

            notificationManager.notify(NOTIFICATION_ID, notification)
        }
    }

    private class CrashStorage(private val context: Context) {
        companion object {
            private const val SHARED_PREFS_FILE = "crash_utility"
            private const val SHARED_PREFS_LAST_CRASH_TIMESTAMP = "last_crash_timestamp"

            private const val UNHANDLED_STACKTRACES_DIR_NAME = "unhandled_stacktraces"
            private const val UNHANDLED_STACKTRACE_FILE_EXT = "stacktrace"
        }

        private val ustDir: FsDir
            get() = context.noBackupFilesDir.subDir(UNHANDLED_STACKTRACES_DIR_NAME).also { it.mkdirs() }

        private val prefs
            get() = context.getSharedPreferences(SHARED_PREFS_FILE, Context.MODE_PRIVATE)

        var lastCrashTimestamp: Long
            get() = prefs.getLong(SHARED_PREFS_LAST_CRASH_TIMESTAMP, 0)
            set(value) {
                prefs.edit(commit = true) { putLong(SHARED_PREFS_LAST_CRASH_TIMESTAMP, value) }
            }

        fun hasStackTraces(): Boolean = listStacktraceFiles().isNotEmpty()

        fun handleStackTraces(): List<Stacktrace> {
            return listStacktraceFiles().map { file ->
                flogInfo(LogTopic.CRASH_UTILITY) { "Reading unhandled stacktrace: ${file.name}" }
                Stacktrace(file.name, readFile(file)).also { file.delete() }
            }
        }

        fun saveStacktrace(timestamp: Long, stackTrace: String) {
            try {
                stacktraceFile(timestamp).writeText(stackTrace)
            } catch (e: Exception) {
                flogError(LogTopic.CRASH_UTILITY) { "Failed to write stacktrace file:\n${e.stackTraceToString()}" }
            }
        }

        fun deleteStacktraceIfDuplicate(timestamp: Long, stackTrace: String) {
            val file = stacktraceFile(timestamp)
            if (readFile(file) == stackTrace) {
                file.delete()
            }
        }

        private fun stacktraceFile(timestamp: Long): FsFile {
            return ustDir.subFile("$timestamp.$UNHANDLED_STACKTRACE_FILE_EXT")
        }


        private fun listStacktraceFiles(): List<FsFile> {
            return ustDir.listFiles()
                ?.filter { it.name.endsWith(".${UNHANDLED_STACKTRACE_FILE_EXT}") }
                .orEmpty()
        }

        private fun readFile(file: FsFile): String {
            return if (file.exists()) file.readText() else ""
        }
    }

    @Synchronized
    fun stageException(e: Throwable?) {
        if (stagedException == null) {
            stagedException = e
        }
    }

    @Synchronized
    fun handleStagedButUnhandledExceptions() {
        val e = stagedException ?: return
        val handler = Thread.getDefaultUncaughtExceptionHandler() as? UncaughtExceptionHandler ?: return
        stagedException = null
        handler.handleCrash(e)
    }

    /**
     * Reads and returns all unhandled stacktrace files.
     *
     * @param context The current package context. If null is supplied, this function returns
     *  an empty string.
     * @return All unhandled stacktrace files or an empty list.
     */
    fun getUnhandledStacktraces(context: Context?): List<Stacktrace> {
        context ?: return emptyList()
        return CrashStorage(context).handleStackTraces()
    }

    /**
     * Custom UncaughtExceptionHandler, which writes the captured stacktrace of the crash to the
     * internal storage, pushes a crash notification and kills the current process.
     */
    private class UncaughtExceptionHandler(
        private val application: Application,
        private val storage: CrashStorage,
    ) : Thread.UncaughtExceptionHandler {
        companion object {
            private const val REPEATED_CRASH_WINDOW_MS = 5000L
            private const val EXIT_CODE = 10
        }

        override fun uncaughtException(thread: Thread, throwable: Throwable) = handleCrash(throwable)

        fun handleCrash(throwable: Throwable) {
            flogInfo(LogTopic.CRASH_UTILITY) {
                "Detected application crash, executing custom crash handler."
            }
            try {
                recordCrash(throwable)
            } catch (e: Exception) {
                flogError(LogTopic.CRASH_UTILITY) { "Crash handler failed to record the crash:\n${e.stackTraceToString()}" }
            }
            lastActivityCreated.get()?.finish()
            lastActivityCreated.clear()
            Process.killProcess(Process.myPid())
            exitProcess(EXIT_CODE)
        }

        private fun recordCrash(throwable: Throwable) {
            val timestamp = System.currentTimeMillis()
            val stacktrace = Log.getStackTraceString(throwable)
            storage.saveStacktrace(timestamp, stacktrace)

            val lastTimestamp = storage.lastCrashTimestamp
            if (lastTimestamp > 0) {
                storage.deleteStacktraceIfDuplicate(lastTimestamp, stacktrace)
            }

            val isRepeatedCrash = timestamp - lastTimestamp < REPEATED_CRASH_WINDOW_MS
            CrashNotificationHandler.pushNotification(application, isRepeatedCrash)
            if (isRepeatedCrash && !FlorisImeService.switchToPrevInputMethod()) {
                FlorisImeService.switchToNextInputMethod()
            }
        }
    }

}
