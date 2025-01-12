/*
 * Copyright (C) 2024-2025 The FlorisBoard Contributors
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

package dev.patrickgold.florisboard.ime.media.emoji

import dev.patrickgold.florisboard.app.AppPrefs
import dev.patrickgold.florisboard.lib.devtools.flogError
import dev.patrickgold.jetpref.datastore.model.PreferenceSerializer
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class EmojiHistory(
    val pinned: List<@Serializable(with = Emoji.ValueOnlySerializer::class) Emoji>,
    val recent: List<@Serializable(with = Emoji.ValueOnlySerializer::class) Emoji>,
) {
    fun edit(): Editor {
        return Editor(pinned.toMutableList(), recent.toMutableList())
    }

    data class Editor(
        val pinned: MutableList<Emoji>,
        val recent: MutableList<Emoji>,
    ) {
        fun build(): EmojiHistory {
            return EmojiHistory(pinned.toList(), recent.toList())
        }
    }

    enum class UpdateStrategy(val isAutomatic: Boolean, val isPrepend: Boolean) {
        AUTO_SORT_PREPEND(isAutomatic = true, isPrepend = true),
        AUTO_SORT_APPEND(isAutomatic = true, isPrepend = false),
        MANUAL_SORT_PREPEND(isAutomatic = false, isPrepend = true),
        MANUAL_SORT_APPEND(isAutomatic = false, isPrepend = false);
    }

    object Serializer : PreferenceSerializer<EmojiHistory> {
        override fun serialize(value: EmojiHistory): String {
            return Json.encodeToString(value)
        }

        override fun deserialize(value: String): EmojiHistory {
            try {
                return Json.decodeFromString(value)
            } catch (e: Exception) {
                flogError { "Failed to deserialize EmojiHistory: $e" }
                return Empty
            }
        }
    }

    companion object {
        val Empty = EmojiHistory(emptyList(), emptyList())

        @Suppress("ConstPropertyName")
        const val MaxSizeUnlimited: Int = 0
    }
}

object EmojiHistoryHelper {
    private var emojiGuard = Mutex(locked = false)

    suspend fun markEmojiUsed(prefs: AppPrefs, emoji: Emoji) = emojiGuard.withLock {
        if (!prefs.emoji.historyEnabled.get()) {
            return
        }

        val dataMut = prefs.emoji.historyData.get().edit()
        val pinnedUS = prefs.emoji.historyPinnedUpdateStrategy.get()
        val recentUS = prefs.emoji.historyRecentUpdateStrategy.get()
        val pinnedMaxSize = prefs.emoji.historyPinnedMaxSize.get().let { maxSize ->
            if (maxSize == EmojiHistory.MaxSizeUnlimited) Int.MAX_VALUE else maxSize
        }
        val recentMaxSize = prefs.emoji.historyRecentMaxSize.get().let { maxSize ->
            if (maxSize == EmojiHistory.MaxSizeUnlimited) Int.MAX_VALUE else maxSize
        }

        val pinnedIndex = dataMut.pinned.indexOf(emoji)
        if (pinnedIndex != -1) {
            if (pinnedUS.isAutomatic) {
                dataMut.pinned.removeAt(pinnedIndex)
                dataMut.pinned.addWithStrategy(pinnedUS, emoji)
            } else {
                // manual sort, keep item in place
            }
        } else {
            val recentIndex = dataMut.recent.indexOf(emoji)
            if (recentIndex != -1) {
                if (recentUS.isAutomatic) {
                    dataMut.recent.removeAt(recentIndex)
                    dataMut.recent.addWithStrategy(recentUS, emoji)
                } else {
                    // manual sort, keep item in place
                }
            } else {
                dataMut.recent.addWithStrategy(recentUS, emoji)
            }
        }

        prefs.emoji.historyData.set(
            EmojiHistory(
                pinned = dataMut.pinned.takeWithStrategy(pinnedUS, pinnedMaxSize),
                recent = dataMut.recent.takeWithStrategy(recentUS, recentMaxSize),
            )
        )
    }

    suspend fun pinEmoji(prefs: AppPrefs, emoji: Emoji) = emojiGuard.withLock {
        if (!prefs.emoji.historyEnabled.get()) {
            return
        }

        val dataMut = prefs.emoji.historyData.get().edit()
        val pinnedUS = prefs.emoji.historyPinnedUpdateStrategy.get()

        val recentIndex = dataMut.recent.indexOf(emoji)
        if (recentIndex != -1) {
            dataMut.recent.removeAt(recentIndex)
            dataMut.pinned.addWithStrategy(pinnedUS, emoji)
        }

        prefs.emoji.historyData.set(dataMut.build())
    }

    suspend fun unpinEmoji(prefs: AppPrefs, emoji: Emoji) = emojiGuard.withLock {
        if (!prefs.emoji.historyEnabled.get()) {
            return
        }

        val dataMut = prefs.emoji.historyData.get().edit()
        val recentUS = prefs.emoji.historyRecentUpdateStrategy.get()

        val pinnedIndex = dataMut.pinned.indexOf(emoji)
        if (pinnedIndex != -1) {
            dataMut.pinned.removeAt(pinnedIndex)
            dataMut.recent.addWithStrategy(recentUS, emoji)
        }

        prefs.emoji.historyData.set(dataMut.build())
    }

    suspend fun moveEmoji(prefs: AppPrefs, emoji: Emoji, offset: Int) = emojiGuard.withLock {
        if (!prefs.emoji.historyEnabled.get() || offset == 0) {
            return
        }

        val dataMut = prefs.emoji.historyData.get().edit()

        val pinnedIndex = dataMut.pinned.indexOf(emoji)
        if (pinnedIndex != -1) {
            dataMut.pinned.move(pinnedIndex, offset)
        } else {
            val recentIndex = dataMut.recent.indexOf(emoji)
            if (recentIndex != -1) {
                dataMut.recent.move(recentIndex, offset)
            }
        }

        prefs.emoji.historyData.set(dataMut.build())
    }

    suspend fun removeEmoji(prefs: AppPrefs, emoji: Emoji) = emojiGuard.withLock {
        if (!prefs.emoji.historyEnabled.get()) {
            return
        }

        val dataMut = prefs.emoji.historyData.get().edit()

        val pinnedIndex = dataMut.pinned.indexOf(emoji)
        if (pinnedIndex != -1) {
            dataMut.pinned.removeAt(pinnedIndex)
        } else {
            val recentIndex = dataMut.recent.indexOf(emoji)
            if (recentIndex != -1) {
                dataMut.recent.removeAt(recentIndex)
            }
        }

        prefs.emoji.historyData.set(dataMut.build())
    }

    suspend fun deleteHistory(prefs: AppPrefs) = emojiGuard.withLock {
        if (!prefs.emoji.historyEnabled.get()) {
            return
        }
        val dataMut = prefs.emoji.historyData.get().edit()
        prefs.emoji.historyData.set(EmojiHistory(pinned = dataMut.pinned, listOf()))
    }

    suspend fun deletePinned(prefs: AppPrefs) = emojiGuard.withLock {
        if (!prefs.emoji.historyEnabled.get()) {
            return
        }
        val dataMut = prefs.emoji.historyData.get().edit()
        prefs.emoji.historyData.set(EmojiHistory(pinned = listOf(), dataMut.recent))
    }

    private fun MutableList<Emoji>.addWithStrategy(strategy: EmojiHistory.UpdateStrategy, emoji: Emoji) {
        if (strategy.isPrepend) {
            add(0, emoji)
        } else {
            add(emoji)
        }
    }

    private fun MutableList<Emoji>.takeWithStrategy(
        strategy: EmojiHistory.UpdateStrategy,
        n: Int,
    ): List<Emoji> {
        return if (strategy.isPrepend) {
            take(n)
        } else {
            takeLast(n)
        }
    }

    private fun MutableList<Emoji>.move(itemIndex: Int, offset: Int) {
        val newIndex = (itemIndex + offset).coerceIn(0..<size)
        val item = removeAt(itemIndex)
        if (newIndex == size) {
            add(item)
        } else {
            add(newIndex, item)
        }
    }
}
