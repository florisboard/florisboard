/*
 * Copyright (C) 2022 Patrick Goldinger
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

import android.content.Context
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.*

/**
 * Type alias for a emoji layout data map.
 */
typealias EmojiLayoutDataMap = EnumMap<EmojiCategory, MutableList<EmojiSet>>

private var cachedEmojiLayoutMap: EmojiLayoutDataMap? = null

/**
 * Reads the emoji list at the given [path] and returns an parsed [EmojiLayoutDataMap]. If the
 * given file path does not exist, an empty [EmojiLayoutDataMap] is returned.
 *
 * @param context The initiating view's context.
 * @param path The path to the asset file.
 */
fun parseRawEmojiSpecsFile(
    context: Context, path: String
): EmojiLayoutDataMap {
    cachedEmojiLayoutMap?.let { return it }
    val layouts = EmojiLayoutDataMap(EmojiCategory::class.java)
    for (category in EmojiCategory.values()) {
        layouts[category] = mutableListOf()
    }

    var ec: EmojiCategory? = null
    var emojiEditorList: MutableList<Emoji>? = null

    fun commitEmojiEditorList() {
        emojiEditorList?.let { layouts[ec]!!.add(EmojiSet(it)) }
        emojiEditorList = null
    }

    BufferedReader(InputStreamReader(context.assets.open(path))).useLines { lines ->
        for (line in lines) {
            if (line.startsWith("#")) {
                // Comment line
            } else if (line.startsWith("[")) {
                commitEmojiEditorList()
                ec = EmojiCategory.values().find { it.id == line.slice(1 until (line.length - 1)) }
            } else if (line.trim().isEmpty() || ec == null) {
                // Empty line
                continue
            } else {
                if (!line.startsWith("\t")) {
                    commitEmojiEditorList()
                }
                // Assume it is a data line
                val data = line.split(";")
                if (data.size == 3) {
                    val emoji = Emoji(
                        value = data[0].trim(),
                        name = data[1].trim(),
                        keywords = data[2].split("|").map { it.trim() }
                    )
                    if (emojiEditorList != null) {
                        emojiEditorList!!.add(emoji)
                    } else {
                        emojiEditorList = mutableListOf(emoji)
                    }
                }
            }
        }
        commitEmojiEditorList()
    }

    cachedEmojiLayoutMap = layouts
    return layouts
}
