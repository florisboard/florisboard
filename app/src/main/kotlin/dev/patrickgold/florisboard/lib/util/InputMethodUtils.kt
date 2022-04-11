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

package dev.patrickgold.florisboard.lib.util

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import dev.patrickgold.florisboard.lib.android.AndroidSettings
import dev.patrickgold.florisboard.lib.android.systemServiceOrNull
import dev.patrickgold.florisboard.lib.devtools.flogDebug

private const val DELIMITER = ':'
private const val IME_SERVICE_CLASS_NAME = "dev.patrickgold.florisboard.FlorisImeService"

object InputMethodUtils {
    @Composable
    fun observeIsFlorisboardEnabled(
        context: Context = LocalContext.current.applicationContext,
        foregroundOnly: Boolean = false,
    ) = AndroidSettings.Secure.observeAsState(
        key = Settings.Secure.ENABLED_INPUT_METHODS,
        foregroundOnly = foregroundOnly,
        transform = { parseIsFlorisboardEnabled(context, it.toString()) },
    )

    @Composable
    fun observeIsFlorisboardSelected(
        context: Context = LocalContext.current.applicationContext,
        foregroundOnly: Boolean = false,
    ) = AndroidSettings.Secure.observeAsState(
        key = Settings.Secure.DEFAULT_INPUT_METHOD,
        foregroundOnly = foregroundOnly,
        transform = { parseIsFlorisboardSelected(context, it.toString()) },
    )

    fun parseIsFlorisboardEnabled(context: Context, activeImeIds: String): Boolean {
        flogDebug { activeImeIds }
        return activeImeIds.split(DELIMITER).map { componentStr ->
            ComponentName.unflattenFromString(componentStr)
        }.any { it?.packageName == context.packageName && it?.className == IME_SERVICE_CLASS_NAME }
    }

    fun parseIsFlorisboardSelected(context: Context, selectedImeId: String): Boolean {
        flogDebug { selectedImeId }
        val component = ComponentName.unflattenFromString(selectedImeId)
        return component?.packageName == context.packageName && component?.className == IME_SERVICE_CLASS_NAME
    }

    fun showImeEnablerActivity(context: Context) {
        val intent = Intent()
        intent.action = Settings.ACTION_INPUT_METHOD_SETTINGS
        intent.addCategory(Intent.CATEGORY_DEFAULT)
        context.startActivity(intent)
    }

    fun showImePicker(context: Context): Boolean {
        val imm = context.systemServiceOrNull(InputMethodManager::class)
        return if (imm != null) {
            imm.showInputMethodPicker()
            true
        } else {
            false
        }
    }
}
