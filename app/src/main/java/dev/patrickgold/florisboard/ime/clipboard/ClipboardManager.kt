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

package dev.patrickgold.florisboard.ime.clipboard

import android.content.ClipboardManager
import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import dev.patrickgold.florisboard.FlorisImeService
import dev.patrickgold.florisboard.app.prefs.florisPreferenceModel
import dev.patrickgold.florisboard.appContext
import dev.patrickgold.florisboard.common.android.AndroidClipboardManager
import dev.patrickgold.florisboard.common.android.AndroidClipboardManager_OnPrimaryClipChangedListener
import dev.patrickgold.florisboard.common.android.systemService
import dev.patrickgold.florisboard.debug.flogError
import dev.patrickgold.florisboard.ime.clipboard.provider.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.Closeable
import kotlin.collections.ArrayDeque

/**
 * [ClipboardManager] manages the clipboard and clipboard history.
 *
 * Also just going to document how all the classes here work.
 *
 * [ClipboardManager] handles storage and retrieval of clipboard items. All manipulation of the
 * clipboard goes through here.
 */
class ClipboardManager(
    context: Context,
) : AndroidClipboardManager_OnPrimaryClipChangedListener, Closeable, CoroutineScope by MainScope() {

    companion object {
        // 1 minute
        private const val INTERVAL = 60 * 1000L

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

    private val prefs by florisPreferenceModel()
    private val appContext by context.appContext()
    private val systemClipboardManager = context.systemService(AndroidClipboardManager::class)

    // Using ArrayDeque because it's "technically" the correct data structure (I think).
    // Newest stored first, oldest stored last.
    private val history: ArrayDeque<TimedClipData> = ArrayDeque()
    private val pins: ArrayDeque<ClipboardItem> = ArrayDeque()
    private lateinit var cleanUpJob: Job
    private lateinit var pinsDao: PinnedClipboardItemDao

    private val _primaryClip = MutableLiveData<ClipboardItem?>(null)
    val primaryClip: LiveData<ClipboardItem?> = _primaryClip

    init {
        try {
            systemClipboardManager.addPrimaryClipChangedListener(this)

            val cleanUpClipboard = Runnable {
                if (!prefs.clipboard.cleanUpOld.get()) {
                    return@Runnable
                }

                val currentTime = System.currentTimeMillis()
                var numToPop = 0
                val expiryTime = prefs.clipboard.cleanUpAfter.get() * 60 * 1000
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
            }
            cleanUpJob = launch(Dispatchers.Main) {
                while (true) {
                    cleanUpClipboard.run()
                    delay(INTERVAL)
                }
            }
            launch(Dispatchers.IO) {
                pinsDao = PinnedItemsDatabase.new(context).clipboardItemDao()
                pinsDao.getAll().toCollection(pins)
                try {
                    FlorisContentProvider.getInstance().init()
                } catch (e: Exception) {
                    flogError { e.toString() }
                }
            }
        } catch (e : Exception) {
            flogError { e.toString() }
        }
    }

    /**
     * Adds a new item to the clipboard history (if enabled).
     */
    fun updateHistory(newData: ClipboardItem) {
        val clipboardPrefs = prefs.clipboard

        if (clipboardPrefs.enableHistory.get()) {
            val historyElement = history.firstOrNull { it.data.type == ItemType.TEXT && it.data.text == newData.text }
            if (historyElement != null) {
                moveToTheBeginning(historyElement, newData)
            } else {
                if (clipboardPrefs.limitHistorySize.get()) {
                    var numRemoved = 0
                    while (history.size >= clipboardPrefs.maxHistorySize.get()) {
                        numRemoved += 1
                        history.removeLast().data.close()
                    }
                }

                createAndAddNewTimedClipData(newData)
            }
        }
    }

    /**
     * Moves a ClipboardItem to the beginning of the history by removing the old one and creating a new one
     */
    private fun moveToTheBeginning(
        historyElement: TimedClipData,
        newData: ClipboardItem,
    ) {
        val elementsPosition = history.indexOf(historyElement)
        history.remove(historyElement)

        createAndAddNewTimedClipData(newData)
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
        if (prefs.clipboard.useInternalClipboard.get()) {
            if (closePrevious) _primaryClip.value?.close()
            _primaryClip.postValue(newData)
            val isEqual = when (newData.type) {
                ItemType.TEXT -> newData.text == systemClipboardManager.primaryClip?.getItemAt(0)?.text
                ItemType.IMAGE -> newData.uri == systemClipboardManager.primaryClip?.getItemAt(0)?.uri
            }
            if (prefs.clipboard.syncToSystem.get() && !isEqual)
                systemClipboardManager.setPrimaryClip(newData.toClipData(appContext))
        } else {
            shouldUpdateHistory = false
            systemClipboardManager.setPrimaryClip(newData.toClipData(appContext))
        }
    }

    /**
     * Change the current text on clipboard, update history (if enabled).
     */
    fun addNewClip(newData: ClipboardItem) {
        updateHistory(newData)
        // If history is disabled, this new item will replace the old one and hence should be closed.
        changeCurrent(newData, !prefs.clipboard.enableHistory.get())
    }

    /**
     * Wraps some plaintext in a ClipData and calls [addNewClip]
     */
    fun addNewPlaintext(newText: String) {
        val newData = ClipboardItem(null, ItemType.TEXT, null, newText, ClipboardItem.TEXT_PLAIN)
        addNewClip(newData)
    }

    fun peekHistory(index: Int): ClipboardItem? {
        return history.getOrNull(index)?.data
    }

    /**
     * Called by system clipboard when the contents are changed
     */
    override fun onPrimaryClipChanged() {
        // Run on async thread to avoid blocking.
        val internalPrimaryClip = _primaryClip.value
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
        if (prefs.clipboard.useInternalClipboard.get()) {
            // In the event that the internal clipboard is enabled, sync to internal clipboard is enabled
            // and the item is not already in internal clipboard, add it.
            if (prefs.clipboard.syncToFloris.get() && !isEqual) {
                addNewClip(ClipboardItem.fromClipData(appContext, systemPrimaryClip, true))
            }
        } else if (prefs.clipboard.enableHistory.get()) {
            // in the event history is enabled, and it should be updated it is updated
            if (shouldUpdateHistory) {
                updateHistory(ClipboardItem.fromClipData(appContext, systemPrimaryClip, true))
            } else {
                shouldUpdateHistory = true
            }
        }
    }

    fun hasPrimaryClip(): Boolean {
        return primaryClip.value != null
    }

    /**
     * Cleans up.
     *
     * Unregisters the system clipboard listener, cancels clipboard clean ups.
     */
    override fun close() {
        systemClipboardManager.removePrimaryClipChangedListener(this)
        cleanUpJob.cancel()
    }

    /**
     * Clears the history with an animation.
     */
    fun clearHistoryWithAnimation() {
        launch(Dispatchers.Main) {
            delay(200)
            val size = history.size
            for (item in history) {
                item.data.close()
            }
            history.clear()
        }
    }

    fun pinClip(adapterPos: Int) {
        val pin = history.removeAt(adapterPos - pins.size)
        pins.addFirst(pin.data)

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
        val item = pins.removeAt(adapterPos)

        val clipboardPrefs = prefs.clipboard
        if (clipboardPrefs.limitHistorySize.get()) {
            var numRemoved = 0
            while (history.size >= clipboardPrefs.maxHistorySize.get()) {
                numRemoved += 1
                history.removeLast().data.close()
            }
        }

        createAndAddNewTimedClipData(item)

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
    }

    fun pasteItem(pos: Int) {
        val activeEditorInstance = FlorisImeService.activeEditorInstance() ?: return
        val item = peekHistoryOrPin(pos)
        activeEditorInstance.commitClipboardItem(item)
    }

    /**
     * Returns true if the editor can accept the clip item, else false.
     */
    fun canBePasted(clipItem: ClipboardItem?): Boolean {
        if (clipItem == null) return false
        val activeEditorInstance = FlorisImeService.activeEditorInstance() ?: return false

        return clipItem.mimeTypes.contains("text/plain") || activeEditorInstance.contentMimeTypes?.any { editorType ->
            clipItem.mimeTypes.any { clipType ->
                if (editorType != null) {
                    compareMimeTypes(clipType, editorType)
                } else {
                    false
                }
            }
        } == true
    }

    data class TimedClipData(val data: ClipboardItem, val timeUTC: Long)
}
