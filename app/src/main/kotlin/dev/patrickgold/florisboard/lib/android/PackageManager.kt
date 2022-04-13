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

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager

private const val SETTINGS_ACTIVITY_NAME = "dev.patrickgold.florisboard.SettingsLauncherAlias"

fun Context.hideAppIcon() {
    val pkg: PackageManager = this.packageManager
    pkg.setComponentEnabledSetting(
        ComponentName(this, SETTINGS_ACTIVITY_NAME),
        PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
        PackageManager.DONT_KILL_APP
    )
}

fun Context.showAppIcon() {
    val pkg: PackageManager = this.packageManager
    pkg.setComponentEnabledSetting(
        ComponentName(this, SETTINGS_ACTIVITY_NAME),
        PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
        PackageManager.DONT_KILL_APP
    )
}
