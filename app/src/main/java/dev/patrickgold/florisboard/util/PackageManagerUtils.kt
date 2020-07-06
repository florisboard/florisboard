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

package dev.patrickgold.florisboard.util

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager

object PackageManagerUtils {
    private const val SETTINGS_ACTIVITY_NAME = "dev.patrickgold.florisboard.SettingsLauncherAlias"

    fun hideAppIcon(context: Context) {
        val pkg: PackageManager = context.packageManager
        pkg.setComponentEnabledSetting(
            ComponentName(context, SETTINGS_ACTIVITY_NAME),
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.DONT_KILL_APP
        )
    }

    fun showAppIcon(context: Context) {
        val pkg: PackageManager = context.packageManager
        pkg.setComponentEnabledSetting(
            ComponentName(context, SETTINGS_ACTIVITY_NAME),
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            PackageManager.DONT_KILL_APP
        )
    }
}
