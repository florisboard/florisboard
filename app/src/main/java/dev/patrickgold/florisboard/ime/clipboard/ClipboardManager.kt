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
import dev.patrickgold.florisboard.common.android.setOrClearPrimaryClip
import dev.patrickgold.florisboard.common.android.systemService
import dev.patrickgold.florisboard.debug.flogError
import dev.patrickgold.florisboard.debug.flogInfo
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
    private var cleanUpJob: Job
    private lateinit var clipHistoryDao: ClipboardHistoryDao

    private val _primaryClip = MutableLiveData<ClipboardItem?>(null)
    val primaryClip: LiveData<ClipboardItem?> get() = _primaryClip

    init {
        onPrimaryClipChanged()
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
                history.removeLast().data.close(appContext)
            }
        }
        cleanUpJob = launch(Dispatchers.Main) {
            while (true) {
                cleanUpClipboard.run()
                delay(INTERVAL)
            }
        }
        launch(Dispatchers.IO) {
            clipHistoryDao = ClipboardHistoryDatabase.new(context).clipboardItemDao()
            clipHistoryDao.getAll().toCollection(pins)
            try {
                FlorisContentProvider.getInstance().init()
            } catch (e: Exception) {
                flogError { e.toString() }
            }
        }
    }

    fun primaryClip(): ClipboardItem? {
        return primaryClip.value
    }

    fun hasPrimaryClip(): Boolean {
        return primaryClip.value != null
    }

    /**
     * Sets the current primary clip without updating the internal clipboard history.
     */
    fun setPrimaryClip(item: ClipboardItem?) {
        _primaryClip.postValue(item)
        if (prefs.clipboard.useInternalClipboard.get()) {
            // Purposely do not sync to system if disabled in prefs
            if (prefs.clipboard.syncToSystem.get()) {
                systemClipboardManager.setOrClearPrimaryClip(item?.toClipData(appContext))
            }
        } else {
            systemClipboardManager.setOrClearPrimaryClip(item?.toClipData(appContext))
        }
    }

    /**
     * Called by system clipboard when the system primary clip has changed.
     */
    override fun onPrimaryClipChanged() {
        if (!prefs.clipboard.useInternalClipboard.get() || prefs.clipboard.syncToFloris.get()) {
            val systemPrimaryClip = systemClipboardManager.primaryClip
            val internalPrimaryClip = primaryClip.value

            if (systemPrimaryClip == null) {
                _primaryClip.postValue(null)
                return
            }

            if (systemPrimaryClip.getItemAt(0).let { it.text == null && it.uri == null }) {
                _primaryClip.postValue(null)
                return
            }

            val isEqual = internalPrimaryClip?.isEqualTo(systemPrimaryClip) == true
            if (!isEqual) {
                _primaryClip.postValue(ClipboardItem.fromClipData(appContext, systemPrimaryClip, cloneUri = true))
                // TODO: update history
            }
        }
    }

    /**
     * Change the current text on clipboard, update history (if enabled).
     */
    fun addNewClip(item: ClipboardItem) {
        updateHistory(item)
        setPrimaryClip(item)
    }

    /**
     * Wraps some plaintext in a ClipData and calls [addNewClip]
     */
    fun addNewPlaintext(newText: String) {
        val newData = ClipboardItem.text(newText)
        addNewClip(newData)
    }

    /**
     * Adds a new item to the clipboard history (if enabled).
     */
    fun updateHistory(newData: ClipboardItem) {
        if (prefs.clipboard.enableHistory.get()) {
            val historyElement = history.firstOrNull { it.data.type == ItemType.TEXT && it.data.text == newData.text }
            if (historyElement != null) {
                moveToTheBeginning(historyElement, newData)
            } else {
                if (prefs.clipboard.limitHistorySize.get()) {
                    var numRemoved = 0
                    while (history.size >= prefs.clipboard.maxHistorySize.get()) {
                        numRemoved += 1
                        history.removeLast().data.close(appContext)
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

    fun peekHistory(index: Int): ClipboardItem? {
        return history.getOrNull(index)?.data
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
                item.data.close(appContext)
            }
            history.clear()
        }
    }

    fun pinClip(adapterPos: Int) {
        val pin = history.removeAt(adapterPos - pins.size)
        pins.addFirst(pin.data)

        launch(Dispatchers.IO) {
            val uid = clipHistoryDao.insert(pin.data)
            pin.data.id = uid
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
                history.removeLast().data.close(appContext)
            }
        }

        createAndAddNewTimedClipData(item)

        launch(Dispatchers.IO) {
            clipHistoryDao.delete(item)
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
                    clipHistoryDao.delete(item)
                }
                item.close(appContext)
            }
            else -> {
                history.removeAt(pos - pins.size).data.close(appContext)
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
