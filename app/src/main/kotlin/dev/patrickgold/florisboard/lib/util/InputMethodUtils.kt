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

package dev.patrickgold.florisboard.lib.util

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import dev.patrickgold.florisboard.BuildConfig
import dev.patrickgold.florisboard.lib.compose.observeAsState
import dev.patrickgold.florisboard.lib.devtools.flogDebug
import kotlinx.coroutines.delay
import org.florisboard.lib.android.AndroidSettings
import org.florisboard.lib.android.AndroidVersion
import org.florisboard.lib.android.systemServiceOrNull

private const val DELIMITER = ':'
private const val IME_SERVICE_CLASS_NAME = "dev.patrickgold.florisboard.FlorisImeService"
private const val TIMED_QUERY_DELAY = 500L

object InputMethodUtils {
    fun isFlorisboardEnabled(context: Context): Boolean {
        return if (AndroidVersion.ATLEAST_API34_U) {
            context.systemServiceOrNull(InputMethodManager::class)
                ?.enabledInputMethodList
                ?.any { it.packageName == BuildConfig.APPLICATION_ID } ?: false
        } else {
            val enabledImeList = AndroidSettings.Secure.getString(
                context, Settings.Secure.ENABLED_INPUT_METHODS
            )
            enabledImeList != null && parseIsFlorisboardEnabled(context, enabledImeList)
        }
    }

    fun isFlorisboardSelected(context: Context): Boolean {
        return if (AndroidVersion.ATLEAST_API34_U) {
            context.systemServiceOrNull(InputMethodManager::class)
                ?.currentInputMethodInfo
                ?.packageName == BuildConfig.APPLICATION_ID
        } else {
            val selectedIme = AndroidSettings.Secure.getString(
                context, Settings.Secure.DEFAULT_INPUT_METHOD
            )
            selectedIme != null && parseIsFlorisboardSelected(context, selectedIme)
        }
    }

    @Composable
    fun observeIsFlorisboardEnabled(
        context: Context = LocalContext.current.applicationContext,
        foregroundOnly: Boolean = false,
    ): State<Boolean> {
        return if (AndroidVersion.ATLEAST_API34_U) {
            timedObserveIsFlorisBoardEnabled()
        } else {
            AndroidSettings.Secure.observeAsState(
                key = Settings.Secure.ENABLED_INPUT_METHODS,
                foregroundOnly = foregroundOnly,
                transform = { parseIsFlorisboardEnabled(context, it.toString()) },
            )
        }
    }

    @Composable
    fun observeIsFlorisboardSelected(
        context: Context = LocalContext.current.applicationContext,
        foregroundOnly: Boolean = false,
    ): State<Boolean> {
        return if (AndroidVersion.ATLEAST_API34_U) {
            timedObserveIsFlorisBoardSelected()
        } else {
            AndroidSettings.Secure.observeAsState(
                key = Settings.Secure.DEFAULT_INPUT_METHOD,
                foregroundOnly = foregroundOnly,
                transform = { parseIsFlorisboardSelected(context, it.toString()) },
            )
        }
    }

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

    @RequiresApi(api = 34)
    @Composable
    private fun timedObserveIsFlorisBoardEnabled(): State<Boolean> {
        val state = remember { mutableStateOf(false) }
        val context = LocalContext.current
        LaunchedEffect(Unit) {
            while (true) {
                state.value = isFlorisboardEnabled(context)
                delay(TIMED_QUERY_DELAY)
            }
        }
        return state
    }

    @RequiresApi(api = 34)
    @Composable
    private fun timedObserveIsFlorisBoardSelected(): State<Boolean> {
        val state = remember { mutableStateOf(false) }
        val context = LocalContext.current
        LaunchedEffect(Unit) {
            while (true) {
                state.value = isFlorisboardSelected(context)
                delay(TIMED_QUERY_DELAY)
            }
        }
        return state
    }
}
