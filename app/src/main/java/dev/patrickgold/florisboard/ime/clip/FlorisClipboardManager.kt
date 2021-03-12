package dev.patrickgold.florisboard.ime.clip

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Context.CLIPBOARD_SERVICE
import android.os.Handler
import android.os.Looper
import dev.patrickgold.florisboard.ime.core.FlorisBoard
import dev.patrickgold.florisboard.ime.core.PrefHelper
import dev.patrickgold.florisboard.util.cancelAll
import dev.patrickgold.florisboard.util.postAtScheduledRate
import timber.log.Timber
import java.io.Closeable

/**
 * [FlorisClipboardManager] manages the clipboard and clipboard history.
 *
 * Also just going to document how all the classes here work.
 *
 * [FlorisClipboardManager] handles storage and retrieval of clipboard items. All manipulation of the
 * clipboard goes through here.
 *
 * [ClipboardInputManager] handles the input view and allows for communication between UI and logic
 *
 * [ClipboardHistoryView] is the view representing the clipboard context. Only does some theme stuff.
 *
 * [ClipboardHistoryItemView] is the view representing an item in the clipboard history (either image or text). Only
 * does UI stuff.
 *
 * [ClipboardHistoryItemAdapter] is the recyclerview adapter that backs the clipboard history.
 *
 * [ClipboardPopupManager] handles the popups for each [ClipboardHistoryItemView] (each item has its own popup manager)
 *
 * [ClipboardPopupView] is the view representing a popup displayed when long pressing on a clipboard history item.
 */
class FlorisClipboardManager private constructor() : ClipboardManager.OnPrimaryClipChangedListener, Closeable {

    // Using ArrayDeque because it's "technically" the correct data structure (I think).
    // Newest stored first, oldest stored last.
    private var history: ArrayDeque<TimedClipData> = ArrayDeque()
    private var pins: ArrayDeque<ClipData> = ArrayDeque()
    private var current: ClipData? = null
    private var onPrimaryClipChangedListeners: ArrayList<OnPrimaryClipChangedListener> = arrayListOf()
    private lateinit var systemClipboardManager: ClipboardManager
    private lateinit var handler: Handler
    private lateinit var prefHelper: PrefHelper

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
     * Changes current clipboard item. WITHOUT updating the history.
     */
    fun changeCurrent(newData: ClipData) {
        if (prefHelper.clipboard.enableInternal) {
            current = newData
            if (prefHelper.clipboard.syncToSystem)
                systemClipboardManager.setPrimaryClip(newData)
        }else {
            systemClipboardManager.setPrimaryClip(newData)
        }
    }


    /**
     * Change the current text on clipboard, update history (if enabled).
     */
    fun addNewClip(newData: ClipData) {
        val clipboardPrefs = prefHelper.clipboard

        if (clipboardPrefs.enableHistory) {
            if (clipboardPrefs.limitHistorySize) {
                if (history.size == clipboardPrefs.maxHistorySize) {
                    history.removeLast()
                }
            }

            val timed = TimedClipData(newData, System.currentTimeMillis())
            history.addFirst(timed)
            changeCurrent(newData)
            ClipboardInputManager.getInstance().notifyItemInserted(pins.size)
        }
    }

    /**
     * Wraps some plaintext in a ClipData and calls [addNewClip]
     */
    fun addNewPlaintext(newText: String) {
        val newData = ClipData.newPlainText(newText, newText)
        addNewClip(newData)
    }

    val primaryClip: ClipData?
        get() = current

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
        if(prefHelper.clipboard.enableInternal && prefHelper.clipboard.syncToFloris &&
            systemClipboardManager.primaryClip != primaryClip) {
            systemClipboardManager.primaryClip?.let { addNewClip(it) }
            onPrimaryClipChangedListeners.forEach { it.onPrimaryClipChanged() }
        }
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
        systemClipboardManager.removePrimaryClipChangedListener(this)
        handler.cancelAll()
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
        systemClipboardManager.addPrimaryClipChangedListener(this)

        prefHelper = PrefHelper.getDefaultInstance(context)

        val cleanUpClipboard = Runnable {

            if (!prefHelper.clipboard.cleanUpOld) {
                return@Runnable
            }

            val currentTime = System.currentTimeMillis()
            var numToPop = 0
            val expiryTime = prefHelper.clipboard.cleanUpAfter * 60 * 1000
            for (item in history.asReversed()) {
                Timber.d("${item.timeUTC + expiryTime - currentTime}")
                if (item.timeUTC + expiryTime < currentTime) {
                    numToPop += 1
                } else {
                    break
                }
            }
            for (i in 0 until numToPop) {
                history.removeLast()
                Timber.d("Popped item.")
            }
            ClipboardInputManager.getInstance().notifyItemRangeRemoved(history.size, numToPop)
            Timber.d("Clearing up clipboard")
        }
        FlorisBoard.getInstance().clipInputManager.initClipboard(this.history, this.pins)
        handler = Handler(Looper.getMainLooper())
        prefHelper
        handler.postAtScheduledRate(0, INTERVAL, cleanUpClipboard)
    }


    /**
     * Clears the history with an animation.
     */
    fun clearHistoryWithAnimation() {
        val clipInputManager = FlorisBoard.getInstance().clipInputManager
        val delay = clipInputManager.clearClipboardWithAnimation(pins.size, history.size)

        handler.postDelayed({
            clipInputManager.notifyItemRangeRemoved(pins.size, history.size)
            history.clear()
        }, delay)
    }

    fun pinClip(adapterPos: Int){
        Timber.d("pinning $adapterPos ${pins.size}")
        val clipInputManager = FlorisBoard.getInstance().clipInputManager
        val pin = history.removeAt(adapterPos - pins.size)
        pins.addFirst(pin.data)
        clipInputManager.notifyItemMoved(adapterPos, 0)
        clipInputManager.notifyItemChanged(0)
    }

    fun peekHistoryOrPin(position: Int): ClipData {
        return when {
            position < pins.size -> pins[position]
            else                 -> history[position - pins.size].data
        }
    }


    fun isPinned(position: Int): Boolean {
        return when {
            position < pins.size -> true
            else -> false
        }
    }

    fun unpinClip(adapterPos: Int) {
        val clipInputManager = FlorisBoard.getInstance().clipInputManager
        val item = pins.removeAt(adapterPos)

        val clipboardPrefs = prefHelper.clipboard
        if (clipboardPrefs.limitHistorySize) {
            if (history.size == clipboardPrefs.maxHistorySize) {
                history.removeLast()
            }
        }

        val timed = TimedClipData(item, System.currentTimeMillis())
        history.addFirst(timed)

        clipInputManager.notifyItemMoved(adapterPos, pins.size)
        clipInputManager.notifyItemChanged(pins.size)
    }

}
