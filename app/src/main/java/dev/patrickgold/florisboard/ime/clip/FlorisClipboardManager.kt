package dev.patrickgold.florisboard.ime.clip

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Context.CLIPBOARD_SERVICE
import android.os.Handler
import android.os.Looper
import dev.patrickgold.florisboard.ime.core.FlorisBoard
import dev.patrickgold.florisboard.util.cancelAll
import dev.patrickgold.florisboard.util.postAtScheduledRate
import timber.log.Timber
import java.io.Closeable

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
    private var current: ClipData? = null
    private var systemClipboardManager: ClipboardManager? = null
    private var onPrimaryClipChangedListeners: ArrayList<OnPrimaryClipChangedListener> = arrayListOf()
    private var handler: Handler? = null

    data class TimedClipData(val data: ClipData, val timeUTC: Long)

    interface OnPrimaryClipChangedListener {
        fun onPrimaryClipChanged()
    }

    companion object {
        private var instance: FlorisClipboardManager? = null
        // 1 minute
        private const val INTERVAL =  60 * 1000L

        @Synchronized
        fun getInstance(): FlorisClipboardManager {
            if (instance == null) {
                instance = FlorisClipboardManager()
            }
            return instance!!
        }
    }

    /**
     * changes current
     */
    fun changeCurrent(newData: ClipData) {
        current = newData
    }


    /**
     * Change the current text on clipboard, update history.
     */
    fun addNewClip(newData: ClipData) {
        if (history.size == maxHistorySize) {
            history.removeLast()
        }
        val timed = TimedClipData(newData, System.currentTimeMillis())
        history.addFirst(timed)
        changeCurrent(newData)
        FlorisBoard.getInstance().clipInputManager.adapter?.notifyItemInserted(0)
    }

    /**
     * Wraps some plaintext in a ClipData and calls [addNewClip]
     */
    fun addNewPlaintext(newText: String) {
        val newData = ClipData.newPlainText(newText, newText)
        addNewClip(newData)
    }

    fun getPrimaryClip(): ClipData? {
        return current

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
        systemClipboardManager?.primaryClip?.let { addNewClip(it) }
        onPrimaryClipChangedListeners.forEach { it.onPrimaryClipChanged() }
    }

    fun hasPrimaryClip(): Boolean {
        return this.current != null
    }

    /**
     * Cleans up.
     *
     * Sets [instance] to null for GC. Unregisters the system clipboard listener, cancels clipboard clean ups.
     */
    override fun close() {
        systemClipboardManager?.removePrimaryClipChangedListener(this)
        handler?.cancelAll()
        instance = null
    }

    /**
     * Initialize the floris clipboard manager. Exists to avoid dependency loop due to reference
     * to [FlorisBoard.context]
     *
     * Sets up the clipboard cleanup task, links the recycler view in clipInputManager to [history].
     *
     * @param context Required to register as an onPrimaryClipChangedListener of ClipboardManager
     */
    fun initialize(context: Context) {
        this.systemClipboardManager = (context.getSystemService(CLIPBOARD_SERVICE) as ClipboardManager)
        systemClipboardManager!!.addPrimaryClipChangedListener(this)

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
                    FlorisBoard.getInstance().clipInputManager.adapter?.notifyItemRemoved(history.size - 1)
                }
            }
            Timber.d("Clearing up clipboard")
        }
        FlorisBoard.getInstance().clipInputManager.initClipboard(this.history)
        handler = Handler(Looper.getMainLooper())
        handler?.postAtScheduledRate(0, INTERVAL, cleanUpClipboard)
    }


    /**
     * Clears the history with an animation.
     */
    fun clearHistoryWithAnimation() {
        val clipInputManager = FlorisBoard.getInstance().clipInputManager
        val delay = clipInputManager.clearClipboardWithAnimation(history.size)

        handler?.postDelayed({
            history.clear()
            clipInputManager.adapter?.notifyDataSetChanged()
        }, delay)
    }

}
