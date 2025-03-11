/*
 * Copyright (C) 2021-2025 The FlorisBoard Contributors
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowRightAlt
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.KeyboardReturn
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.automirrored.outlined.Assignment
import androidx.compose.material.icons.automirrored.outlined.Backspace
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.FontDownload
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.KeyboardCapslock
import androidx.compose.material.icons.filled.KeyboardVoice
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.SentimentSatisfiedAlt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SpaceBar
import androidx.compose.ui.graphics.vector.ImageVector
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.ime.core.DisplayLanguageNamesIn
import dev.patrickgold.florisboard.ime.core.Subtype
import dev.patrickgold.florisboard.ime.editor.FlorisEditorInfo
import dev.patrickgold.florisboard.ime.editor.ImeOptions
import dev.patrickgold.florisboard.ime.input.InputShiftState
import dev.patrickgold.florisboard.ime.text.key.KeyCode
import dev.patrickgold.florisboard.ime.text.key.KeyType
import dev.patrickgold.florisboard.lib.FlorisLocale
import dev.patrickgold.jetpref.datastore.ui.vectorResource

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

fun ComputingEvaluator.computeImageVector(data: KeyData): ImageVector? {
    val evaluator = this
    return when (data.code) {
        KeyCode.ARROW_LEFT -> {
            Icons.AutoMirrored.Filled.KeyboardArrowLeft
        }
        KeyCode.ARROW_RIGHT -> {
            Icons.AutoMirrored.Filled.KeyboardArrowRight
        }
        KeyCode.ARROW_UP -> {
            Icons.Default.KeyboardArrowUp
        }
        KeyCode.ARROW_DOWN -> {
            Icons.Default.KeyboardArrowDown
        }
        KeyCode.CLIPBOARD_COPY -> {
            Icons.Default.ContentCopy
        }
        KeyCode.CLIPBOARD_CUT -> {
            Icons.Default.ContentCut
        }
        KeyCode.CLIPBOARD_PASTE -> {
            Icons.Default.ContentPaste
        }
        KeyCode.CLIPBOARD_SELECT_ALL -> {
            Icons.Default.SelectAll
        }
        KeyCode.CLIPBOARD_CLEAR_PRIMARY_CLIP -> {
            Icons.Default.DeleteSweep
        }
        KeyCode.COMPACT_LAYOUT_TO_LEFT,
        KeyCode.COMPACT_LAYOUT_TO_RIGHT,
        KeyCode.TOGGLE_COMPACT_LAYOUT -> {
            context()?.vectorResource(id = R.drawable.ic_accessibility_one_handed)
        }
        KeyCode.VOICE_INPUT -> {
            Icons.Default.KeyboardVoice
        }
        KeyCode.DELETE -> {
            Icons.AutoMirrored.Outlined.Backspace
        }
        KeyCode.ENTER -> {
            val imeOptions = evaluator.editorInfo.imeOptions
            val inputAttributes = evaluator.editorInfo.inputAttributes
            if (imeOptions.flagNoEnterAction || inputAttributes.flagTextMultiLine) {
                Icons.AutoMirrored.Filled.KeyboardReturn
            } else {
                when (imeOptions.action) {
                    ImeOptions.Action.DONE -> Icons.Default.Done
                    ImeOptions.Action.GO -> Icons.AutoMirrored.Filled.ArrowRightAlt
                    ImeOptions.Action.NEXT -> Icons.AutoMirrored.Filled.ArrowRightAlt
                    ImeOptions.Action.NONE -> Icons.AutoMirrored.Filled.KeyboardReturn
                    ImeOptions.Action.PREVIOUS -> Icons.AutoMirrored.Filled.ArrowRightAlt
                    ImeOptions.Action.SEARCH -> Icons.Default.Search
                    ImeOptions.Action.SEND -> Icons.AutoMirrored.Filled.Send
                    ImeOptions.Action.UNSPECIFIED -> Icons.AutoMirrored.Filled.KeyboardReturn
                }
            }
        }
        KeyCode.IME_UI_MODE_MEDIA -> {
            Icons.Default.SentimentSatisfiedAlt
        }
        KeyCode.IME_UI_MODE_CLIPBOARD -> {
            Icons.AutoMirrored.Outlined.Assignment
        }
        KeyCode.LANGUAGE_SWITCH -> {
            Icons.Default.Language
        }
        KeyCode.SETTINGS -> {
            Icons.Default.Settings
        }
        KeyCode.SHIFT -> {
            when (evaluator.state.inputShiftState != InputShiftState.UNSHIFTED) {
                true -> Icons.Default.KeyboardCapslock
                else -> Icons.Default.KeyboardArrowUp
            }
        }
        KeyCode.SPACE, KeyCode.CJK_SPACE -> {
            when (evaluator.keyboard.mode) {
                KeyboardMode.NUMERIC,
                KeyboardMode.NUMERIC_ADVANCED,
                KeyboardMode.PHONE,
                KeyboardMode.PHONE2 -> {
                    Icons.Default.SpaceBar
                }
                else -> null
            }
        }
        KeyCode.UNDO -> {
            Icons.AutoMirrored.Filled.Undo
        }
        KeyCode.REDO -> {
            Icons.AutoMirrored.Filled.Redo
        }
        KeyCode.TOGGLE_ACTIONS_OVERFLOW -> {
            Icons.Default.MoreHoriz
        }
        KeyCode.TOGGLE_INCOGNITO_MODE -> {
            if (evaluator.state.isIncognitoMode) {
                this.context()?.vectorResource(id = R.drawable.ic_incognito)
            } else {
                this.context()?.vectorResource(id = R.drawable.ic_incognito_off)
            }
        }
        KeyCode.TOGGLE_AUTOCORRECT -> {
            Icons.Default.FontDownload
        }
        KeyCode.KANA_SWITCHER -> {
            if (evaluator.state.isKanaKata) {
                this.context()?.vectorResource(R.drawable.ic_keyboard_kana_switcher_kata)
            } else {
                this.context()?.vectorResource(R.drawable.ic_keyboard_kana_switcher_hira)
            }
        }
        KeyCode.CHAR_WIDTH_SWITCHER -> {
            if (evaluator.state.isCharHalfWidth) {
                this.context()?.vectorResource(R.drawable.ic_keyboard_char_width_switcher_full)
            } else {
                this.context()?.vectorResource(R.drawable.ic_keyboard_char_width_switcher_half)
            }
        }
        KeyCode.CHAR_WIDTH_FULL -> {
            this.context()?.vectorResource(R.drawable.ic_keyboard_char_width_switcher_full)
        }
        KeyCode.CHAR_WIDTH_HALF -> {
            this.context()?.vectorResource(R.drawable.ic_keyboard_char_width_switcher_half)
        }
        KeyCode.DRAG_MARKER -> {
            if (evaluator.state.debugShowDragAndDropHelpers) Icons.Default.Close else null
        }
        KeyCode.NOOP -> {
            Icons.Default.Close
        }
        else -> null
    }
}
