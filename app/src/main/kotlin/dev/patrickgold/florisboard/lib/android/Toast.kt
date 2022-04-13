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

package dev.patrickgold.florisboard.lib.android

import android.content.Context
import android.widget.Toast
import androidx.annotation.StringRes
import dev.patrickgold.florisboard.lib.kotlin.CurlyArg

/**
 * Shows a short toast with specified text.
 *
 * @param text The text to show in the toast popup.
 */
fun Context.showShortToast(text: String): Toast {
    return Toast.makeText(this, text, Toast.LENGTH_SHORT).also { it.show() }
}

/**
 * Shows a short toast with the string resource specified by [id].
 *
 * @param id The string resource id of the text to display. Must not be 0.
 */
fun Context.showShortToast(@StringRes id: Int): Toast {
    val text = this.stringRes(id)
    return Toast.makeText(this, text, Toast.LENGTH_SHORT).also { it.show() }
}

/**
 * Shows a short toast with the string resource specified by [id], additionally curly formatting the string with
 * supplied arguments [args].
 *
 * @param id The string resource id of the text to display. Must not be 0.
 * @param args The curly arguments which will be filled into the string template identified by [id].
 */
fun Context.showShortToast(@StringRes id: Int, vararg args: CurlyArg): Toast {
    val text = this.stringRes(id, *args)
    return Toast.makeText(this, text, Toast.LENGTH_SHORT).also { it.show() }
}

/**
 * Shows a long toast with specified text.
 *
 * @param text The text to show in the toast popup.
 */
fun Context.showLongToast(text: String): Toast {
    return Toast.makeText(this, text, Toast.LENGTH_LONG).also { it.show() }
}

/**
 * Shows a long toast with the string resource specified by [id].
 *
 * @param id The string resource id of the text to display. Must not be 0.
 */
fun Context.showLongToast(@StringRes id: Int): Toast {
    val text = this.stringRes(id)
    return Toast.makeText(this, text, Toast.LENGTH_LONG).also { it.show() }
}

/**
 * Shows a long toast with the string resource specified by [id], additionally curly formatting the string with
 * supplied arguments [args].
 *
 * @param id The string resource id of the text to display. Must not be 0.
 * @param args The curly arguments which will be filled into the string template identified by [id].
 */
fun Context.showLongToast(@StringRes id: Int, vararg args: CurlyArg): Toast {
    val text = this.stringRes(id, *args)
    return Toast.makeText(this, text, Toast.LENGTH_LONG).also { it.show() }
}
