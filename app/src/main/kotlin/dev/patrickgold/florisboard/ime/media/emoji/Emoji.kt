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

import dev.patrickgold.florisboard.ime.keyboard.AbstractKeyData
import dev.patrickgold.florisboard.ime.keyboard.ComputingEvaluator
import dev.patrickgold.florisboard.ime.keyboard.KeyData
import dev.patrickgold.florisboard.ime.popup.PopupSet
import dev.patrickgold.florisboard.ime.text.key.KeyCode
import dev.patrickgold.florisboard.ime.text.key.KeyType
import java.util.stream.IntStream
import kotlin.streams.toList

enum class EmojiSkinTone(val id: Int) {
    DEFAULT(0x0),
    LIGHT_SKIN_TONE(0x1F3FB),
    MEDIUM_LIGHT_SKIN_TONE(0x1F3FC),
    MEDIUM_SKIN_TONE(0x1F3FD),
    MEDIUM_DARK_SKIN_TONE(0x1F3FE),
    DARK_SKIN_TONE(0x1F3FF);
}

enum class EmojiHairStyle(val id: Int) {
    DEFAULT(0x0),
    RED_HAIR(0x1F9B0),
    CURLY_HAIR(0x1F9B1),
    WHITE_HAIR(0x1F9B2),
    BALD(0x1F9B3);
}

data class Emoji(val value: String, val name: String, val keywords: List<String>) : KeyData {
    override val type = KeyType.CHARACTER
    override val code = KeyCode.UNSPECIFIED
    override val label = value
    override val groupId = 0
    override val popup: PopupSet<AbstractKeyData>? = null

    val skinTone: EmojiSkinTone

    val hairStyle: EmojiHairStyle

    val codePoints: IntStream
        get() = value.codePoints()

    init {
        val codePoints = value.codePoints().toList()
        skinTone = EmojiSkinTone.entries.firstOrNull { codePoints.contains(it.id) } ?: EmojiSkinTone.DEFAULT
        hairStyle = EmojiHairStyle.entries.firstOrNull { codePoints.contains(it.id) } ?: EmojiHairStyle.DEFAULT
    }

    override fun compute(evaluator: ComputingEvaluator): KeyData {
        return this
    }

    override fun asString(isForDisplay: Boolean): String {
        return value
    }

    override fun toString(): String {
        return "Emoji { value=$value, name=$name, keywords=$keywords }"
    }
}
