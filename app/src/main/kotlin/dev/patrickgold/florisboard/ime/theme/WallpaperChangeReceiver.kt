/*
 * Copyright (C) 2025 The FlorisBoard Contributors
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

package dev.patrickgold.florisboard.ime.theme

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dev.patrickgold.florisboard.lib.devtools.flogDebug
import dev.patrickgold.florisboard.themeManager

class WallpaperChangeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent == null) return
        if (context == null) return
        @Suppress("DEPRECATION") // We do not retrieve the wallpaper but only listen to changes
        if (intent.action == Intent.ACTION_WALLPAPER_CHANGED) {
            flogDebug { "Wallpaper changed" }
            context.themeManager().value.updateActiveTheme()
        }
    }
}
