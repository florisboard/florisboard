/*
 * Copyright (C) 2021-2026 The FlorisBoard Contributors
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

package dev.patrickgold.florisboard.app.devtools

import androidx.compose.material3.ListItemDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.app.FlorisPreferenceStore
import dev.patrickgold.florisboard.app.LocalNavController
import dev.patrickgold.florisboard.app.NoBackupPreferenceStore
import dev.patrickgold.florisboard.app.Routes
import dev.patrickgold.florisboard.app.components.FlorisConfirmDeleteDialog
import dev.patrickgold.florisboard.app.components.FlorisScreenNg
import dev.patrickgold.florisboard.app.components.ScrollableScreenColumn
import dev.patrickgold.florisboard.app.components.SegmentedListColumn
import dev.patrickgold.florisboard.app.components.SpotlightSwitchPreference
import dev.patrickgold.florisboard.app.components.SpotlightText
import dev.patrickgold.florisboard.ime.dictionary.DictionaryManager
import dev.patrickgold.florisboard.ime.dictionary.FlorisUserDictionaryDatabase
import dev.patrickgold.florisboard.ime.keyboard3.interaction.LocalInteractionController
import dev.patrickgold.florisboard.ime.keyboard3.interaction.showLongToast
import dev.patrickgold.florisboard.ime.smartbar.quickaction.QuickActionArrangement
import dev.patrickgold.jetpref.datastore.model.collectAsState
import dev.patrickgold.jetpref.datastore.ui.Preference
import dev.patrickgold.jetpref.datastore.ui.SwitchPreference
import kotlinx.coroutines.launch
import org.florisboard.lib.android.AndroidSettings
import org.florisboard.lib.android.AndroidVersion
import org.florisboard.lib.compose.stringRes

class DebugOnPurposeCrashException : Exception(
    "Success! The app crashed purposely to display this beautiful screen we all love :)"
)

@Composable
fun DevtoolsScreen() {
    val prefs by FlorisPreferenceStore
    val noBackupPrefs by NoBackupPreferenceStore
    val interactionController = LocalInteractionController.current
    val navController = LocalNavController.current
    val scope = rememberCoroutineScope()

    val (showDialog, setShowDialog) = remember { mutableStateOf(false) }

    FlorisScreenNg(
        title = stringRes(R.string.devtools__title),
        previewFieldVisible = true,
    ) { contentPadding ->
        ScrollableScreenColumn(contentPadding) {
            SpotlightText(
                text = stringRes(R.string.devtools__enabled__summary),
            )

            SpotlightSwitchPreference(
                prefs.devtools.enabled,
                title = stringRes(R.string.devtools__enabled__label),
            )

            SegmentedListColumn(title = stringRes(R.string.devtools__title)) {
                val count = 14
                SwitchPreference(
                    prefs.devtools.showPrimaryClip,
                    shapes = ListItemDefaults.segmentedShapes(0, count),
                    title = stringRes(R.string.devtools__show_primary_clip__label),
                    summary = stringRes(R.string.devtools__show_primary_clip__summary),
                    enabledIf = { prefs.devtools.enabled isEqualTo true },
                )
                SwitchPreference(
                    prefs.devtools.showInputStateOverlay,
                    shapes = ListItemDefaults.segmentedShapes(1, count),
                    title = stringRes(R.string.devtools__show_input_state_overlay__label),
                    summary = stringRes(R.string.devtools__show_input_state_overlay__summary),
                    enabledIf = { prefs.devtools.enabled isEqualTo true },
                )
                SwitchPreference(
                    prefs.devtools.showSpellingOverlay,
                    shapes = ListItemDefaults.segmentedShapes(2, count),
                    title = stringRes(R.string.devtools__show_spelling_overlay__label),
                    summary = stringRes(R.string.devtools__show_spelling_overlay__summary),
                    enabledIf = { prefs.devtools.enabled isEqualTo true },
                )
                SwitchPreference(
                    prefs.devtools.showInlineAutofillOverlay,
                    shapes = ListItemDefaults.segmentedShapes(3, count),
                    title = stringRes(R.string.devtools__show_inline_autofill_overlay__label),
                    summary = stringRes(R.string.devtools__show_inline_autofill_overlay__summary),
                    enabledIf = { prefs.devtools.enabled isEqualTo true },
                    visibleIf = { AndroidVersion.ATLEAST_API30_R },
                )
                SwitchPreference(
                    prefs.devtools.showMozcOverlay,
                    title = stringRes(R.string.devtools__show_mozc_overlay__label),
                    summary = stringRes(R.string.devtools__show_mozc_overlay__summary),
                    enabledIf = { prefs.devtools.enabled isEqualTo true },
                )
                SwitchPreference(
                    prefs.devtools.showKeyTouchBoundaries,
                    shapes = ListItemDefaults.segmentedShapes(4, count),
                    title = stringRes(R.string.devtools__show_key_touch_boundaries__label),
                    summary = stringRes(R.string.devtools__show_key_touch_boundaries__summary),
                    enabledIf = { prefs.devtools.enabled isEqualTo true },
                )
                SwitchPreference(
                    prefs.devtools.showDragAndDropHelpers,
                    shapes = ListItemDefaults.segmentedShapes(5, count),
                    title = stringRes(R.string.devtools__show_drag_and_drop_helpers__label),
                    summary = stringRes(R.string.devtools__show_drag_and_drop_helpers__summary),
                    enabledIf = { prefs.devtools.enabled isEqualTo true },
                )
                Preference(
                    onClick = { setShowDialog(true) },
                    shapes = ListItemDefaults.segmentedShapes(6, count),
                    title = stringRes(R.string.devtools__clear_udm_internal_database__label),
                    summary = stringRes(R.string.devtools__clear_udm_internal_database__summary),
                    enabledIf = { prefs.devtools.enabled isEqualTo true },
                )
                val successMsg = stringRes(R.string.devtools__reset_quick_actions_to_default__toast_success)
                Preference(
                    onClick = {
                        scope.launch {
                            prefs.smartbar.actionArrangement.set(QuickActionArrangement.Default)
                            interactionController.showLongToast(successMsg)
                        }
                    },
                    shapes = ListItemDefaults.segmentedShapes(7, count),
                    title = stringRes(R.string.devtools__reset_quick_actions_to_default__label),
                    summary = stringRes(R.string.devtools__reset_quick_actions_to_default__summary),
                    enabledIf = { prefs.devtools.enabled isEqualTo true },
                )
                Preference(
                    onClick = { scope.launch { noBackupPrefs.isImeSetUp.set(false) } },
                    shapes = ListItemDefaults.segmentedShapes(8, count),
                    title = stringRes(R.string.devtools__reset_flag__label, "flag_name" to "isImeSetUp"),
                    summary = stringRes(R.string.devtools__reset_flag_is_ime_set_up__summary),
                    enabledIf = { prefs.devtools.enabled isEqualTo true },
                )
                Preference(
                    onClick = { throw DebugOnPurposeCrashException() },
                    shapes = ListItemDefaults.segmentedShapes(9, count),
                    title = stringRes(R.string.devtools__test_crash_report__label),
                    summary = stringRes(R.string.devtools__test_crash_report__summary),
                    enabledIf = { prefs.devtools.enabled isEqualTo true },
                )
                Preference(
                    onClick = { navController.navigate(Routes.Devtools.ExportDebugLog) },
                    shapes = ListItemDefaults.segmentedShapes(10, count),
                    title = "Debug log",
                    summary = "View and export the debug log",
                    enabledIf = { prefs.devtools.enabled isEqualTo true },
                )
                SwitchPreference(
                    prefs.glide.enabled,
                    shapes = ListItemDefaults.segmentedShapes(11, count),
                    title = "prefs.glide.enabled (debug)",
                    summaryOn = "This impacts your performance and may trigger the all keys invisible bug!",
                    summaryOff = "Recommended to keep this off!",
                    enabledIf = { prefs.devtools.enabled isEqualTo true },
                )
                SwitchPreference(
                    prefs.emoji.experimentalEmojiSearchEnabled,
                    shapes = ListItemDefaults.segmentedShapes(12, count),
                    title = "prefs.emoji.experimentalEmojiSearchEnabled",
                    summary = "Enables the technical PoC UI of emoji search. No proper UX yet!",
                    enabledIf = { prefs.devtools.enabled isEqualTo true },
                )
            }

            SegmentedListColumn(title = stringRes(R.string.devtools__group_ime_window_tools__title)) {
                val count = 2
                SwitchPreference(
                    prefs.devtools.showWindowResizeHandleBoundaries,
                    shapes = ListItemDefaults.segmentedShapes(0, count),
                    title = stringRes(R.string.devtools__show_window_resize_handle_boundaries__label),
                    summary = stringRes(R.string.devtools__show_window_resize_handle_boundaries__summary),
                    enabledIf = { prefs.devtools.enabled isEqualTo true },
                )
                val successMsg = stringRes(R.string.devtools__reset_window_config__toast_success)
                val failureMsg = stringRes(R.string.devtools__reset_window_config__toast_failure)
                Preference(
                    onClick = {
                        scope.launch {
                            prefs.keyboard.windowConfig.reset().fold(
                                onSuccess = {
                                    interactionController.showLongToast(successMsg)
                                },
                                onFailure = { error ->
                                    interactionController.showLongToast(
                                        failureMsg,
                                        "message" to "${error.localizedMessage}",
                                    )
                                },
                            )
                        }
                    },
                    shapes = ListItemDefaults.segmentedShapes(1, count),
                    title = stringRes(R.string.devtools__reset_window_config__label),
                    summary = stringRes(R.string.devtools__reset_window_config__summary),
                    enabledIf = { prefs.devtools.enabled isEqualTo true },
                )
            }

            SegmentedListColumn(title = stringRes(R.string.devtools__group_android__title)) {
                val count = 4
                Preference(
                    shapes = ListItemDefaults.segmentedShapes(0, count),
                    title = stringRes(R.string.devtools__android_settings_global__title),
                    onClick = {
                        navController.navigate(
                            Routes.Devtools.AndroidSettings(AndroidSettings.Global.groupId)
                        )
                    },
                    enabledIf = { prefs.devtools.enabled isEqualTo true },
                )
                Preference(
                    onClick = {
                        navController.navigate(
                            Routes.Devtools.AndroidSettings(AndroidSettings.Secure.groupId)
                        )
                    },
                    shapes = ListItemDefaults.segmentedShapes(1, count),
                    title = stringRes(R.string.devtools__android_settings_secure__title),
                    enabledIf = { prefs.devtools.enabled isEqualTo true },
                )
                Preference(
                    onClick = {
                        navController.navigate(
                            Routes.Devtools.AndroidSettings(AndroidSettings.System.groupId)
                        )
                    },
                    shapes = ListItemDefaults.segmentedShapes(2, count),
                    title = stringRes(R.string.devtools__android_settings_system__title),
                    enabledIf = { prefs.devtools.enabled isEqualTo true },
                )
                Preference(
                    onClick = { navController.navigate(Routes.Devtools.AndroidLocales) },
                    shapes = ListItemDefaults.segmentedShapes(3, count),
                    title = stringRes(R.string.devtools__android_locales__title),
                    enabledIf = { prefs.devtools.enabled isEqualTo true },
                )
            }

            SegmentedListColumn(title = "prefs.internal.version*") {
                val count = 3
                val versionOnInstall by noBackupPrefs.versionOnInstall.collectAsState()
                Preference(
                    onClick = {},
                    shapes = ListItemDefaults.segmentedShapes(0, count),
                    title = "prefs.internal.versionOnInstall",
                    summary = versionOnInstall,
                )
                val versionLastUse by noBackupPrefs.versionLastUse.collectAsState()
                Preference(
                    onClick = {},
                    shapes = ListItemDefaults.segmentedShapes(1, count),
                    title = "prefs.internal.versionLastUse",
                    summary = versionLastUse,
                )
                val versionLastChangelog by noBackupPrefs.versionLastChangelog.collectAsState()
                Preference(
                    onClick = {},
                    shapes = ListItemDefaults.segmentedShapes(2, count),
                    title = "prefs.internal.versionLastChangelog",
                    summary = versionLastChangelog,
                )
            }
        }
    }

    if (showDialog) {
        FlorisConfirmDeleteDialog(
            onConfirm = {
                DictionaryManager.default().let {
                    it.loadUserDictionariesIfNecessary()
                    it.florisUserDictionaryDao()?.deleteAll()
                }
                setShowDialog(false)
            },
            onDismiss = { setShowDialog(false) },
            what = FlorisUserDictionaryDatabase.DB_FILE_NAME,
        )
    }
}
