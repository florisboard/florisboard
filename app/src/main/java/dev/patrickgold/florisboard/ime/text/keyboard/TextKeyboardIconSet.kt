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

package dev.patrickgold.florisboard.ime.text.keyboard

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.ime.keyboard.KeyboardIconSet

/**
 * An icon set for [TextKeyboardView].
 */
class TextKeyboardIconSet private constructor(context: Context) : KeyboardIconSet() {
    private val drawableCache: Array<Drawable?> = Array(RES_ICON_IDS.size) {
        ContextCompat.getDrawable(context.applicationContext, RES_ICON_IDS[it])?.mutate()
    }

    companion object {
        private val RES_ICON_IDS: IntArray = intArrayOf(
            R.drawable.ic_arrow_right_alt,
            R.drawable.ic_assignment,
            R.drawable.ic_backspace,
            R.drawable.ic_content_copy,
            R.drawable.ic_content_cut,
            R.drawable.ic_content_paste,
            R.drawable.ic_done,
            R.drawable.ic_keyboard_arrow_down,
            R.drawable.ic_keyboard_arrow_left,
            R.drawable.ic_keyboard_arrow_right,
            R.drawable.ic_keyboard_arrow_up,
            R.drawable.ic_keyboard_capslock,
            R.drawable.ic_keyboard_return,
            R.drawable.ic_language,
            R.drawable.ic_search,
            R.drawable.ic_select_all,
            R.drawable.ic_send,
            R.drawable.ic_sentiment_satisfied,
            R.drawable.ic_space_bar,
            R.drawable.ic_keyboard_kana_switcher,
        )

        /**
         * Constructs a new icon set with given [context].
         *
         * @param context The context which should be used to retrieve the pre-defined drawable resource ids.
         */
        fun new(context: Context): TextKeyboardIconSet {
            return TextKeyboardIconSet(context)
        }
    }

    override fun getDrawable(@DrawableRes id: Int): Drawable? {
        return drawableCache.getOrNull(RES_ICON_IDS.indexOf(id))
    }
}
