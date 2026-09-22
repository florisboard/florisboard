/*
 * Copyright (C) 2020-2026 The FlorisBoard Contributors
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

import androidx.compose.runtime.staticCompositionLocalOf
import dev.patrickgold.florisboard.app.FlorisPreferenceStore
import dev.patrickgold.florisboard.ime.extension.ExtensionComponentName
import dev.patrickgold.florisboard.ime.extension.ExtensionController
import dev.patrickgold.florisboard.ime.io.FlorisRef
import dev.patrickgold.florisboard.ime.io.Storage
import dev.patrickgold.florisboard.ime.io.StorageController
import dev.patrickgold.florisboard.ime.io.readText
import dev.patrickgold.florisboard.lib.util.TimeUtils.javaLocalTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.florisboard.lib.snygg.SnyggStylesheet
import org.florisboard.lib.snygg.value.SnyggStaticColorValue
import java.time.LocalTime
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * Provides the [ThemeController] instance this composition tree is associated with.
 */
val LocalThemeController = staticCompositionLocalOf<ThemeController> {
    error("No theme controller is associated with this composition tree.")
}

/**
 * Core class which manages the keyboard theme. Note, that this does not affect the UI theme of the
 * Settings Activities.
 */
class ThemeController(
    storageController: StorageController,
    extensionController: ExtensionController,
    initialSystemThemeMode: SystemThemeMode = SystemThemeMode.UNKNOWN,
) {
    private val prefs by FlorisPreferenceStore
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    val activeSystemThemeMode = MutableStateFlow(initialSystemThemeMode)
    val activePreviewThemeId = MutableStateFlow<ExtensionComponentName?>(null)
    val activePreviewTheme = MutableStateFlow<Theme?>(null)

    val effectiveThemeIndex = extensionController.activeIndex.map { extensionIndex ->
        val themeExtensions = buildMap(extensionIndex.extensions.size) {
            extensionIndex.extensions.forEach { (extensionId, extension) ->
                if (extension is ThemeExtension) {
                    put(extensionId, extension)
                }
            }
        }
        ThemeIndex(themeExtensions, mutableListOf())
    }.stateIn(scope, SharingStarted.Eagerly, ThemeIndex(emptyMap(), mutableListOf()))

    private val effectiveLocalTime = flow {
        while (true) {
            val now = LocalTime.now()
            emit(now)
            delay(1.minutes - now.second.seconds)
        }
    }

    private val effectiveThemeMode = combine(
        prefs.theme.mode.asFlow(),
        activeSystemThemeMode,
        prefs.theme.sunriseTime.asFlow(),
        prefs.theme.sunsetTime.asFlow(),
        effectiveLocalTime,
    ) { themeMode, systemThemeMode, sunriseTime, sunsetTime, localTime ->
        when (themeMode) {
            PreferredThemeMode.ALWAYS_DAY -> EffectiveThemeMode.DAY
            PreferredThemeMode.ALWAYS_NIGHT -> EffectiveThemeMode.NIGHT
            PreferredThemeMode.FOLLOW_SYSTEM -> when (systemThemeMode) {
                SystemThemeMode.NIGHT -> EffectiveThemeMode.NIGHT
                else -> EffectiveThemeMode.DAY
            }
            PreferredThemeMode.FOLLOW_TIME -> {
                val sunrise = sunriseTime.javaLocalTime
                val sunset = sunsetTime.javaLocalTime
                if (localTime in sunrise..sunset) {
                    EffectiveThemeMode.DAY
                } else {
                    EffectiveThemeMode.NIGHT
                }
            }
        }
    }.distinctUntilChanged()

    private val effectiveThemeName = combine(
        effectiveThemeMode,
        prefs.theme.dayThemeId.asFlow(),
        prefs.theme.nightThemeId.asFlow(),
        activePreviewThemeId,
    ) { themeMode, dayThemeId, nightThemeId, previewThemeId ->
        previewThemeId ?: when (themeMode) {
            EffectiveThemeMode.DAY -> dayThemeId
            EffectiveThemeMode.NIGHT -> nightThemeId
        }
    }.distinctUntilChanged()

    val effectiveTheme = combine(
        storageController.activeStorage,
        effectiveThemeIndex,
        effectiveThemeName,
        activePreviewTheme,
    ) { storage, themeIndex, themeName, previewThemeInfo ->
        if (previewThemeInfo != null) {
            return@combine previewThemeInfo
        }
        val cachedInfo = themeIndex.cachedThemes.find { it.name == themeName }
        if (cachedInfo != null) {
            return@combine cachedInfo
        }
        val themeExt = themeIndex.extensions[themeName.extensionId] ?:
            return@combine Theme.DEFAULT.copy(
                loadFailure = LoadFailure(null, null, Exception("no extension with id ${themeName.extensionId} found"))
            )
        val themeExtRef = themeExt.sourceRef
        val themeConfig = themeExt.manifest.themes.find { it.id == themeName.componentId } ?:
            return@combine Theme.DEFAULT.copy(
                loadFailure = LoadFailure(themeExt.manifest, null, Exception("no component with id ${themeName.componentId} found"))
            )
        runCatching {
            val stylesheetRef = themeExtRef.subRef(themeConfig.stylesheetPath())
            val stylesheetJson = storage.readText(stylesheetRef)
            SnyggStylesheet.fromJson(stylesheetJson).getOrThrow()
        }.fold(
            onSuccess = { newStylesheet ->
                val newInfo = Theme(themeName, themeConfig, newStylesheet, themeExtRef, storage, null)
                themeIndex.cachedThemes.add(newInfo)
                newInfo
            },
            onFailure = { cause ->
                Theme.DEFAULT.copy(
                    loadFailure = LoadFailure(themeExt.manifest, themeConfig, cause)
                )
            },
        )
    }.stateIn(scope, SharingStarted.Eagerly, Theme.DEFAULT)

    data class ThemeIndex(
        val extensions: Map<String, ThemeExtension>,
        // TODO review this cache mechanism
        val cachedThemes: MutableList<Theme>,
    )

    data class Theme(
        val name: ExtensionComponentName,
        val config: ThemeExtension.ThemeComponent,
        val stylesheet: SnyggStylesheet,
        val extensionRef: FlorisRef?,
        val storage: Storage?,
        val loadFailure: LoadFailure?,
    ) {
        override fun toString(): String {
            return "ThemeInfo(name=$name, config=$config, extensionRef=$extensionRef)"
        }

        companion object {
            val DEFAULT = Theme(
                name = extCoreTheme("base"),
                config = ThemeExtension.ThemeComponent(id = "base", name = "Base", authors = listOf()),
                stylesheet = FlorisImeThemeBaseStyle,
                extensionRef = null,
                storage = null,
                loadFailure = null,
            )
        }
    }

    data class LoadFailure(
        val manifest: ThemeExtension.Manifest?,
        val component: ThemeExtension.ThemeComponent?,
        val cause: Throwable,
    )

    data class RemoteColors(
        val packageName: String,
        val colorPrimary: SnyggStaticColorValue?,
        val colorPrimaryVariant: SnyggStaticColorValue?,
        val colorSecondary: SnyggStaticColorValue?,
    ) {
        companion object {
            val DEFAULT = RemoteColors("undefined", null, null, null)
        }
    }
}
