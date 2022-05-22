/*
 * Copyright (C) 2021 Patrick Goldinger
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

package dev.patrickgold.florisboard.lib.devtools

import android.content.Context
import android.util.Log
import dev.patrickgold.florisboard.appContext
import dev.patrickgold.florisboard.lib.devtools.Flog.OUTPUT_CONSOLE
import dev.patrickgold.florisboard.lib.devtools.Flog.createTag
import dev.patrickgold.florisboard.lib.devtools.Flog.getStacktraceElement
import dev.patrickgold.florisboard.lib.devtools.Flog.log
import java.lang.ref.WeakReference
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/** Type alias for a flog topic Integer. */
typealias FlogTopic = UInt

/** Type alias for a flog level Integer. */
typealias FlogLevel = UInt

/** Type alias for a flog output Integer. */
typealias FlogOutput = UInt

/**
 * Logs an error message returned by [block] together with the automatically retrieved
 * calling class and method name either to the console or to a log file. The class name
 * is used for the tag, the method name prepended to the message.
 *
 * This method automatically evaluates if logging is enabled and calls [block] only
 * if a log message should be generated.
 *
 * Optionally a [topic] can also be specified to allow to only partially enable
 * debug messages across the codebase. The passed [topic] is compared with the
 * currently active [Flog.flogTopics] variable and only if at least 1 topic match
 * is found, [block] will be called and a log message written.
 *
 * @param topic The topic of this message. To specify multiple topics, use the binary
 *  OR operator. Defaults to [Flog.TOPIC_OTHER].
 * @param block The lambda expression to evaluate the message which is appended to the
 *  method name. Is called only if logging is enabled and the topics match. Must return
 *  a [String]. If this argument is omitted, only the calling method name will be used
 *  as the log message.
 */
inline fun flogError(topic: FlogTopic = Flog.TOPIC_OTHER, block: () -> String = { "" }) {
    contract {
        callsInPlace(block, InvocationKind.AT_MOST_ONCE)
    }
    if (Flog.checkShouldFlog(topic, Flog.LEVEL_ERROR)) {
        log(Flog.LEVEL_ERROR, block())
    }
}

/**
 * Logs a warning message returned by [block] together with the automatically retrieved
 * calling class and method name either to the console or to a log file. The class name
 * is used for the tag, the method name prepended to the message.
 *
 * This method automatically evaluates if logging is enabled and calls [block] only
 * if a log message should be generated.
 *
 * Optionally a [topic] can also be specified to allow to only partially enable
 * debug messages across the codebase. The passed [topic] is compared with the
 * currently active [Flog.flogTopics] variable and only if at least 1 topic match
 * is found, [block] will be called and a log message written.
 *
 * @param topic The topic of this message. To specify multiple topics, use the binary
 *  OR operator. Defaults to [Flog.TOPIC_OTHER].
 * @param block The lambda expression to evaluate the message which is appended to the
 *  method name. Is called only if logging is enabled and the topics match. Must return
 *  a [String]. If this argument is omitted, only the calling method name will be used
 *  as the log message.
 */
inline fun flogWarning(topic: FlogTopic = Flog.TOPIC_OTHER, block: () -> String = { "" }) {
    contract {
        callsInPlace(block, InvocationKind.AT_MOST_ONCE)
    }
    if (Flog.checkShouldFlog(topic, Flog.LEVEL_WARNING)) {
        log(Flog.LEVEL_WARNING, block())
    }
}

/**
 * Logs a info message returned by [block] together with the automatically retrieved
 * calling class and method name either to the console or to a log file. The class name
 * is used for the tag, the method name prepended to the message.
 *
 * This method automatically evaluates if logging is enabled and calls [block] only
 * if a log message should be generated.
 *
 * Optionally a [topic] can also be specified to allow to only partially enable
 * debug messages across the codebase. The passed [topic] is compared with the
 * currently active [Flog.flogTopics] variable and only if at least 1 topic match
 * is found, [block] will be called and a log message written.
 *
 * @param topic The topic of this message. To specify multiple topics, use the binary
 *  OR operator. Defaults to [Flog.TOPIC_OTHER].
 * @param block The lambda expression to evaluate the message which is appended to the
 *  method name. Is called only if logging is enabled and the topics match. Must return
 *  a [String]. If this argument is omitted, only the calling method name will be used
 *  as the log message.
 */
inline fun flogInfo(topic: FlogTopic = Flog.TOPIC_OTHER, block: () -> String = { "" }) {
    contract {
        callsInPlace(block, InvocationKind.AT_MOST_ONCE)
    }
    if (Flog.checkShouldFlog(topic, Flog.LEVEL_INFO)) {
        log(Flog.LEVEL_INFO, block())
    }
}

/**
 * Logs a debug message returned by [block] together with the automatically retrieved
 * calling class and method name either to the console or to a log file. The class name
 * is used for the tag, the method name prepended to the message.
 *
 * This method automatically evaluates if logging is enabled and calls [block] only
 * if a log message should be generated.
 *
 * Optionally a [topic] can also be specified to allow to only partially enable
 * debug messages across the codebase. The passed [topic] is compared with the
 * currently active [Flog.flogTopics] variable and only if at least 1 topic match
 * is found, [block] will be called and a log message written.
 *
 * @param topic The topic of this message. To specify multiple topics, use the binary
 *  OR operator. Defaults to [Flog.TOPIC_OTHER].
 * @param block The lambda expression to evaluate the message which is appended to the
 *  method name. Is called only if logging is enabled and the topics match. Must return
 *  a [String]. If this argument is omitted, only the calling method name will be used
 *  as the log message.
 */
inline fun flogDebug(topic: FlogTopic = Flog.TOPIC_OTHER, block: () -> String = { "" }) {
    contract {
        callsInPlace(block, InvocationKind.AT_MOST_ONCE)
    }
    if (Flog.checkShouldFlog(topic, Flog.LEVEL_DEBUG)) {
        log(Flog.LEVEL_DEBUG, block())
    }
}

/**
 * Logs a wtf message returned by [block] together with the automatically retrieved
 * calling class and method name either to the console or to a log file. The class name
 * is used for the tag, the method name prepended to the message.
 *
 * This method automatically evaluates if logging is enabled and calls [block] only
 * if a log message should be generated.
 *
 * Optionally a [topic] can also be specified to allow to only partially enable
 * debug messages across the codebase. The passed [topic] is compared with the
 * currently active [Flog.flogTopics] variable and only if at least 1 topic match
 * is found, [block] will be called and a log message written.
 *
 * @param topic The topic of this message. To specify multiple topics, use the binary
 *  OR operator. Defaults to [Flog.TOPIC_OTHER].
 * @param block The lambda expression to evaluate the message which is appended to the
 *  method name. Is called only if logging is enabled and the topics match. Must return
 *  a [String]. If this argument is omitted, only the calling method name will be used
 *  as the log message.
 */
inline fun flogWtf(topic: FlogTopic = Flog.TOPIC_OTHER, block: () -> String = { "" }) {
    contract {
        callsInPlace(block, InvocationKind.AT_MOST_ONCE)
    }
    if (Flog.checkShouldFlog(topic, Flog.LEVEL_WTF)) {
        log(Flog.LEVEL_WTF, block())
    }
}

/**
 * Helper function to evaluate if a bit flag is set in an integer value.
 *
 * @param flag The flag to check if it is set.
 *
 * @return True if the flag is set, false otherwise.
 */
private infix fun UInt.isSet(flag: UInt): Boolean {
    return (this and flag) == flag
}

/**
 * Main helper object for FlorisBoard logging (=Flog). Manages the enabled
 * state and the active topics. Provides relevant helper functions for the
 * flog methods to properly work.
 *
 * This helper object uses some parts of the Timber library to assist in
 * logging. In particular:
 *  - [createTag] (converted to Kotlin, renamed from "createStackElementTag",
 *     removed manual tagging).
 *  - [getStacktraceElement] (converted to Kotlin, renamed from "getTag",
 *     method now returns stack trace element).
 *  - [log] (only the [OUTPUT_CONSOLE] part, converted to Kotlin).
 * Timber is licensed under the Apache 2.0 license, see the repo here:
 *  https://github.com/JakeWharton/timber
 */
@Suppress("MemberVisibilityCanBePrivate")
object Flog {
    const val TOPIC_NONE: FlogTopic =               UInt.MIN_VALUE
    const val TOPIC_OTHER: FlogTopic =              0x80000000u
    const val TOPIC_ALL: FlogTopic =                UInt.MAX_VALUE

    const val LEVEL_NONE: FlogLevel =               UInt.MIN_VALUE
    const val LEVEL_ERROR: FlogLevel =              0x01u
    const val LEVEL_WARNING: FlogLevel =            0x02u
    const val LEVEL_INFO: FlogLevel =               0x04u
    const val LEVEL_DEBUG: FlogLevel =              0x08u
    const val LEVEL_WTF: FlogLevel =                0x10u
    const val LEVEL_ALL: FlogLevel =                UInt.MAX_VALUE

    const val OUTPUT_CONSOLE: FlogOutput =          0x01u
    const val OUTPUT_FILE: FlogOutput =             0x02u

    /** The relevant call stack element is always on the 4th position, thus 4-1=3. */
    private const val CALL_STACK_INDEX: Int =       3

    /** The maximum log length limit. */
    private const val MAX_LOG_LENGTH: Int =         4000

    private var applicationContext: WeakReference<Context> = WeakReference(null)
    private var isFloggingEnabled: Boolean = false
    private var flogTopics: FlogTopic = TOPIC_NONE
    private var flogLevels: FlogLevel = LEVEL_NONE
    private var flogOutputs: FlogOutput = OUTPUT_CONSOLE

    /**
     * Installs the flog utility for given [applicationContext] and sets the relevant
     * configuration variables based on the given config values.
     *
     * @param context The application context, used for file logging. The context
     *  will be wrapped in a [WeakReference] to prevent memory leaks.
     * @param isFloggingEnabled If logging is enabled. If this value is false, all calls to
     *  the flog methods will be ignored and no logs will be written, regardless of the topics
     *  and levels set.
     * @param flogTopics The enabled topics for this installation. Use [TOPIC_ALL] to enable
     *  all topics. If this value is [TOPIC_NONE], this essentially disables all logging.
     * @param flogLevels The enabled levels for this installation. Use [LEVEL_ALL] to enable
     *  all levels. If this value is [LEVEL_NONE], this essentially disables all logging.
     * @param flogOutputs The enabled outputs for this installation. Use either [OUTPUT_CONSOLE]
     *  for logging to Logcat or [OUTPUT_FILE] to a logging file.
     */
    fun install(
        context: Context,
        isFloggingEnabled: Boolean,
        flogTopics: FlogTopic,
        flogLevels: FlogLevel,
        flogOutputs: FlogOutput
    ) {
        this.applicationContext = WeakReference(context.appContext().value)
        this.isFloggingEnabled = isFloggingEnabled
        this.flogTopics = flogTopics
        this.flogLevels = flogLevels
        this.flogOutputs = flogOutputs
    }

    /**
     * Checks if a log message should be evaluated by checking [isFloggingEnabled] and
     * by matching the given [topic] and [level] values with the configured settings.
     *
     * @param topic The topic(s) to check for.
     * @param level The level(s) to check for.
     *
     * @return True if a log message should be evaluated, false otherwise.
     */
    fun checkShouldFlog(topic: FlogTopic, level: FlogLevel): Boolean {
        return isFloggingEnabled && (flogTopics isSet topic) && (flogLevels isSet level)
    }

    /**
     * Extract the tag which should be used for the message from the `element`.
     */
    private fun createTag(element: StackTraceElement): String {
        var tag = element.className
        tag = tag.substring(tag.lastIndexOf('.') + 1)
        return tag
    }

    private fun createMessage(element: StackTraceElement, msg: String): String {
        return StringBuilder().run {
            append(element.methodName)
            append('(')
            append(')')
            if (msg.isNotBlank()) {
                append(' ')
                append('-')
                append(' ')
                append(msg)
            }
            toString()
        }
    }

    private fun getStacktraceElement(): StackTraceElement {
        val stackTrace = Throwable().stackTrace
        check(stackTrace.size > CALL_STACK_INDEX) {
            "Synthetic stacktrace didn't have enough elements: are you using proguard?"
        }
        return stackTrace[CALL_STACK_INDEX]
    }

    fun log(level: FlogLevel, msg: String) {
        when {
            flogOutputs isSet OUTPUT_CONSOLE -> {
                if (msg.length < MAX_LOG_LENGTH) {
                    androidLog(level, msg)
                } else {
                    // Split by line, then ensure each line can fit into Log's maximum length.
                    var i = 0
                    val length: Int = msg.length
                    while (i < length) {
                        var newline: Int = msg.indexOf('\n', i)
                        newline = if (newline != -1) newline else length
                        do {
                            val end = newline.coerceAtMost(i + MAX_LOG_LENGTH)
                            val part: String = msg.substring(i, end)
                            androidLog(level, part)
                            i = end
                        } while (i < newline)
                        i++
                    }
                }
            }
            flogOutputs isSet OUTPUT_FILE -> {
                fileLog(level, msg)
            }
        }
    }

    private fun androidLog(level: FlogLevel, msg: String) {
        val ste = getStacktraceElement()
        val tag = createTag(ste)
        val message = createMessage(ste, msg)
        when {
            level isSet LEVEL_ERROR ->      Log.e(tag, message)
            level isSet LEVEL_WARNING ->    Log.w(tag, message)
            level isSet LEVEL_INFO ->       Log.i(tag, message)
            level isSet LEVEL_DEBUG ->      Log.d(tag, message)
            level isSet LEVEL_WTF ->        Log.wtf(tag, message)
        }
    }

    private fun fileLog(level: FlogLevel, msg: String) {
        val context = applicationContext.get() ?: return
        // TODO: introduce file logging here for runtime debug logging
    }
}
