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

package dev.patrickgold.florisboard.ime.keyboard

import android.content.Context
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.ime.core.DisplayLanguageNamesIn
import dev.patrickgold.florisboard.ime.core.Subtype
import dev.patrickgold.florisboard.ime.editor.FlorisEditorInfo
import dev.patrickgold.florisboard.ime.editor.ImeOptions
import dev.patrickgold.florisboard.ime.input.InputShiftState
import dev.patrickgold.florisboard.ime.text.key.KeyCode
import dev.patrickgold.florisboard.ime.text.key.KeyType
import dev.patrickgold.florisboard.lib.FlorisLocale

interface ComputingEvaluator {
    val version: Int

    val keyboard: Keyboard

    val editorInfo: FlorisEditorInfo

    val state: KeyboardState

    val subtype: Subtype

    fun context(): Context?

    fun displayLanguageNamesIn(): DisplayLanguageNamesIn

    fun evaluateEnabled(data: KeyData): Boolean

    fun evaluateVisible(data: KeyData): Boolean

    fun isSlot(data: KeyData): Boolean

    fun slotData(data: KeyData): KeyData?
}

object DefaultComputingEvaluator : ComputingEvaluator {
    override val version = -1

    override val keyboard = PlaceholderLoadingKeyboard

    override val editorInfo = FlorisEditorInfo.Unspecified

    override val state = KeyboardState.new()

    override val subtype = Subtype.DEFAULT

    override fun context(): Context? = null

    override fun displayLanguageNamesIn() = DisplayLanguageNamesIn.NATIVE_LOCALE

    override fun evaluateEnabled(data: KeyData): Boolean = true

    override fun evaluateVisible(data: KeyData): Boolean = true

    override fun isSlot(data: KeyData): Boolean = false

    override fun slotData(data: KeyData): KeyData? = null
}

private var cachedDisplayNameState = Triple(FlorisLocale.ROOT, DisplayLanguageNamesIn.SYSTEM_LOCALE, "")

/**
 * Compute language name with a cache to prevent repetitive calling of `locale.displayName()`, which invokes the
 * underlying `LocaleNative.getLanguageName()` method and in turn uses the rather slow ICU data table to look up the
 * language name. This only caches the last display name, but that's more than enough, as a one-time re-computation when
 * the subtype changes does not hurt, the repetitive computation for the same language hurts.
 */
private fun computeLanguageDisplayName(locale: FlorisLocale, displayLanguageNamesIn: DisplayLanguageNamesIn): String {
    val (cachedLocale, cachedDisplayLanguageNamesIn, cachedDisplayName) = cachedDisplayNameState
    if (cachedLocale == locale && cachedDisplayLanguageNamesIn == displayLanguageNamesIn) {
        return cachedDisplayName
    }
    val displayName = when (displayLanguageNamesIn) {
        DisplayLanguageNamesIn.SYSTEM_LOCALE -> locale.displayName()
        DisplayLanguageNamesIn.NATIVE_LOCALE -> locale.displayName(locale)
    }
    cachedDisplayNameState = Triple(locale, displayLanguageNamesIn, displayName)
    return displayName
}

fun ComputingEvaluator.computeLabel(data: KeyData): String? {
    val evaluator = this
    return if (data.type == KeyType.CHARACTER && data.code != KeyCode.SPACE && data.code != KeyCode.CJK_SPACE
        && data.code != KeyCode.HALF_SPACE && data.code != KeyCode.KESHIDA || data.type == KeyType.NUMERIC
    ) {
        data.asString(isForDisplay = true)
    } else {
        when (data.code) {
            KeyCode.PHONE_PAUSE -> evaluator.context()?.getString(R.string.key__phone_pause)
            KeyCode.PHONE_WAIT -> evaluator.context()?.getString(R.string.key__phone_wait)
            KeyCode.SPACE, KeyCode.CJK_SPACE -> {
                when (evaluator.keyboard.mode) {
                    KeyboardMode.CHARACTERS -> evaluator.subtype.primaryLocale.let { locale ->
                        computeLanguageDisplayName(locale, evaluator.displayLanguageNamesIn())
                    }
                    else -> null
                }
            }
            KeyCode.IME_UI_MODE_TEXT,
            KeyCode.VIEW_CHARACTERS -> {
                evaluator.context()?.getString(R.string.key__view_characters)
            }
            KeyCode.VIEW_NUMERIC,
            KeyCode.VIEW_NUMERIC_ADVANCED -> {
                evaluator.context()?.getString(R.string.key__view_numeric)
            }
            KeyCode.VIEW_PHONE -> {
                evaluator.context()?.getString(R.string.key__view_phone)
            }
            KeyCode.VIEW_PHONE2 -> {
                evaluator.context()?.getString(R.string.key__view_phone2)
            }
            KeyCode.VIEW_SYMBOLS -> {
                evaluator.context()?.getString(R.string.key__view_symbols)
            }
            KeyCode.VIEW_SYMBOLS2 -> {
                evaluator.context()?.getString(R.string.key__view_symbols2)
            }
            KeyCode.HALF_SPACE -> {
                evaluator.context()?.getString(R.string.key__view_half_space)
            }
            KeyCode.KESHIDA -> {
                evaluator.context()?.getString(R.string.key__view_keshida)
            }
            else -> null
        }
    }
}

fun ComputingEvaluator.computeIconResId(data: KeyData): Int? {
    val evaluator = this
    return when (data.code) {
        KeyCode.ARROW_LEFT -> {
            R.drawable.ic_keyboard_arrow_left
        }
        KeyCode.ARROW_RIGHT -> {
            R.drawable.ic_keyboard_arrow_right
        }
        KeyCode.ARROW_UP -> {
            R.drawable.ic_keyboard_arrow_up
        }
        KeyCode.ARROW_DOWN -> {
            R.drawable.ic_keyboard_arrow_down
        }
        KeyCode.CLIPBOARD_COPY -> {
            R.drawable.ic_content_copy
        }
        KeyCode.CLIPBOARD_CUT -> {
            R.drawable.ic_content_cut
        }
        KeyCode.CLIPBOARD_PASTE -> {
            R.drawable.ic_content_paste
        }
        KeyCode.CLIPBOARD_SELECT_ALL -> {
            R.drawable.ic_select_all
        }
        KeyCode.CLIPBOARD_CLEAR_PRIMARY_CLIP -> {
            R.drawable.ic_delete_sweep
        }
        KeyCode.COMPACT_LAYOUT_TO_LEFT,
        KeyCode.COMPACT_LAYOUT_TO_RIGHT -> {
            // TODO: find a better icon for compact mode
            R.drawable.ic_smartphone
        }
        KeyCode.VOICE_INPUT -> {
            R.drawable.ic_keyboard_voice
        }
        KeyCode.DELETE -> {
            R.drawable.ic_backspace
        }
        KeyCode.ENTER -> {
            val imeOptions = evaluator.editorInfo.imeOptions
            val inputAttributes = evaluator.editorInfo.inputAttributes
            if (imeOptions.flagNoEnterAction || inputAttributes.flagTextMultiLine) {
                R.drawable.ic_keyboard_return
            } else {
                when (imeOptions.action) {
                    ImeOptions.Action.DONE -> R.drawable.ic_done
                    ImeOptions.Action.GO -> R.drawable.ic_arrow_right_alt
                    ImeOptions.Action.NEXT -> R.drawable.ic_arrow_right_alt
                    ImeOptions.Action.NONE -> R.drawable.ic_keyboard_return
                    ImeOptions.Action.PREVIOUS -> R.drawable.ic_arrow_right_alt
                    ImeOptions.Action.SEARCH -> R.drawable.ic_search
                    ImeOptions.Action.SEND -> R.drawable.ic_send
                    ImeOptions.Action.UNSPECIFIED -> R.drawable.ic_keyboard_return
                }
            }
        }
        KeyCode.IME_UI_MODE_MEDIA -> {
            R.drawable.ic_sentiment_satisfied
        }
        KeyCode.IME_UI_MODE_CLIPBOARD -> {
            R.drawable.ic_assignment
        }
        KeyCode.LANGUAGE_SWITCH -> {
            R.drawable.ic_language
        }
        KeyCode.SETTINGS -> {
            R.drawable.ic_settings
        }
        KeyCode.SHIFT -> {
            when (evaluator.state.inputShiftState != InputShiftState.UNSHIFTED) {
                true -> R.drawable.ic_keyboard_capslock
                else -> R.drawable.ic_keyboard_arrow_up
            }
        }
        KeyCode.SPACE, KeyCode.CJK_SPACE -> {
            when (evaluator.keyboard.mode) {
                KeyboardMode.NUMERIC,
                KeyboardMode.NUMERIC_ADVANCED,
                KeyboardMode.PHONE,
                KeyboardMode.PHONE2 -> {
                    R.drawable.ic_space_bar
                }
                else -> null
            }
        }
        KeyCode.UNDO -> {
            R.drawable.ic_undo
        }
        KeyCode.REDO -> {
            R.drawable.ic_redo
        }
        KeyCode.TOGGLE_ACTIONS_OVERFLOW -> {
            R.drawable.ic_more_horiz
        }
        KeyCode.TOGGLE_INCOGNITO_MODE -> {
            if (evaluator.state.isIncognitoMode) {
                R.drawable.ic_incognito
            } else {
                R.drawable.ic_incognito_off
            }
        }
        KeyCode.TOGGLE_AUTOCORRECT -> {
            R.drawable.ic_font_download
        }
        KeyCode.KANA_SWITCHER -> {
            if (evaluator.state.isKanaKata) {
                R.drawable.ic_keyboard_kana_switcher_kata
            } else {
                R.drawable.ic_keyboard_kana_switcher_hira
            }
        }
        KeyCode.CHAR_WIDTH_SWITCHER -> {
            if (evaluator.state.isCharHalfWidth) {
                R.drawable.ic_keyboard_char_width_switcher_full
            } else {
                R.drawable.ic_keyboard_char_width_switcher_half
            }
        }
        KeyCode.CHAR_WIDTH_FULL -> {
            R.drawable.ic_keyboard_char_width_switcher_full
        }
        KeyCode.CHAR_WIDTH_HALF -> {
            R.drawable.ic_keyboard_char_width_switcher_half
        }
        KeyCode.DRAG_MARKER -> {
            if (evaluator.state.debugShowDragAndDropHelpers) R.drawable.ic_close else null
        }
        KeyCode.NOOP -> {
            R.drawable.ic_close
        }
        else -> null
    }
}
