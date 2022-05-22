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

package dev.patrickgold.florisboard.lib.util

import android.os.Bundle
import android.text.InputType
import android.text.TextUtils
import android.view.inputmethod.EditorInfo
import androidx.core.view.inputmethod.EditorInfoCompat
import kotlin.reflect.KClass

private const val SECTION_SEPARATOR = "---"

fun EditorInfo.debugSummarize(): String {
    val info = this
    return buildString {
        appendLine(info::class.qualifiedName)
        append("packageName: ").appendLine(info.packageName)
        appendLine(SECTION_SEPARATOR)
        append("inputType: ").appendLine(info.inputType.debugSummarize(InputType::class))
        append("imeOptions: ").appendLine(info.imeOptions.debugSummarize(EditorInfo::class))
        append("privateImeOptions: ").appendLine(info.privateImeOptions ?: "(null)")
        appendLine(SECTION_SEPARATOR)
        append("actionId: ").appendLine(info.actionId.dsEditorInfoActionId())
        append("actionLabel: ").appendLine(info.actionLabel ?: "(null)")
        append("contentMimeTypes: ").appendLine(EditorInfoCompat.getContentMimeTypes(info).contentToString())
        append("extras: ").appendLine(info.extras?.debugSummarize() ?: "(null)")
        append("hintLocales: ").also {
            appendLine(info.hintLocales?.toLanguageTags() ?: "(null)")
        }
        append("hintText: ").appendLine(info.hintText ?: "(null)")
        appendLine(SECTION_SEPARATOR)
        append("initialCapsMode: ").appendLine(info.initialCapsMode.debugSummarize(TextUtils::class))
        append("initialSelStart: ").appendLine(info.initialSelStart)
        append("initialSelEnd: ").appendLine(info.initialSelEnd)
    }
}

private fun Bundle.debugSummarize(): String {
    val bundle = this
    return buildString {
        append("[")
        for ((i, key) in bundle.keySet().withIndex()) {
            if (i > 0) {
                append(",")
            }
            append(key)
            append("=")
            append(bundle.get(key))
        }
        append("]")
    }
}

private fun Int.dsEditorInfoActionId(): String {
    return when (this) {
        EditorInfo.IME_ACTION_DONE -> "IME_ACTION_DONE"
        EditorInfo.IME_ACTION_GO -> "IME_ACTION_GO"
        EditorInfo.IME_ACTION_NEXT -> "IME_ACTION_NEXT"
        EditorInfo.IME_ACTION_NONE -> "IME_ACTION_NONE"
        EditorInfo.IME_ACTION_PREVIOUS -> "IME_ACTION_PREVIOUS"
        EditorInfo.IME_ACTION_SEARCH -> "IME_ACTION_SEARCH"
        EditorInfo.IME_ACTION_SEND -> "IME_ACTION_SEND"
        EditorInfo.IME_ACTION_UNSPECIFIED -> "IME_ACTION_UNSPECIFIED"
        else -> String.format("0x%08x", this)
    }
}

private fun <T: Any> Int.debugSummarize(type: KClass<T>): String {
    val summary = StringBuilder()
    when (type) {
        EditorInfo::class -> {
            when (this) {
                EditorInfo.IME_NULL -> {
                    summary.append("IME_NULL")
                }
                else -> {
                    val tAction = (this and EditorInfo.IME_MASK_ACTION).dsEditorInfoActionId()
                    val tFlags = StringBuilder()
                    if (this and EditorInfo.IME_FLAG_FORCE_ASCII > 0) {
                        tFlags.append("IME_FLAG_FORCE_ASCII|")
                    }
                    if (this and EditorInfo.IME_FLAG_NAVIGATE_NEXT > 0) {
                        tFlags.append("IME_FLAG_NAVIGATE_NEXT|")
                    }
                    if (this and EditorInfo.IME_FLAG_NAVIGATE_PREVIOUS > 0) {
                        tFlags.append("IME_FLAG_NAVIGATE_PREVIOUS|")
                    }
                    if (this and EditorInfo.IME_FLAG_NO_ACCESSORY_ACTION > 0) {
                        tFlags.append("IME_FLAG_NO_ACCESSORY_ACTION|")
                    }
                    if (this and EditorInfo.IME_FLAG_NO_ENTER_ACTION > 0) {
                        tFlags.append("IME_FLAG_NO_ENTER_ACTION|")
                    }
                    if (this and EditorInfo.IME_FLAG_NO_EXTRACT_UI > 0) {
                        tFlags.append("IME_FLAG_NO_EXTRACT_UI|")
                    }
                    if (this and EditorInfo.IME_FLAG_NO_FULLSCREEN > 0) {
                        tFlags.append("IME_FLAG_NO_FULLSCREEN|")
                    }
                    if (this and EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING > 0) {
                        tFlags.append("IME_FLAG_NO_PERSONALIZED_LEARNING|")
                    }
                    if (tFlags.isEmpty()) {
                        tFlags.append("(none)")
                    }
                    if (tFlags.endsWith("|")) {
                        tFlags.deleteAt(tFlags.length - 1)
                    }
                    summary.append("action=$tAction flags=$tFlags")
                }
            }
        }
        InputType::class -> {
            when (this) {
                InputType.TYPE_NULL -> {
                    summary.append("TYPE_NULL")
                }
                else -> {
                    val tClass: String
                    val tVariation: String
                    val tFlags = StringBuilder()
                    when (this and InputType.TYPE_MASK_CLASS) {
                        InputType.TYPE_CLASS_DATETIME -> {
                            tClass = "TYPE_CLASS_DATETIME"
                            tVariation = when (this and InputType.TYPE_MASK_VARIATION) {
                                InputType.TYPE_DATETIME_VARIATION_DATE -> "TYPE_DATETIME_VARIATION_DATE"
                                InputType.TYPE_DATETIME_VARIATION_NORMAL -> "TYPE_DATETIME_VARIATION_NORMAL"
                                InputType.TYPE_DATETIME_VARIATION_TIME -> "TYPE_DATETIME_VARIATION_TIME"
                                else -> String.format("0x%08x", this and InputType.TYPE_MASK_VARIATION)
                            }
                        }
                        InputType.TYPE_CLASS_NUMBER -> {
                            tClass = "TYPE_CLASS_NUMBER"
                            tVariation = when (this and InputType.TYPE_MASK_VARIATION) {
                                InputType.TYPE_NUMBER_VARIATION_NORMAL -> "TYPE_NUMBER_VARIATION_NORMAL"
                                InputType.TYPE_NUMBER_VARIATION_PASSWORD -> "TYPE_NUMBER_VARIATION_PASSWORD"
                                else -> String.format("0x%08x", this and InputType.TYPE_MASK_VARIATION)
                            }
                            if (this and InputType.TYPE_NUMBER_FLAG_DECIMAL > 0) {
                                tFlags.append("TYPE_NUMBER_FLAG_DECIMAL|")
                            }
                            if (this and InputType.TYPE_NUMBER_FLAG_SIGNED > 0) {
                                tFlags.append("TYPE_NUMBER_FLAG_SIGNED|")
                            }
                        }
                        InputType.TYPE_CLASS_PHONE -> {
                            tClass = "TYPE_CLASS_PHONE"
                            tVariation = String.format("0x%08x", this and InputType.TYPE_MASK_VARIATION)
                        }
                        InputType.TYPE_CLASS_TEXT -> {
                            tClass = "TYPE_CLASS_TEXT"
                            tVariation = when (this and InputType.TYPE_MASK_VARIATION) {
                                InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS -> "TYPE_TEXT_VARIATION_EMAIL_ADDRESS"
                                InputType.TYPE_TEXT_VARIATION_EMAIL_SUBJECT -> "TYPE_TEXT_VARIATION_EMAIL_SUBJECT"
                                InputType.TYPE_TEXT_VARIATION_FILTER -> "TYPE_TEXT_VARIATION_FILTER"
                                InputType.TYPE_TEXT_VARIATION_LONG_MESSAGE -> "TYPE_TEXT_VARIATION_LONG_MESSAGE"
                                InputType.TYPE_TEXT_VARIATION_NORMAL -> "TYPE_TEXT_VARIATION_NORMAL"
                                InputType.TYPE_TEXT_VARIATION_PASSWORD -> "TYPE_TEXT_VARIATION_PASSWORD"
                                InputType.TYPE_TEXT_VARIATION_PERSON_NAME -> "TYPE_TEXT_VARIATION_PERSON_NAME"
                                InputType.TYPE_TEXT_VARIATION_PHONETIC -> "TYPE_TEXT_VARIATION_PHONETIC"
                                InputType.TYPE_TEXT_VARIATION_POSTAL_ADDRESS -> "TYPE_TEXT_VARIATION_POSTAL_ADDRESS"
                                InputType.TYPE_TEXT_VARIATION_SHORT_MESSAGE -> "TYPE_TEXT_VARIATION_SHORT_MESSAGE"
                                InputType.TYPE_TEXT_VARIATION_URI -> "TYPE_TEXT_VARIATION_URI"
                                InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD -> "TYPE_TEXT_VARIATION_VISIBLE_PASSWORD"
                                InputType.TYPE_TEXT_VARIATION_WEB_EDIT_TEXT -> "TYPE_TEXT_VARIATION_WEB_EDIT_TEXT"
                                InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS -> "TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS"
                                InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD -> "TYPE_TEXT_VARIATION_WEB_PASSWORD"
                                else -> String.format("0x%08x", this and InputType.TYPE_MASK_VARIATION)
                            }
                            if (this and InputType.TYPE_TEXT_FLAG_AUTO_COMPLETE > 0) {
                                tFlags.append("TYPE_TEXT_FLAG_AUTO_COMPLETE|")
                            }
                            if (this and InputType.TYPE_TEXT_FLAG_AUTO_CORRECT > 0) {
                                tFlags.append("TYPE_TEXT_FLAG_AUTO_CORRECT|")
                            }
                            if (this and InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS > 0) {
                                tFlags.append("TYPE_TEXT_FLAG_CAP_CHARACTERS|")
                            }
                            if (this and InputType.TYPE_TEXT_FLAG_CAP_SENTENCES > 0) {
                                tFlags.append("TYPE_TEXT_FLAG_CAP_SENTENCES|")
                            }
                            if (this and InputType.TYPE_TEXT_FLAG_CAP_WORDS > 0) {
                                tFlags.append("TYPE_TEXT_FLAG_CAP_WORDS|")
                            }
                            if (this and InputType.TYPE_TEXT_FLAG_IME_MULTI_LINE > 0) {
                                tFlags.append("TYPE_TEXT_FLAG_IME_MULTI_LINE|")
                            }
                            if (this and InputType.TYPE_TEXT_FLAG_MULTI_LINE > 0) {
                                tFlags.append("TYPE_TEXT_FLAG_MULTI_LINE|")
                            }
                            if (this and InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS > 0) {
                                tFlags.append("TYPE_TEXT_FLAG_NO_SUGGESTIONS|")
                            }
                        }
                        else -> {
                            tClass = String.format("0x%08x", this and InputType.TYPE_MASK_CLASS)
                            tVariation = String.format("0x%08x", this and InputType.TYPE_MASK_VARIATION)
                        }
                    }
                    if (tFlags.isEmpty()) {
                        tFlags.append("(none)")
                    }
                    if (tFlags.endsWith("|")) {
                        tFlags.deleteAt(tFlags.length - 1)
                    }
                    summary.append("class=$tClass variation=$tVariation flags=$tFlags")
                }
            }
        }
        TextUtils::class -> {
            val tFlags = StringBuilder()
            if (this and TextUtils.CAP_MODE_CHARACTERS > 0) {
                tFlags.append("CAP_MODE_CHARACTERS|")
            }
            if (this and TextUtils.CAP_MODE_SENTENCES > 0) {
                tFlags.append("CAP_MODE_SENTENCES|")
            }
            if (this and TextUtils.CAP_MODE_WORDS > 0) {
                tFlags.append("CAP_MODE_WORDS|")
            }
            if (this and TextUtils.SAFE_STRING_FLAG_FIRST_LINE > 0) {
                tFlags.append("SAFE_STRING_FLAG_FIRST_LINE|")
            }
            if (this and TextUtils.SAFE_STRING_FLAG_SINGLE_LINE > 0) {
                tFlags.append("SAFE_STRING_FLAG_SINGLE_LINE|")
            }
            if (this and TextUtils.SAFE_STRING_FLAG_TRIM > 0) {
                tFlags.append("SAFE_STRING_FLAG_TRIM|")
            }
            if (tFlags.isEmpty()) {
                tFlags.append("(none)")
            }
            if (tFlags.endsWith("|")) {
                tFlags.deleteAt(tFlags.length - 1)
            }
            summary.append("flags=$tFlags")
        }
    }
    return summary.toString()
}
