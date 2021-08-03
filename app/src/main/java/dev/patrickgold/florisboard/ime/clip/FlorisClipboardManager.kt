package dev.patrickgold.florisboard.ime.clip

import android.content.ClipboardManager
import android.content.Context
import android.content.Context.CLIPBOARD_SERVICE
import dev.patrickgold.florisboard.debug.flogDebug
import dev.patrickgold.florisboard.ime.clip.provider.*
import dev.patrickgold.florisboard.ime.core.FlorisBoard
import dev.patrickgold.florisboard.ime.core.Preferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.Closeable
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
class FlorisClipboardManager private constructor() : ClipboardManager.OnPrimaryClipChangedListener, Closeable,
    CoroutineScope by MainScope() {
    private lateinit var pinsDao: PinnedClipboardItemDao

    // Using ArrayDeque because it's "technically" the correct data structure (I think).
    // Newest stored first, oldest stored last.
    private var history: ArrayDeque<TimedClipData> = ArrayDeque()
    private var pins: ArrayDeque<ClipboardItem> = ArrayDeque()
    private var current: ClipboardItem? = null
    private var onPrimaryClipChangedListeners: ArrayList<OnPrimaryClipChangedListener> = arrayListOf()
    private lateinit var systemClipboardManager: ClipboardManager
    private val prefs get() = Preferences.default()
    private lateinit var cleanUpJob: Job

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

        @Synchronized
        fun getInstanceOrNull(): FlorisClipboardManager? = instance

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
        val clipboardPrefs = prefs.clipboard

        if (clipboardPrefs.enableHistory) {
            val clipboardInputManager = ClipboardInputManager.getInstance()

            val historyElement = history.firstOrNull { it.data.type == ItemType.TEXT && it.data.text == newData.text }
            if (historyElement != null) {
                moveToTheBeginning(historyElement, newData, clipboardInputManager)
            } else {
                if (clipboardPrefs.limitHistorySize) {
                    var numRemoved = 0
                    while (history.size >= clipboardPrefs.maxHistorySize) {
                        numRemoved += 1
                        history.removeLast().data.close()
                    }
                    clipboardInputManager.notifyItemRangeRemoved(history.size, numRemoved)
                }

                createAndAddNewTimedClipData(newData)
                clipboardInputManager.notifyItemInserted(pins.size)
            }
        }
    }

    /**
     * Moves a ClipboardItem to the beginning of the history by removing the old one and creating a new one
     */
    private fun moveToTheBeginning(
        historyElement: TimedClipData,
        newData: ClipboardItem,
        clipboardInputManager: ClipboardInputManager
    ) {
        val elementsPosition = history.indexOf(historyElement)
        history.remove(historyElement)

        createAndAddNewTimedClipData(newData)

        clipboardInputManager.notifyItemMoved(elementsPosition, 0)
        clipboardInputManager.notifyItemChanged(0)
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
        if (prefs.clipboard.enableInternal) {
            if (closePrevious) current?.close()
            current = newData
            val isEqual = when (newData.type) {
                ItemType.TEXT -> newData.text == systemClipboardManager.primaryClip?.getItemAt(0)?.text
                ItemType.IMAGE -> newData.uri == systemClipboardManager.primaryClip?.getItemAt(0)?.uri
            }
            if (prefs.clipboard.syncToSystem && !isEqual)
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
        changeCurrent(newData, !prefs.clipboard.enableHistory)
    }

    /**
     * Wraps some plaintext in a ClipData and calls [addNewClip]
     */
    fun addNewPlaintext(newText: String) {
        val newData = ClipboardItem(null, ItemType.TEXT, null, newText, ClipboardItem.TEXT_PLAIN)
        addNewClip(newData)
    }

    val primaryClip: ClipboardItem?
        get() = if (prefs.clipboard.enableInternal) {
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
        val internalPrimaryClip = primaryClip
        val systemPrimaryClip = systemClipboardManager.primaryClip

        if (systemPrimaryClip?.getItemAt(0)?.text == null &&
            systemPrimaryClip?.getItemAt(0)?.uri == null
        ) {
            return
        }

        val isEqual = when (internalPrimaryClip?.type) {
            ItemType.TEXT -> internalPrimaryClip.text == systemPrimaryClip.getItemAt(0)?.text
            ItemType.IMAGE -> internalPrimaryClip.uri == systemPrimaryClip.getItemAt(0)?.uri
            else -> false
        }
        if (prefs.clipboard.enableInternal) {
            // In the event that the internal clipboard is enabled, sync to internal clipboard is enabled
            // and the item is not already in internal clipboard, add it.
            if (prefs.clipboard.syncToFloris && !isEqual) {
                addNewClip(ClipboardItem.fromClipData(systemPrimaryClip, true))
            }
        } else if (prefs.clipboard.enableHistory) {
            // in the event history is enabled, and it should be updated it is updated
            if (shouldUpdateHistory) {
                updateHistory(ClipboardItem.fromClipData(systemPrimaryClip, true))
            } else {
                shouldUpdateHistory = true
            }
        }
    }

    fun hasPrimaryClip(): Boolean {
        return primaryClip != null
    }

    /**
     * Cleans up.
     *
     * Sets [instance] to null for GC. Unregisters the system clipboard listener, cancels clipboard clean ups.
     */
    override fun close() {
        systemClipboardManager.removePrimaryClipChangedListener(this)
        cleanUpJob.cancel()
        instance = null
    }

    /**
     * Initialize the floris clipboard manager. Exists to avoid dependency loop due to reference
     * to [FlorisBoard].
     *
     * Sets up the clipboard cleanup task, links the recycler view in clipInputManager to [history].
     *
     * @param context Required to register as an onPrimaryClipChangedListener of ClipboardManager
     */
    fun initialize(context: Context) {
        try {
            systemClipboardManager = (context.getSystemService(CLIPBOARD_SERVICE) as ClipboardManager)
            systemClipboardManager.addPrimaryClipChangedListener(this)

            val cleanUpClipboard = Runnable {
                if (!prefs.clipboard.cleanUpOld) {
                    return@Runnable
                }

                val currentTime = System.currentTimeMillis()
                var numToPop = 0
                val expiryTime = prefs.clipboard.cleanUpAfter * 60 * 1000
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
            cleanUpJob = launch(Dispatchers.Main) {
                while (true) {
                    cleanUpClipboard.run()
                    delay(INTERVAL)
                }
            }
            launch(Dispatchers.IO) {
                pinsDao = PinnedItemsDatabase.getInstance().clipboardItemDao()
                pinsDao.getAll().toCollection(pins)
                try {
                    FlorisContentProvider.getInstance().initIfNotAlready()
                } catch (e: Exception) {
                    e.fillInStackTrace()
                }
            }
        } catch (e : Exception) {
            e.fillInStackTrace()
        }
    }

    /**
     * Clears the history with an animation.
     */
    fun clearHistoryWithAnimation() {
        val clipInputManager = FlorisBoard.getInstance().clipInputManager
        val animationDelay = clipInputManager.clearClipboardWithAnimation(pins.size, history.size)

        launch(Dispatchers.Main) {
            delay(animationDelay)
            val size = history.size
            for (item in history) {
                item.data.close()
            }
            history.clear()
            clipInputManager.notifyItemRangeRemoved(pins.size, size)
        }
    }

    fun pinClip(adapterPos: Int) {
        val clipInputManager = FlorisBoard.getInstance().clipInputManager
        val pin = history.removeAt(adapterPos - pins.size)
        pins.addFirst(pin.data)
        clipInputManager.notifyItemMoved(adapterPos, 0)
        clipInputManager.notifyItemChanged(0)

        launch(Dispatchers.IO) {
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

        val clipboardPrefs = prefs.clipboard
        if (clipboardPrefs.limitHistorySize) {
            var numRemoved = 0
            while (history.size >= clipboardPrefs.maxHistorySize) {
                numRemoved += 1
                history.removeLast().data.close()
            }
            ClipboardInputManager.getInstance().notifyItemRangeRemoved(history.size, numRemoved)
        }

        createAndAddNewTimedClipData(item)

        clipInputManager.notifyItemMoved(adapterPos, pins.size)
        clipInputManager.notifyItemChanged(pins.size)

        launch(Dispatchers.IO) {
            pinsDao.delete(item)
        }
    }

    /**
     * Creates a new TimedClipData and adds it to the history
     */
    private fun createAndAddNewTimedClipData(newData: ClipboardItem) {
        val timed = TimedClipData(newData, System.currentTimeMillis())
        history.addFirst(timed)
    }

    fun removeClip(pos: Int) {
        when {
            pos < pins.size -> {
                val item = pins.removeAt(pos)
                launch(Dispatchers.IO) {
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
                } else {
                    false
                }
            }
        } == true
    }
}
