/*
 * Copyright (C) 2020-2025 The FlorisBoard Contributors
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

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.Icon
import android.graphics.drawable.LayerDrawable
import android.graphics.drawable.RippleDrawable
import android.os.Build
import android.os.Bundle
import android.util.TypedValue
import androidx.annotation.AttrRes
import androidx.annotation.RequiresApi
import androidx.autofill.inline.UiVersions
import androidx.autofill.inline.common.ImageViewStyle
import androidx.autofill.inline.common.TextViewStyle
import androidx.autofill.inline.common.ViewStyle
import androidx.autofill.inline.v1.InlineSuggestionUi
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.app.florisPreferenceModel
import dev.patrickgold.florisboard.appContext
import dev.patrickgold.florisboard.extensionManager
import dev.patrickgold.florisboard.ime.smartbar.CachedInlineSuggestionsChipStyleSet
import dev.patrickgold.florisboard.lib.devtools.flogInfo
import dev.patrickgold.florisboard.lib.ext.ExtensionComponentName
import dev.patrickgold.florisboard.lib.ext.ExtensionMeta
import dev.patrickgold.florisboard.lib.io.ZipUtils
import dev.patrickgold.florisboard.lib.util.ViewUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.florisboard.lib.kotlin.io.FsDir
import org.florisboard.lib.kotlin.io.deleteContentsRecursively
import org.florisboard.lib.kotlin.io.subDir
import org.florisboard.lib.kotlin.io.subFile
import org.florisboard.lib.snygg.SnyggStylesheet
import org.florisboard.lib.snygg.value.SnyggStaticColorValue
import java.util.UUID
import kotlin.properties.Delegates

/**
 * Core class which manages the keyboard theme. Note, that this does not affect the UI theme of the
 * Settings Activities.
 */
class ThemeManager(context: Context) {
    private val prefs by florisPreferenceModel()
    private val appContext by context.appContext()
    private val extensionManager by context.extensionManager()

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private val _indexedThemeConfigs = MutableLiveData(mapOf<ExtensionComponentName, ThemeExtensionComponent>())
    val indexedThemeConfigs: LiveData<Map<ExtensionComponentName, ThemeExtensionComponent>> get() = _indexedThemeConfigs
    var previewThemeId: ExtensionComponentName? by Delegates.observable(null) { _, _, _ ->
        updateActiveTheme()
    }
    var previewThemeInfo: ThemeInfo? by Delegates.observable(null) { _, _, _ ->
        updateActiveTheme()
    }

    private val cachedThemeInfos = mutableListOf<ThemeInfo>()
    private val activeThemeGuard = Mutex(locked = false)
    private val _activeThemeInfo = MutableLiveData(ThemeInfo.DEFAULT)
    val activeThemeInfo: LiveData<ThemeInfo> get() = _activeThemeInfo

    init {
        extensionManager.themes.observeForever { themeExtensions ->
            val map = buildMap {
                for (themeExtension in themeExtensions) {
                    for (themeComponent in themeExtension.themes) {
                        put(ExtensionComponentName(themeExtension.meta.id, themeComponent.id), themeComponent)
                    }
                }
            }
            _indexedThemeConfigs.postValue(map)
        }
        indexedThemeConfigs.observeForever {
            updateActiveTheme {
                cachedThemeInfos.clear()
            }
        }
        prefs.theme.mode.observeForever {
            updateActiveTheme()
        }
        prefs.theme.dayThemeId.observeForever {
            updateActiveTheme()
        }
        prefs.theme.nightThemeId.observeForever {
            updateActiveTheme()
        }
    }

    /**
     * Updates the current theme ref and loads the corresponding theme, as well as notifies all
     * callback receivers about the new theme.
     */
    fun updateActiveTheme(action: () -> Unit = { }) = scope.launch {
        activeThemeGuard.withLock {
            action()
            previewThemeInfo?.let { previewThemeInfo ->
                _activeThemeInfo.postValue(previewThemeInfo)
                return@withLock
            }
            val activeName = evaluateActiveThemeName()
            val cachedInfo = cachedThemeInfos.find { it.name == activeName }
            if (cachedInfo != null) {
                _activeThemeInfo.postValue(cachedInfo)
                return@withLock
            }
            val themeExt = extensionManager.getExtensionById(activeName.extensionId) as? ThemeExtension
            val themeExtRef = themeExt?.sourceRef
            if (themeExtRef == null) {
                return@withLock
            }
            val themeConfig = themeExt.themes.find { it.id == activeName.componentId }
            if (themeConfig == null) {
                return@withLock
            }
            // TODO: loaded dir is implemented already...
            // TODO: this leaks the loaded dir, but at least the state is not kaputt from compose viewpoint
            val loadedDir = appContext.cacheDir.subDir("loaded").subDir(UUID.randomUUID().toString())
            runCatching {
                loadedDir.mkdirs()
                loadedDir.deleteContentsRecursively()
                ZipUtils.unzip(appContext, themeExtRef, loadedDir).getOrThrow()
                flogInfo { "Loaded extension ${themeExt.meta.id} into $loadedDir" }
                val stylesheetFile = loadedDir.subFile(themeConfig.stylesheetPath())
                val stylesheetJson = stylesheetFile.readText()
                SnyggStylesheet.fromJson(stylesheetJson).getOrThrow()
            }.fold(
                onSuccess = { newStylesheet ->
                    val newInfo = ThemeInfo(activeName, themeConfig, newStylesheet, loadedDir, null)
                    cachedThemeInfos.add(newInfo)
                    _activeThemeInfo.postValue(newInfo)
                },
                onFailure = { cause ->
                    _activeThemeInfo.postValue(ThemeInfo.DEFAULT.copy(
                        loadFailure = LoadFailure(themeExt.meta, themeConfig, cause)
                    ))
                },
            )
        }
    }

    private fun evaluateActiveThemeName(): ExtensionComponentName {
        previewThemeId?.let { return it }
        return when (prefs.theme.mode.get()) {
            ThemeMode.ALWAYS_DAY -> {
                prefs.theme.dayThemeId.get()
            }
            ThemeMode.ALWAYS_NIGHT -> {
                prefs.theme.nightThemeId.get()
            }
            ThemeMode.FOLLOW_SYSTEM -> if (appContext.resources.configuration.uiMode and
                Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
            ) {
                prefs.theme.nightThemeId.get()
            } else {
                prefs.theme.dayThemeId.get()
            }
            ThemeMode.FOLLOW_TIME -> {
                //if (AndroidVersion.ATLEAST_API26_O) {
                //    val current = LocalTime.now()
                //    val sunrise = prefs.theme.sunriseTime.get()
                //    val sunset = prefs.theme.sunsetTime.get()
                //    if (current in sunrise..sunset) {
                //        prefs.theme.dayThemeId.get()
                //    } else {
                //        prefs.theme.nightThemeId.get()
                //    }
                //} else {
                    prefs.theme.nightThemeId.get()
                //}
            }
        }
    }

    /**
     * Creates a new inline suggestion UI bundle based on the attributes of the given [style].
     *
     * @param context The context of the parent view/controller.
     * @param style The style set which is responsible for styling the chips.
     *
     * @return A bundle containing all necessary attributes for the inline suggestion views to properly display.
     */
    @SuppressLint("RestrictedApi")
    @RequiresApi(Build.VERSION_CODES.R)
    fun createInlineSuggestionUiStyleBundle(context: Context): Bundle? {
        val styleSet = CachedInlineSuggestionsChipStyleSet ?: return null
        val bgColor = styleSet.background(default = Color.White)
        val fgColor = styleSet.foreground(default = Color.Black)

        val bgDrawableId = androidx.autofill.R.drawable.autofill_inline_suggestion_chip_background
        val bgDrawable = Icon.createWithResource(context, bgDrawableId).apply {
            setTint(bgColor.toArgb())
        }
        val chipStyle = ViewStyle.Builder().run {
            setBackground(bgDrawable)
            setPadding(
                context.resources.getDimension(R.dimen.suggestion_chip_bg_padding_start).toInt(),
                context.resources.getDimension(R.dimen.suggestion_chip_bg_padding_top).toInt(),
                context.resources.getDimension(R.dimen.suggestion_chip_bg_padding_end).toInt(),
                context.resources.getDimension(R.dimen.suggestion_chip_bg_padding_bottom).toInt(),
            )
            build()
        }
        val iconStyle = ImageViewStyle.Builder().run {
            setLayoutMargin(0, 0, 0, 0)
            build()
        }
        val titleStyle = TextViewStyle.Builder().run {
            setLayoutMargin(
                context.resources.getDimension(R.dimen.suggestion_chip_fg_title_margin_start).toInt(),
                context.resources.getDimension(R.dimen.suggestion_chip_fg_title_margin_top).toInt(),
                context.resources.getDimension(R.dimen.suggestion_chip_fg_title_margin_end).toInt(),
                context.resources.getDimension(R.dimen.suggestion_chip_fg_title_margin_bottom).toInt(),
            )
            setTextColor(fgColor.toArgb())
            setTextSize(16f)
            build()
        }
        val subtitleStyle = TextViewStyle.Builder().run {
            setLayoutMargin(
                context.resources.getDimension(R.dimen.suggestion_chip_fg_subtitle_margin_start).toInt(),
                context.resources.getDimension(R.dimen.suggestion_chip_fg_subtitle_margin_top).toInt(),
                context.resources.getDimension(R.dimen.suggestion_chip_fg_subtitle_margin_end).toInt(),
                context.resources.getDimension(R.dimen.suggestion_chip_fg_subtitle_margin_bottom).toInt(),
            )
            setTextColor(ColorUtils.setAlphaComponent(fgColor.toArgb(), 150))
            setTextSize(14f)
            build()
        }
        val suggestionStyle = InlineSuggestionUi.newStyleBuilder().run {
            setSingleIconChipStyle(chipStyle)
            setChipStyle(chipStyle)
            setStartIconStyle(iconStyle)
            setEndIconStyle(iconStyle)
            setTitleStyle(titleStyle)
            setSubtitleStyle(subtitleStyle)
            build()
        }
        return UiVersions.newStylesBuilder().run {
            addStyle(suggestionStyle)
            build()
        }
    }

    private fun getColorFromThemeAttribute(
        context: Context, typedValue: TypedValue, @AttrRes attr: Int,
    ): Int? {
        return if (context.theme.resolveAttribute(attr, typedValue, true)) {
            if (typedValue.type == TypedValue.TYPE_REFERENCE) {
                ContextCompat.getColor(context, typedValue.resourceId)
            } else {
                typedValue.data
            }
        } else {
            null
        }
    }

    data class ThemeInfo(
        val name: ExtensionComponentName,
        val config: ThemeExtensionComponent,
        val stylesheet: SnyggStylesheet,
        val loadedDir: FsDir?,
        val loadFailure: LoadFailure?,
    ) {
        override fun toString(): String {
            return "ThemeInfo(name=$name, config=$config, loadedDir=$loadedDir)"
        }

        companion object {
            val DEFAULT = ThemeInfo(
                name = extCoreTheme("base"),
                config = ThemeExtensionComponentImpl(id = "base", label = "Base", authors = listOf()),
                stylesheet = FlorisImeThemeBaseStyle,
                loadedDir = null,
                loadFailure = null,
            )
        }
    }

    data class LoadFailure(
        val extension: ExtensionMeta,
        val component: ThemeExtensionComponent,
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

    @RequiresApi(Build.VERSION_CODES.R)
    private fun autofillChipBackgroundOf(
        bgColor: Color,
        rippleColor: Color,
    ): Drawable {
        val cornerRadius = ViewUtils.dp2px(32f)
        val shadowColors = intArrayOf(
            Color.Transparent.toArgb(),
            Color.Transparent.toArgb(),
            Color(red = 0x00, green = 0x00, blue = 0x00, alpha = 0x1F).toArgb(),
        )
        val shadowDistribution = floatArrayOf(0f, 0.5f, 1f)
        val padding = ViewUtils.dp2px(5f).toInt()

        fun gradientDrawableOf() = GradientDrawable().also {
            it.shape = GradientDrawable.RECTANGLE
            it.cornerRadius = cornerRadius
            it.setGradientCenter(0.5f, 0.5f)
            it.gradientType = GradientDrawable.LINEAR_GRADIENT
            it.setColors(shadowColors, shadowDistribution)
        }

        val layerList = LayerDrawable(arrayOf(
            gradientDrawableOf().also {
                it.orientation = GradientDrawable.Orientation.BOTTOM_TOP
            },
            gradientDrawableOf().also {
                it.orientation = GradientDrawable.Orientation.LEFT_RIGHT
            },
            gradientDrawableOf().also {
                it.orientation = GradientDrawable.Orientation.TOP_BOTTOM
            },
            gradientDrawableOf().also {
                it.orientation = GradientDrawable.Orientation.RIGHT_LEFT
            },
            GradientDrawable().also {
                it.shape = GradientDrawable.RECTANGLE
                it.cornerRadius = cornerRadius
                it.setColor(bgColor.toArgb())
            },
        )).also { it.setLayerInset(4, padding, padding, padding, padding) }
        return RippleDrawable(ColorStateList.valueOf(rippleColor.toArgb()), layerList, null)
    }
}
