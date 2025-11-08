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

package org.florisboard.lib.android

import android.content.Context
import android.widget.Toast
import androidx.annotation.StringRes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.florisboard.lib.kotlin.CurlyArg

/**
 * Shows a short toast with specified text.
 *
 * @param text The text to show in the toast popup.
 */
suspend fun Context.showShortToast(text: String): Toast = withContext(Dispatchers.Main.immediate) {
    Toast.makeText(this@showShortToast, text, Toast.LENGTH_SHORT).also { it.show() }
}

/**
 * Shows a short toast with the string resource specified by [id].
 *
 * @param id The string resource id of the text to display. Must not be 0.
 */
suspend fun Context.showShortToast(@StringRes id: Int): Toast {
    val text = this.stringRes(id)
    return showShortToast(text)
}

/**
 * Shows a short toast with the string resource specified by [id], additionally curly formatting the string with
 * supplied arguments [args].
 *
 * @param id The string resource id of the text to display. Must not be 0.
 * @param args The curly arguments which will be filled into the string template identified by [id].
 */
suspend fun Context.showShortToast(@StringRes id: Int, vararg args: CurlyArg): Toast {
    val text = this.stringRes(id, *args)
    return showShortToast(text)
}

/**
 * Shows a long toast with specified text.
 *
 * @param text The text to show in the toast popup.
 */
suspend fun Context.showLongToast(text: String): Toast = withContext(Dispatchers.Main.immediate) {
    Toast.makeText(this@showLongToast, text, Toast.LENGTH_LONG).also { it.show() }
}

/**
 * Shows a long toast with the string resource specified by [id].
 *
 * @param id The string resource id of the text to display. Must not be 0.
 */
suspend fun Context.showLongToast(@StringRes id: Int): Toast {
    val text = this.stringRes(id)
    return showLongToast(text)
}

/**
 * Shows a long toast with the string resource specified by [id], additionally curly formatting the string with
 * supplied arguments [args].
 *
 * @param id The string resource id of the text to display. Must not be 0.
 * @param args The curly arguments which will be filled into the string template identified by [id].
 */
suspend fun Context.showLongToast(@StringRes id: Int, vararg args: CurlyArg): Toast {
    val text = this.stringRes(id, *args)
    return showLongToast(text)
}




// These wrappers are temporary, but needed.
// Gradually in the future all event logic will be suspendable, then these wrappers will not be needed anymore.
// DO NOT USE THESE IN SUSPENDABLE CONTEXTS, THIS CAUSES ISSUES

@Deprecated(
    "Use suspend showShortToast instead",
    ReplaceWith("showShortToast(text)")
)
fun Context.showShortToastSync(text: String): Toast = runBlocking {
    showShortToast(text)
}

@Deprecated(
    "Use suspend showShortToast instead",
    ReplaceWith("showShortToast(id)")
)
fun Context.showShortToastSync(@StringRes id: Int): Toast = runBlocking {
    showShortToast(id)
}

@Deprecated(
    "Use suspend showShortToast instead",
    ReplaceWith("showShortToast(id, *args)")
)
fun Context.showShortToastSync(@StringRes id: Int, vararg args: CurlyArg): Toast = runBlocking {
    showShortToast(id, *args)
}

@Deprecated(
    "Use suspend showLongToast instead",
    ReplaceWith("showLongToast(text)")
)
fun Context.showLongToastSync(text: String): Toast = runBlocking {
    showLongToast(text)
}

@Deprecated(
    "Use suspend showLongToast instead",
    ReplaceWith("showLongToast(id)")
)
fun Context.showLongToastSync(@StringRes id: Int): Toast = runBlocking {
    showLongToast(id)
}

@Deprecated(
    "Use suspend showLongToast instead",
    ReplaceWith("showLongToast(id, *args)")
)
fun Context.showLongToastSync(@StringRes id: Int, vararg args: CurlyArg): Toast = runBlocking {
    showLongToast(id, *args)
}

