/*
 * Copyright (C) 2020 Patrick Goldinger
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
import android.graphics.Paint
import android.graphics.Typeface
import androidx.core.graphics.PaintCompat
import timber.log.Timber
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.util.*

private const val GROUP_IDENTIFIER          = "# group: "
private const val SUBGROUP_IDENTIFIER       = "# subgroup: "
private const val EOF_IDENTIFIER            = "#EOF"

private const val FULLY_QUALIFIED           = "fully-qualified"
private const val MINIMALLY_QUALIFIED       = "minimally-qualified"
private const val UNQUALIFIED               = "unqualified"

private const val LIGHT_SKIN_TONE           = "1F3FB"
private const val MEDIUM_LIGHT_SKIN_TONE    = "1F3FC"
private const val MEDIUM_SKIN_TONE          = "1F3FD"
private const val MEDIUM_DARK_SKIN_TONE     = "1F3FE"
private const val DARK_SKIN_TONE            = "1F3FF"

private const val RED_HAIR                  = "1F9B0"
private const val CURLY_HAIR                = "1F9B1"
private const val WHITE_HAIR                = "1F9B2"
private const val BALD                      = "1F9B3"

private val NAME_JUNK_SPLIT_REGEX   = "E([0-9]+)\\.([0-9]+)\\s+".toRegex()
private val CODE_POINT_REGEX        = "([a-fA-F0-9]+)\\s".toRegex()

/**
 * Type alias for a emoji layout data map.
 */
typealias EmojiLayoutDataMap = EnumMap<EmojiCategory, MutableList<EmojiKeyData>>

/**
 * Converts a mutable list of Strings (which represent a number) to a list of Ints.
 *
 * @param list The input list.
 * @return The converted output list.
 */
private fun listStringToListInt(list: List<String>): List<Int> {
    val ret: MutableList<Int> = mutableListOf()
    for (num in list) {
        try {
            ret.add(num.toInt(16))
        } catch (e: Exception) {}
    }
    return ret.toList()
}

private var cachedEmojiLayoutMap: EmojiLayoutDataMap? = null

/**
 * Reads the emoji list at the given [path] and returns an parsed [EmojiLayoutDataMap]. If the
 * given file path does not exist, an empty [EmojiLayoutDataMap] is returned.
 *
 * TODO: Fix potential bug where the base version of a emoji is missing and therefore the emoji
 *       before has like 25 popups...
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
    var reader: BufferedReader? = null
    try {
        reader = BufferedReader(
            InputStreamReader(context.assets.open(path))
        )
        val paint = Paint().apply {
            typeface = Typeface.DEFAULT
        }
        var ec: EmojiCategory? = null
        var lastKey: EmojiKeyData? = null
        var skipUntilNextGroup = true
        for (line in reader.readLines()) {
            if (line.startsWith("#")) {
                // Comment line
                if (line.startsWith(GROUP_IDENTIFIER, true)) {
                    // A new group begins
                    val rawGroupName = line.trim().substring(GROUP_IDENTIFIER.length)
                    if (rawGroupName.toUpperCase(Locale.ENGLISH) == "COMPONENT") {
                        skipUntilNextGroup = true
                        continue
                    } else {
                        skipUntilNextGroup = false
                        ec = EmojiCategory.fromString(rawGroupName)
                    }
                } else if (line.startsWith(SUBGROUP_IDENTIFIER, true)) {
                    // A new subgroup begins
                } else if (line.startsWith(EOF_IDENTIFIER, true)) {
                    break
                }
            } else if (line.trim().isEmpty() || skipUntilNextGroup || ec == null) {
                // Empty line or skipUntilNextGroup true
                continue
            } else {
                // Assume it is a data line
                val data = line.split("#")
                if (data.size == 2) {
                    val data2 = data[0].split(";")
                    if (data2.size == 2 && data[1].contains(NAME_JUNK_SPLIT_REGEX)) {
                        val dataC = data2[0].trim()
                        val dataQ = data2[1].trim()
                        val dataN = data[1].split(NAME_JUNK_SPLIT_REGEX)[1]
                        if (dataQ.toLowerCase(Locale.ENGLISH) == FULLY_QUALIFIED) {
                            // Only fully-qualified emojis are accepted
                            val dataCPs = dataC.split(" ")
                            val key = EmojiKeyData(listStringToListInt(dataCPs), dataN)
                            // Check if system font can render the emoji, else skip it as it makes
                            //  no include it in the emoji keyboard as it will be the default
                            //  glyph not found box.
                            if (PaintCompat.hasGlyph(paint, key.getCodePointsAsString())) {
                                if (dataCPs.size > 1) {
                                    // Emoji COULD be an extension
                                    when (dataCPs[1]) {
                                        LIGHT_SKIN_TONE,
                                        MEDIUM_LIGHT_SKIN_TONE,
                                        MEDIUM_SKIN_TONE,
                                        MEDIUM_DARK_SKIN_TONE,
                                        DARK_SKIN_TONE,
                                        RED_HAIR,
                                        CURLY_HAIR,
                                        WHITE_HAIR,
                                        BALD -> {
                                            // Emoji is extension, add it as popup to last one
                                            lastKey?.popup?.add(key)
                                        }
                                        else -> {
                                            // Emoji is standalone
                                            layouts[ec]?.add(key)
                                            lastKey = key
                                        }
                                    }
                                } else {
                                    // Emoji is standalone
                                    layouts[ec]?.add(key)
                                    lastKey = key
                                }
                            } else {
                                lastKey = null
                            }
                        }
                    }
                }
            }
        }
    } catch (e: IOException) {
        Timber.e("parseRawEmojiSpecsFile(): $e")
    } finally {
        if (reader != null) {
            try {
                reader.close()
            } catch (e: IOException) {
                Timber.e("parseRawEmojiSpecsFile(): $e")
            }
        }
    }
    cachedEmojiLayoutMap = layouts
    return layouts
}
