package dev.patrickgold.florisboard.ime.clip

import android.content.ClipboardManager
import android.content.Context
import android.content.Context.CLIPBOARD_SERVICE
import android.os.Handler
import android.os.Looper
import dev.patrickgold.florisboard.ime.clip.provider.*
import dev.patrickgold.florisboard.ime.core.FlorisBoard
import dev.patrickgold.florisboard.ime.core.PrefHelper
import dev.patrickgold.florisboard.util.cancelAll
import dev.patrickgold.florisboard.util.postAtScheduledRate
import timber.log.Timber
import java.io.Closeable
import java.util.*
import java.util.concurrent.ExecutorService
import kotlin.collections.ArrayDeque

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

    private lateinit var pinsDao: PinnedClipboardItemDao
    lateinit var executor: ExecutorService

    // Using ArrayDeque because it's "technically" the correct data structure (I think).
    // Newest stored first, oldest stored last.
    private var history: ArrayDeque<TimedClipData> = ArrayDeque()
    private var pins: ArrayDeque<ClipboardItem> = ArrayDeque()
    private var current: ClipboardItem? = null
    private var onPrimaryClipChangedListeners: ArrayList<OnPrimaryClipChangedListener> = arrayListOf()
    private lateinit var systemClipboardManager: ClipboardManager
    private lateinit var handler: Handler
    private lateinit var prefHelper: PrefHelper

    data class TimedClipData(val data: ClipboardItem, val timeUTC: Long)

    interface OnPrimaryClipChangedListener {
        fun onPrimaryClipChanged()
    }

    companion object {
        private var instance: FlorisClipboardManager? = null

        // 1 minute
        private const val INTERVAL = 60 * 1000L

        @Synchronized
        fun getInstance(): FlorisClipboardManager {
            if (instance == null) {
                instance = FlorisClipboardManager()
            }
            return instance!!
        }
        /**
         * Taken from ClipboardDescription.java from the AOSP
         *
         * Helper to compare two MIME types, where one may be a pattern.
         * @param concreteType A fully-specified MIME type.
         * @param desiredType A desired MIME type that may be a pattern such as * / *.
         * @return Returns true if the two MIME types match.
         */
        fun compareMimeTypes(concreteType: String, desiredType: String): Boolean {
            val typeLength = desiredType.length
            if (typeLength == 3 && desiredType == "*/*") {
                return true
            }
            val slashpos = desiredType.indexOf('/')
            if (slashpos > 0) {
                if (typeLength == slashpos + 2 && desiredType[slashpos + 1] == '*') {
                    if (desiredType.regionMatches(0, concreteType, 0, slashpos + 1)) {
                        return true
                    }
                } else if (desiredType == concreteType) {
                    return true
                }
            }
            return false
        }
    }


    /**
     * Adds a new item to the clipboard history (if enabled).
     */
    fun updateHistory(newData: ClipboardItem) {
        val clipboardPrefs = prefHelper.clipboard

        if (clipboardPrefs.enableHistory) {
            if (clipboardPrefs.limitHistorySize) {
                var numRemoved = 0
                while (history.size >= clipboardPrefs.maxHistorySize) {
                    numRemoved += 1
                    history.removeLast().data.close()
                }
                ClipboardInputManager.getInstance().notifyItemRangeRemoved(history.size, numRemoved)
            }


            val timed = TimedClipData(newData, System.currentTimeMillis())
            history.addFirst(timed)
            ClipboardInputManager.getInstance().notifyItemInserted(pins.size)
        }

    }

    /**
     * Used so that [onPrimaryClipChanged] knows whether it was called by [changeCurrent] (and hence shouldn't update
     * history)
     */
    private var shouldUpdateHistory = true

    /**
     * Changes current clipboard item. WITHOUT updating the history.
     */
    fun changeCurrent(newData: ClipboardItem, closePrevious: Boolean) {
        if (prefHelper.clipboard.enableInternal) {
            if (closePrevious) current?.close()
            current = newData
            val isEqual = when (newData.type) {
                ItemType.TEXT -> newData.text == systemClipboardManager.primaryClip?.getItemAt(0)?.text
                ItemType.IMAGE -> newData.uri == systemClipboardManager.primaryClip?.getItemAt(0)?.uri
            }
            if (prefHelper.clipboard.syncToSystem && !isEqual)
                systemClipboardManager.setPrimaryClip(newData.toClipData())
        } else {
            shouldUpdateHistory = false
            systemClipboardManager.setPrimaryClip(newData.toClipData())
        }
        onPrimaryClipChangedListeners.forEach { it.onPrimaryClipChanged() }
    }


    /**
     * Change the current text on clipboard, update history (if enabled).
     *
     */
    fun addNewClip(newData: ClipboardItem) {
        updateHistory(newData)
        // If history is disabled, this new item will replace the old one and hence should be closed.
        changeCurrent(newData, !prefHelper.clipboard.enableHistory)
    }

    /**
     * Wraps some plaintext in a ClipData and calls [addNewClip]
     */
    fun addNewPlaintext(newText: String) {
        val newData = ClipboardItem(null, ItemType.TEXT, null, newText, arrayOf("text/plain"))
        addNewClip(newData)
    }

    val primaryClip: ClipboardItem?
        get() = if (prefHelper.clipboard.enableInternal) {
            current
        } else {
            systemClipboardManager.primaryClip?.let { ClipboardItem.fromClipData(it, false) }
        }

    fun peekHistory(index: Int): ClipboardItem? {
        return history.getOrNull(index)?.data
    }

    fun addPrimaryClipChangedListener(listener: OnPrimaryClipChangedListener) {
        onPrimaryClipChangedListeners.add(listener)
    }

    fun removePrimaryClipChangedListener(listener: OnPrimaryClipChangedListener) {
        onPrimaryClipChangedListeners.remove(listener)
    }

    /**
     * Called by system clipboard when the contents are changed
     */
    override fun onPrimaryClipChanged() {
        // Run on async thread to avoid blocking.
        if (systemClipboardManager.primaryClip?.getItemAt(0)?.text == null &&
            systemClipboardManager.primaryClip?.getItemAt(0)?.uri == null) {
            return
        }

        val isEqual = when (primaryClip?.type) {
            ItemType.TEXT -> primaryClip?.text == systemClipboardManager.primaryClip?.getItemAt(0)?.text
            ItemType.IMAGE -> primaryClip?.uri == systemClipboardManager.primaryClip?.getItemAt(0)?.uri
            null -> false
        }
        systemClipboardManager.primaryClip?.let {
            if (prefHelper.clipboard.enableInternal) {
                // In the event that the internal clipboard is enabled, sync to internal clipboard is enabled
                // and the item is not already in internal clipboard, add it.
                if (prefHelper.clipboard.syncToFloris && !isEqual) {
                    addNewClip(ClipboardItem.fromClipData(it, true))
                }
            } else if (prefHelper.clipboard.enableHistory) {
                // in the event history is enabled, and it should be updated it is updated
                if (shouldUpdateHistory) {
                    updateHistory(ClipboardItem.fromClipData(it, false))
                } else {
                    shouldUpdateHistory = true
                }
            }
        }
    }


    fun hasPrimaryClip(): Boolean {
        return this.primaryClip != null
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
                if (item.timeUTC + expiryTime < currentTime) {
                    numToPop += 1
                } else {
                    break
                }
            }
            for (i in 0 until numToPop) {
                history.removeLast().data.close()
            }
            ClipboardInputManager.getInstance().notifyItemRangeRemoved(pins.size + history.size, numToPop)
        }
        FlorisBoard.getInstance().clipInputManager.initClipboard(this.history, this.pins)
        handler = Handler(Looper.getMainLooper())
        prefHelper
        handler.postAtScheduledRate(0, INTERVAL, cleanUpClipboard)
        executor = FlorisBoard.getInstance().asyncExecutor
        executor.execute {
            pinsDao = PinnedItemsDatabase.getInstance().clipboardItemDao()
            pinsDao.getAll().toCollection(this.pins)
            FlorisContentProvider.getInstance().initIfNotAlready()
        }
    }


    /**
     * Clears the history with an animation.
     */
    fun clearHistoryWithAnimation() {
        val clipInputManager = FlorisBoard.getInstance().clipInputManager
        val delay = clipInputManager.clearClipboardWithAnimation(pins.size, history.size)

        handler.postDelayed({
            val size = history.size
            for (item in history) {
                item.data.close()
            }
            history.clear()
            clipInputManager.notifyItemRangeRemoved(pins.size, size)
        }, delay)
    }

    fun pinClip(adapterPos: Int) {
        val clipInputManager = FlorisBoard.getInstance().clipInputManager
        val pin = history.removeAt(adapterPos - pins.size)
        pins.addFirst(pin.data)
        clipInputManager.notifyItemMoved(adapterPos, 0)
        clipInputManager.notifyItemChanged(0)

        executor.execute {
            val uid = pinsDao.insert(pin.data)
            pin.data.uid = uid
        }
    }

    /**
     * Get the item at a particular [adapterPos] (i.e the position the item is displayed at.)
     */
    fun peekHistoryOrPin(adapterPos: Int): ClipboardItem {
        return when {
            adapterPos < pins.size -> pins[adapterPos]
            else -> history[adapterPos - pins.size].data
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
            var numRemoved = 0
            while (history.size >= clipboardPrefs.maxHistorySize) {
                numRemoved += 1
                history.removeLast().data.close()
            }
            ClipboardInputManager.getInstance().notifyItemRangeRemoved(history.size, numRemoved)
        }

        val timed = TimedClipData(item, System.currentTimeMillis())
        history.addFirst(timed)

        clipInputManager.notifyItemMoved(adapterPos, pins.size)
        clipInputManager.notifyItemChanged(pins.size)

        executor.execute {
            pinsDao.delete(item)
        }
    }

    fun removeClip(pos: Int) {
        when {
            pos < pins.size -> {
                val item = pins.removeAt(pos)
                executor.execute {
                    Timber.d("removing pin")
                    pinsDao.delete(item)
                }
                item.close()
            }
            else -> {
                history.removeAt(pos - pins.size).data.close()
            }
        }
        val clipboardInputManager = ClipboardInputManager.getInstance()
        clipboardInputManager.notifyItemRemoved(pos)
    }


    fun pasteItem(pos: Int) {
        val item = peekHistoryOrPin(pos)
        FlorisBoard.getInstance().activeEditorInstance.commitClipboardItem(item)
    }

    /**
     * Returns true if the editor can accept the clip item, else false.
     */
    fun canBePasted(clipItem: ClipboardItem?): Boolean {
        if (clipItem == null) return false

        return clipItem.mimeTypes.contains("text/plain") || FlorisBoard.getInstance().activeEditorInstance.contentMimeTypes?.any { editorType ->
            clipItem.mimeTypes.any { clipType ->
                if (editorType != null) {
                    compareMimeTypes(clipType, editorType)
                }else { false }
            }
        } == true
    }


}
