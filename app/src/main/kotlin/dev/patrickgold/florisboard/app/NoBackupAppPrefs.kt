/*
 * Copyright (C) 2026 The FlorisBoard Contributors
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

package dev.patrickgold.florisboard.app

import dev.patrickgold.florisboard.app.setup.NotificationPermissionState
import dev.patrickgold.florisboard.lib.util.VersionName
import dev.patrickgold.jetpref.datastore.annotations.Preferences
import dev.patrickgold.jetpref.datastore.jetprefDataStoreOf
import dev.patrickgold.jetpref.datastore.model.PreferenceModel

val NoBackupPreferenceStore = jetprefDataStoreOf(NoBackupPreferenceModel::class)

@Preferences
abstract class NoBackupPreferenceModel : PreferenceModel() {
    companion object {
        const val NAME = "no-backup-prefs"
    }

    val homeIsBetaToolboxCollapsed = boolean(
        key = "internal__home_is_beta_toolbox_collapsed_040a01",
        default = false,
    )
    val isImeSetUp = boolean(
        key = "internal__is_ime_set_up",
        default = false,
    )
    val versionOnInstall = string(
        key = "internal__version_on_install",
        default = VersionName.DEFAULT_RAW,
    )
    val versionLastUse = string(
        key = "internal__version_last_use",
        default = VersionName.DEFAULT_RAW,
    )
    val versionLastChangelog = string(
        key = "internal__version_last_changelog",
        default = VersionName.DEFAULT_RAW,
    )
    val notificationPermissionState = enum(
        key = "internal__notification_permission_state",
        default = NotificationPermissionState.NOT_SET,
    )
}
