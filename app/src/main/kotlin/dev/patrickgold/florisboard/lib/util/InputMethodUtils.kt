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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.LocalContext
import dev.patrickgold.florisboard.BuildConfig
import dev.patrickgold.florisboard.lib.devtools.flogDebug
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import org.florisboard.lib.android.AndroidSettings
import org.florisboard.lib.android.AndroidVersion
import org.florisboard.lib.android.systemServiceOrNull
import kotlin.time.Duration.Companion.milliseconds

private const val DELIMITER = ':'
private const val IME_SERVICE_CLASS_NAME = "dev.patrickgold.florisboard.FlorisImeService"
private val TIMED_QUERY_DELAY = 500.milliseconds

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
    ): State<Boolean> {
        return observeIsFlorisboardEnabledFlow(context).collectAsState(isFlorisboardEnabled(context))
    }

    @Composable
    fun observeIsFlorisboardSelected(
        context: Context = LocalContext.current.applicationContext,
    ): State<Boolean> {
        return observeIsFlorisboardSelectedFlow(context).collectAsState(isFlorisboardSelected(context))
    }

    fun observeIsFlorisboardEnabledFlow(context: Context): Flow<Boolean> {
        val appContext = context.applicationContext
        return flow {
            while (true) {
                emit(isFlorisboardEnabled(appContext))
                delay(TIMED_QUERY_DELAY)
            }
        }.distinctUntilChanged()
    }

    fun observeIsFlorisboardSelectedFlow(context: Context): Flow<Boolean> {
        val appContext = context.applicationContext
        return flow {
            while (true) {
                emit(isFlorisboardSelected(appContext))
                delay(TIMED_QUERY_DELAY)
            }
        }.distinctUntilChanged()
    }

    fun parseIsFlorisboardEnabled(context: Context, activeImeIds: String): Boolean {
        flogDebug { activeImeIds }
        return activeImeIds.split(DELIMITER).map { componentStr ->
            ComponentName.unflattenFromString(componentStr)
        }.any { it?.packageName == context.packageName && it.className == IME_SERVICE_CLASS_NAME }
    }

    fun parseIsFlorisboardSelected(context: Context, selectedImeId: String): Boolean {
        flogDebug { selectedImeId }
        val component = ComponentName.unflattenFromString(selectedImeId)
        return component?.packageName == context.packageName && component.className == IME_SERVICE_CLASS_NAME
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
