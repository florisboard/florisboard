package dev.patrickgold.florisboard.ime.clip

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Context.CLIPBOARD_SERVICE
import dev.patrickgold.florisboard.ime.core.FlorisBoard
import timber.log.Timber
import java.io.Closeable
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

/**
 * [FlorisClipboardManager] manages the clipboard and clipboard history
 */
class FlorisClipboardManager private constructor() : ClipboardManager.OnPrimaryClipChangedListener, Closeable {
    // TODO: use preferences
    private val maxHistorySize: Int = 25
    // 10 seconds
    private val expiryTime: Long = 5 * 60 * 1000

    // Using ArrayDeque because it's "technically" the correct data structure (I think).
    // Newest stored first, oldest stored last.
    private var history: ArrayDeque<TimedClipData> = ArrayDeque(maxHistorySize)
    private var systemClipboardManager: ClipboardManager? = null
    private var onPrimaryClipChangedListeners: ArrayList<OnPrimaryClipChangedListener> = arrayListOf()
    private var cleanUpHandle: ScheduledFuture<*>? = null

    data class TimedClipData(val data: ClipData, val timeUTC: Long)

    interface OnPrimaryClipChangedListener {
        fun onPrimaryClipChanged()
    }


    companion object {
        private var instance: FlorisClipboardManager? = null
        // 1 minute
        private val INTERVAL =  60L

        @Synchronized
        fun getInstance(): FlorisClipboardManager {
            if (instance == null) {
                instance = FlorisClipboardManager()
            }
            return instance!!
        }
    }

    /**
     * Change the current text on clipboard, update history.
     */
    fun changeCurrent(newData: ClipData) {
        if (history.size == maxHistorySize) {
            history.removeLast()
        }
        history.addFirst(TimedClipData(newData, System.currentTimeMillis()))
        Timber.d("changing selection to $newData")
    }

    fun changeCurrentText(newText: String) {
        val newData = ClipData.newPlainText(newText, newText)
        changeCurrent(newData)
    }

    fun getPrimaryClip(): ClipData? {
        return history.getOrNull(0)?.data

    }

    fun peekHistory(index: Int): ClipData? {
        return history.getOrNull(index)?.data
    }

    fun addPrimaryClipChangedListener(listener: OnPrimaryClipChangedListener){
        onPrimaryClipChangedListeners.add(listener)
    }

    fun removePrimaryClipChangedListener(listener: OnPrimaryClipChangedListener){
        onPrimaryClipChangedListeners.remove(listener)
    }

    override fun onPrimaryClipChanged() {
        Timber.d("System clipboard changed")
        systemClipboardManager?.primaryClip?.let { changeCurrent(it) }
        onPrimaryClipChangedListeners.forEach { it.onPrimaryClipChanged() }
        FlorisBoard.getInstance().clipInputManager.notifyClipboardDataChanged()
    }

    fun hasPrimaryClip(): Boolean {
        return this.history.size > 0
    }

    override fun close() {
        cleanUpHandle?.cancel(false)
    }

    fun initialize(context: Context) {
        this.systemClipboardManager = (context.getSystemService(CLIPBOARD_SERVICE) as ClipboardManager)
        systemClipboardManager!!.addPrimaryClipChangedListener(this)

        val scheduler = Executors.newScheduledThreadPool(1)

        val cleanUpClipboard = Runnable {
            if (history.size > 1) {

                val currentTime = System.currentTimeMillis()
                var numToPop = 0
                for (item in history.subList(1, history.size).asReversed()) {
                    Timber.d("${item.timeUTC + expiryTime - currentTime}")
                    if (item.timeUTC + expiryTime < currentTime) {
                        numToPop += 1
                    } else {
                        break
                    }
                }
                for (i in 0 until numToPop) {
                    history.removeLast()
                }
                if (numToPop > 0) {
                    FlorisBoard.getInstance().clipInputManager.notifyClipboardDataChanged()
                }
            }
        }
        val floris = FlorisBoard.getInstance()
        floris.clipInputManager.initClipboard(this.history)
        cleanUpHandle = scheduler.scheduleAtFixedRate(cleanUpClipboard, 0, INTERVAL, TimeUnit.SECONDS)
    }

    fun clearHistory() {
        this.history.clear()
        FlorisBoard.getInstance().clipInputManager.notifyClipboardDataChanged()
    }

}
