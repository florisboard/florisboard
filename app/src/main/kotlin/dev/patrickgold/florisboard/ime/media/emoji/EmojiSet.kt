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

@JvmInline
value class EmojiSet(val emojis: List<Emoji>) {
    companion object {
        val Unspecified = EmojiSet(listOf(Emoji("", "", emptyList())))
    }

    init {
        require(emojis.isNotEmpty()) { "Cannot create an EmojiSet with no emojis specified." }
    }

    fun base(
        withSkinTone: EmojiSkinTone = EmojiSkinTone.DEFAULT,
        withHairStyle: EmojiHairStyle = EmojiHairStyle.DEFAULT,
    ): Emoji {
        if (emojis.size == 1) return emojis[0] // Fast compute
        for (emoji in emojis) {
            if (emoji.skinTone == withSkinTone /*&& emoji.hairStyle == withHairStyle*/) {
                return emoji
            }
        }
        return emojis[0] // Fallback
    }

    fun variations(
        withoutSkinTone: EmojiSkinTone = EmojiSkinTone.DEFAULT,
        withoutHairStyle: EmojiHairStyle = EmojiHairStyle.DEFAULT,
    ): List<Emoji> {
        if (emojis.size == 1) return emptyList() // Fast compute
        return emojis.filterNot { it.skinTone == withoutSkinTone /*&& it.hairStyle == withoutHairStyle*/ }
    }
}
