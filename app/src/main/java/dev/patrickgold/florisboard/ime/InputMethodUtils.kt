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

package dev.patrickgold.florisboard.ime

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import dev.patrickgold.florisboard.BuildConfig
import dev.patrickgold.florisboard.debug.flogInfo

private const val IME_ID: String =
    "dev.patrickgold.florisboard/.FlorisImeService"
private const val IME_ID_BETA: String =
    "dev.patrickgold.florisboard.beta/dev.patrickgold.florisboard.FlorisImeService"
private const val IME_ID_DEBUG: String =
    "dev.patrickgold.florisboard.debug/dev.patrickgold.florisboard.FlorisImeService"

object InputMethodUtils {
    fun checkIsFlorisboardEnabled(context: Context): Boolean {
        val activeImeIds = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_INPUT_METHODS
        ) ?: "(none)"
        flogInfo { "List of active IMEs: $activeImeIds" }
        return when {
            BuildConfig.DEBUG -> {
                activeImeIds.split(":").contains(IME_ID_DEBUG)
            }
            context.packageName.endsWith(".beta") -> {
                activeImeIds.split(":").contains(IME_ID_BETA)
            }
            else -> {
                activeImeIds.split(":").contains(IME_ID)
            }
        }
    }

    fun checkIsFlorisboardSelected(context: Context): Boolean {
        val selectedImeId = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.DEFAULT_INPUT_METHOD
        ) ?: "(none)"
        flogInfo { "Selected IME: $selectedImeId" }
        return when {
            BuildConfig.DEBUG -> {
                selectedImeId == IME_ID_DEBUG
            }
            context.packageName.endsWith(".beta") -> {
                selectedImeId.split(":").contains(IME_ID_BETA)
            }
            else -> {
                selectedImeId == IME_ID
            }
        }
    }

    fun showSystemEnablerActivity(context: Context) {
        val intent = Intent()
        intent.action = Settings.ACTION_INPUT_METHOD_SETTINGS
        intent.addCategory(Intent.CATEGORY_DEFAULT)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(intent)
    }

    fun showSystemPicker(context: Context) {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.showInputMethodPicker()
    }
}
