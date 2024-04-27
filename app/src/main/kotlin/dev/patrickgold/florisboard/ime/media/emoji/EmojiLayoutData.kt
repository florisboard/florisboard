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
import dev.patrickgold.florisboard.lib.FlorisLocale
import dev.patrickgold.florisboard.lib.android.bufferedReader
import dev.patrickgold.florisboard.lib.lowercase
import io.github.reactivecircus.cache4k.Cache
import java.util.*

class EmojiLayoutData() : EnumMap<EmojiCategory, MutableList<EmojiSet>>(EmojiCategory::class.java) {
    companion object {
        val Empty = EmojiLayoutData().also { map ->
            for (category in EmojiCategory.entries) {
                map[category] = mutableListOf()
            }
        }

        private val cache = Cache.Builder().build<String, EmojiLayoutData>()

        suspend fun get(context: Context, path: String): EmojiLayoutData {
            return cache.get(path) {
                loadEmojiDataMap(context, path)
            }
        }

        suspend fun get(context: Context, locale: FlorisLocale): EmojiLayoutData {
            return get(context, resolveEmojiAssetPath(context, locale))
        }

        private fun loadEmojiDataMap(context: Context, path: String): EmojiLayoutData {
            val layouts = EmojiLayoutData()
            for (category in EmojiCategory.entries) {
                layouts[category] = mutableListOf()
            }

            var ec: EmojiCategory? = null
            var emojiEditorList: MutableList<Emoji>? = null

            fun commitEmojiEditorList() {
                emojiEditorList?.let { layouts[ec]!!.add(EmojiSet(it)) }
                emojiEditorList = null
            }

            context.assets.bufferedReader(path).useLines { lines ->
                for (line in lines) {
                    if (line.startsWith("#")) {
                        // Comment line
                    } else if (line.startsWith("[")) {
                        commitEmojiEditorList()
                        ec = EmojiCategory.entries.find { it.id == line.slice(1 until (line.length - 1)) }
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

            return layouts
        }

        /**
         * Resolves the path to the emoji asset file based on the active keyboard subtype and active locale.
         *
         * This method prioritizes usage of cached emoji layout maps when available. If a cached map is found then return,
         * the parseRawEmojiSpecsFile will correctly return the cached data
         *
         * It then attempts to locate a matching emoji asset file within the "ime/media/emoji/" directory in the
         * application's assets based on locale priority:
         * - Primary locale of the active subtype
         * - Secondary locales of the active subtype
         * - Root path ("ime/media/emoji/root.txt") as a fallback
         *
         * For each locale, file matching follows this preference order:
         * - {language}_{country}_{variant}.txt
         * - {language}_{country}.txt
         * - {language}.txt
         *
         * @param context The context used to access application assets.
         * @param locale The locale to resolve for.
         *
         * @return The path to the emoji asset file, or the root path ("ime/media/emoji/root.txt") if no match is found.
         */
        private fun resolveEmojiAssetPath(context: Context, locale: FlorisLocale): String {
            val rootPath = "ime/media/emoji/root.txt"
            val emojiAssets = context.assets.list("ime/media/emoji/")?.toList() ?: return rootPath
            return emojiFilesForLocale(locale, emojiAssets) ?: rootPath
        }

        private fun emojiFilesForLocale(locale: FlorisLocale, assets: List<String>): String? {
            val makePath = { file: String -> "ime/media/emoji/$file" }
            val language = locale.language.lowercase()
            val country = locale.country.takeIf { it.isNotBlank() }
            val variant = locale.variant.takeIf { it.isNotBlank() }
            if (variant != null && country != null) {
                "${language}_${country}_${variant}.txt".takeIf { assets.contains(it) }?.let {
                    return makePath(it)
                }
            }
            if (country != null) {
                "${language}_${country}.txt".takeIf { assets.contains(it) }?.let { return makePath(it) }
            }
            "${language}.txt".takeIf { assets.contains(it) }?.let {
                return makePath(it)
            }
            return null
        }
    }
}
