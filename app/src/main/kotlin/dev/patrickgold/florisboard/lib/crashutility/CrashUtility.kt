/*
 * Copyright (C) 2020 Patrick Goldinger
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

import android.annotation.SuppressLint
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
import dev.patrickgold.florisboard.BuildConfig
import dev.patrickgold.florisboard.FlorisImeService
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.lib.android.AndroidVersion
import dev.patrickgold.florisboard.lib.devtools.LogTopic
import dev.patrickgold.florisboard.lib.devtools.flogError
import dev.patrickgold.florisboard.lib.devtools.flogInfo
import dev.patrickgold.florisboard.lib.io.FsDir
import dev.patrickgold.florisboard.lib.io.FsFile
import dev.patrickgold.florisboard.lib.io.subDir
import dev.patrickgold.florisboard.lib.io.subFile
import java.lang.ref.WeakReference
import kotlin.system.exitProcess

/**
 * Abstract class which holds several static methods used for handling unexpected errors.
 *
 * Parts of this class (especially the install() function and the uncaughtException() handler) have
 * been inspired by the great CustomActivityOnCrash library:
 *  https://github.com/Ereza/CustomActivityOnCrash (licensed under Apache 2.0)
 *  https://github.com/Ereza/CustomActivityOnCrash/blob/master/library/src/main/java/cat/ereza/customactivityoncrash/CustomActivityOnCrash.java
 */
abstract class CrashUtility private constructor() {
    companion object {
        private const val SHARED_PREFS_FILE = "crash_utility"
        private const val SHARED_PREFS_LAST_CRASH_TIMESTAMP = "last_crash_timestamp"

        private const val NOTIFICATION_CHANNEL_ID = "${BuildConfig.APPLICATION_ID}.lib.crashutility"
        private const val NOTIFICATION_ID = 0xFBAD0100

        private const val UNHANDLED_STACKTRACES_DIR_NAME = "unhandled_stacktraces"
        private const val UNHANDLED_STACKTRACE_FILE_EXT = "stacktrace"

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
            } else {
                val application = context.applicationContext
                if (application != null && application is Application) {
                    try {
                        Thread.setDefaultUncaughtExceptionHandler(
                            UncaughtExceptionHandler(
                                WeakReference(application),
                                WeakReference(oldHandler),
                                context.getUstDir(),
                            )
                        )
                        flogInfo(LogTopic.CRASH_UTILITY) {
                            "Successfully installed crash handler for this application!"
                        }
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
                    application.registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
                        override fun onActivityCreated(
                            activity: Activity,
                            savedInstanceState: Bundle?
                        ) {
                            if (activity !is CrashDialogActivity) {
                                lastActivityCreated = WeakReference(activity)
                            }
                        }
                        override fun onActivityStarted(activity: Activity) {}
                        override fun onActivityResumed(activity: Activity) {}
                        override fun onActivityPaused(activity: Activity) {}
                        override fun onActivityStopped(activity: Activity) {}
                        override fun onActivitySaveInstanceState(
                            activity: Activity,
                            outState: Bundle
                        ) {}
                        override fun onActivityDestroyed(activity: Activity) {}
                    })
                    if (AndroidVersion.ATLEAST_API26_O) {
                        try {
                            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE)
                            if (notificationManager != null && notificationManager is NotificationManager) {
                                val notificationChannel = NotificationChannel(
                                    NOTIFICATION_CHANNEL_ID,
                                    context.resources.getString(R.string.crash_notification_channel__title),
                                    NotificationManager.IMPORTANCE_HIGH
                                )
                                notificationManager.createNotificationChannel(notificationChannel)
                            }
                            flogInfo(LogTopic.CRASH_UTILITY) {
                                "Successfully created crash handler notification channel!"
                            }
                        } catch (e: Exception) {
                            flogError(LogTopic.CRASH_UTILITY) {
                                "Failed to create crash handler notification channel due to an unspecified error:\n$e"
                            }
                        }
                    }
                } else {
                    flogError(LogTopic.CRASH_UTILITY) {
                        "Can't install crash handler with a null Application object, doing nothing!"
                    }
                    return false
                }
            }
            return true
        }

        fun stageException(e: Throwable?) {
            if (stagedException == null) {
                stagedException = e
            }
        }

        @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
        fun handleStagedButUnhandledExceptions() {
            val e = stagedException ?: return
            val handler = Thread.getDefaultUncaughtExceptionHandler()
            if (handler is UncaughtExceptionHandler) {
                stagedException = null
                handler.uncaughtException(null, e)
            }
        }

        /**
         * Reads and returns all unhandled stacktrace files.
         *
         * @param context The current package context. If null is supplied, this function returns
         *  an empty string.
         * @return All unhandled stacktrace files or an empty list.
         */
        fun getUnhandledStacktraces(context: Context?): List<Stacktrace> {
            context ?: return listOf()
            val retList = mutableListOf<Stacktrace>()
            val ustDir = context.getUstDir()
            if (ustDir.isDirectory) {
                (ustDir.listFiles { pathname ->
                    pathname.name.endsWith(".$UNHANDLED_STACKTRACE_FILE_EXT")
                })?.forEach { file ->
                    flogInfo(LogTopic.CRASH_UTILITY) {
                        "Reading unhandled stacktrace: ${file.name}"
                    }
                    retList.add(Stacktrace(file.name, readFile(file)))
                    file.delete()
                }
            }
            return retList.toList()
        }

        fun hasUnhandledStacktraceFiles(context: Context): Boolean {
            val ustDir = context.getUstDir()
            return if (ustDir.isDirectory) {
                (ustDir.listFiles { pathname ->
                    pathname.name.endsWith(".$UNHANDLED_STACKTRACE_FILE_EXT")
                })?.isNotEmpty() ?: false
            } else {
                false
            }
        }

        /**
         * Gets the last crash timestamp from the shared preferences.
         *
         * @param context The current package context. If null is supplied, this function returns
         *  the default value for the timestamp (0).
         * @return The last time crash timestamp or 0.
         */
        private fun getLastCrashTimestamp(context: Context?): Long {
            context ?: return 0
            return context.getSharedPreferences(SHARED_PREFS_FILE, Context.MODE_PRIVATE)
                .getLong(SHARED_PREFS_LAST_CRASH_TIMESTAMP, 0)
        }

        /**
         * Sets the last crash timestamp in the shared preferences.
         *
         * @param context The current package context. If null is supplied, this function does
         *  nothing.
         * @param value The timestamp of the current crash.
         */
        @SuppressLint("ApplySharedPref")
        private fun setLastCrashTimestamp(context: Context?, value: Long) {
            context ?: return
            // Note: must use commit() instead of apply(), as the value must be immediately written
            //       to be possibly instantly read again.
            context.getSharedPreferences(SHARED_PREFS_FILE, Context.MODE_PRIVATE)
                .edit()
                .putLong(SHARED_PREFS_LAST_CRASH_TIMESTAMP, value)
                .commit()
        }

        /**
         * Gets a reference to the current unhandled stacktrace directory.
         *
         * @return The File object for the directory.
         */
        private fun Context.getUstDir(): FsDir {
            return this.noBackupFilesDir.subDir(UNHANDLED_STACKTRACES_DIR_NAME).also { it.mkdirs() }
        }

        /**
         * Gets a reference to the stacktrace file for given [timestamp].
         *
         * @param timestamp The timestamp of the stacktrace file to get.
         * @return The File object for the stacktrace file.
         */
        private fun Context.getUstFile(timestamp: Long): FsFile {
            return this.getUstDir().subFile("$timestamp.$UNHANDLED_STACKTRACE_FILE_EXT")
        }

        /**
         * Push a notification which opens [CrashDialogActivity] with given parameters.
         *
         * @param context The current package context. If null is supplied, this function does
         *  nothing.
         * @param id The ID of the notification.
         * @param title The title of the notification.
         * @param body The body of the notification.
         */
        private fun pushNotification(context: Context?, id: Int, title: String, body: String) {
            context ?: return
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE)
            if (notificationManager != null && notificationManager is NotificationManager) {
                val notificationBuilder = if (AndroidVersion.ATLEAST_API26_O) {
                    Notification.Builder(context.applicationContext, NOTIFICATION_CHANNEL_ID)
                } else {
                    @Suppress("DEPRECATION")
                    Notification.Builder(context.applicationContext).apply {
                        setPriority(Notification.PRIORITY_MAX)
                    }
                }
                val crashDialogIntent = Intent(context, CrashDialogActivity::class.java)
                val notification = notificationBuilder.run {
                    setContentTitle(title)
                    style = Notification.BigTextStyle().bigText(body)
                    setContentText(body)
                    setSmallIcon(android.R.drawable.stat_notify_error)
                    setContentIntent(PendingIntent.getActivity(context, 0, crashDialogIntent, PendingIntent.FLAG_IMMUTABLE)).setAutoCancel(
                        true
                    )
                    build()
                }
                notificationManager.notify(id, notification)
            }
        }

        /**
         * Push a notification configured for a single crash.
         *
         * @param context The current package context. If null is supplied, this function does
         *  nothing.
         */
        private fun pushCrashOnceNotification(context: Context?) {
            context ?: return
            pushNotification(
                context,
                NOTIFICATION_ID.toInt(),
                context.resources.getString(R.string.crash_once_notification__title),
                context.resources.getString(R.string.crash_once_notification__body)
            )
        }

        /**
         * Push a notification configured for multiple crashes.
         *
         * @param context The current package context. If null is supplied, this function does
         *  nothing.
         */
        private fun pushCrashMultipleNotification(context: Context?) {
            context ?: return
            pushNotification(
                context,
                NOTIFICATION_ID.toInt(),
                context.resources.getString(R.string.crash_multiple_notification__title),
                context.resources.getString(R.string.crash_multiple_notification__body)
            )
        }

        /**
         * Reads a given [file] and returns its content.
         *
         * @param file The file object.
         * @return The contents of the file or an empty string, if the file does not exist.
         */
        private fun readFile(file: FsFile): String {
            val retText = StringBuilder()
            if (file.exists()) {
                val newLine = System.lineSeparator()
                file.forEachLine {
                    retText.append(it)
                    retText.append(newLine)
                }
            }
            return retText.toString()
        }

        /**
         * Writes given [text] to given [file]. If the file already exists, its current content
         * will be overwritten.
         *
         * @param file The file object.
         * @param text The text to write to the file.
         * @return The contents of the file or an empty string, if the file does not exist.
         */
        private fun writeToFile(file: FsFile, text: String) {
            try {
                file.writeText(text)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * A simple stacktrace data class capable of holding a [name] and the [details] of a stacktrace.
     */
    data class Stacktrace(
        val name: String,
        val details: String
    )

    /**
     * Custom UncaughtExceptionHandler, which writes the captured stacktrace of the crash to the
     * internal storage, pushes a crash notification and kills the current process.
     */
    class UncaughtExceptionHandler(
        private val application: WeakReference<Application>,
        private val oldHandler: WeakReference<Thread.UncaughtExceptionHandler?>,
        private val ustDir: FsDir,
    ) : Thread.UncaughtExceptionHandler {
        override fun uncaughtException(thread: Thread?, throwable: Throwable?) {
            flogInfo(LogTopic.CRASH_UTILITY) {
                "Detected application crash, executing custom crash handler."
            }
            throwable ?: return
            val timestamp = System.currentTimeMillis()
            val stacktrace = Log.getStackTraceString(throwable)
            val ustFile = ustDir.subFile("$timestamp.$UNHANDLED_STACKTRACE_FILE_EXT")
            writeToFile(ustFile, stacktrace)
            val application = application.get()
            if (application != null) {
                val lastTimestamp = getLastCrashTimestamp(application)
                if (lastTimestamp > 0) {
                    val lastFile = application.getUstFile(lastTimestamp)
                    val lastStacktrace = readFile(lastFile)
                    if (lastStacktrace == stacktrace) {
                        // Delete last stacktrace if it matches previous unhandled one
                        lastFile.delete()
                    }
                }
                setLastCrashTimestamp(application, timestamp)
                if (timestamp - lastTimestamp < 5000) {
                    pushCrashMultipleNotification(application)
                    FlorisImeService.switchToPrevInputMethod() || FlorisImeService.switchToNextInputMethod()
                } else {
                    pushCrashOnceNotification(application)
                }
            }
            val lastActivity = lastActivityCreated.get()
            if (lastActivity != null) {
                //oldHandler.get()?.uncaughtException(thread, throwable)
                lastActivity.finish()
                lastActivityCreated.clear()
            }
            Process.killProcess(Process.myPid())
            exitProcess(10)
        }
    }
}
