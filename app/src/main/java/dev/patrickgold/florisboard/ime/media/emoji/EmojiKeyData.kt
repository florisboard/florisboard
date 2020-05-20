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

/**
 * Data class for a single emoji (with possible emoji variants in [popup]).
 *
 * @property codePoints The code points of the emoji.
 * @property label The name of the emoji.
 * @property popup List of possible variants of the emoji.
 */
data class EmojiKeyData(
    var codePoints: List<Int>,
    var label: String = "",
    var popup: MutableList<EmojiKeyData> = mutableListOf()
) {

    /**
     * Returns an encoded String based on this emoji's [codePoints].
     *
     * @return The encoded String.
     */
    fun getCodePointsAsString(): String {
        var ret = ""
        for (codePoint in codePoints) {
            ret += String(Character.toChars(codePoint))
        }
        return ret
    }
}
