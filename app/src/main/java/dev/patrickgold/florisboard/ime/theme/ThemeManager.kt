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

package dev.patrickgold.florisboard.ime.theme

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.Icon
import android.net.Uri
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
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.debug.LogTopic
import dev.patrickgold.florisboard.debug.flogDebug
import dev.patrickgold.florisboard.debug.flogError
import dev.patrickgold.florisboard.debug.flogInfo
import dev.patrickgold.florisboard.ime.core.Preferences
import dev.patrickgold.florisboard.res.AssetManager
import dev.patrickgold.florisboard.res.FlorisRef
import dev.patrickgold.florisboard.util.TimeUtil
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Core class which manages the keyboard theme. Note, that this does not affect the UI theme of the
 * Settings Activities.
 */
class ThemeManager private constructor(
    private val applicationContext: Context,
    private val assetManager: AssetManager
) {
    private val prefs get() = Preferences.default()
    private val callbackReceivers: CopyOnWriteArrayList<OnThemeUpdatedListener> = CopyOnWriteArrayList()
    private val packageManager: PackageManager? = applicationContext.packageManager

    var activeTheme: Theme = Theme.empty()
        private set
    var indexedDayThemeRefs: MutableMap<FlorisRef, ThemeMetaOnly> = mutableMapOf()
    var indexedNightThemeRefs: MutableMap<FlorisRef, ThemeMetaOnly> = mutableMapOf()
    var isAdaptiveThemeEnabled: Boolean = false
        private set

    var remote: RemoteColors = RemoteColors.DEFAULT
        private set
    private val remoteCache: ArrayList<RemoteColors> = arrayListOf()

    companion object {
        /** The static relative path where a theme is located, regardless of the asset source. */
        const val THEME_PATH_REL: String = "ime/theme"
        /** Maximum size in bytes a theme file may have when loaded. */
        const val THEME_MAX_SIZE: Int = 512_000

        private var defaultInstance: ThemeManager? = null

        fun init(
            applicationContext: Context,
            assetManager: AssetManager
        ): ThemeManager {
            val instance = ThemeManager(applicationContext, assetManager)
            defaultInstance = instance
            return instance
        }

        fun default(): ThemeManager {
            val instance = defaultInstance
            if (instance != null) {
                return instance
            } else {
                throw UninitializedPropertyAccessException(
                    "${ThemeManager::class.simpleName} has not been initialized previously. Make sure to call init() before using default()."
                )
            }
        }

        fun defaultOrNull(): ThemeManager? = defaultInstance
    }

    init {
        update()
    }

    /**
     * Updates the current theme ref and loads the corresponding theme, as well as notifies all
     * callback receivers about the new theme.
     */
    fun update() {
        indexThemeRefs()
        val ref = evaluateActiveThemeRef()
        flogDebug(LogTopic.THEME_MANAGER) { ref.toString() }
        activeTheme = AdaptiveThemeOverlay(
            this, if (ref.isInvalid) {
                Theme.BASE_THEME
            } else {
                loadTheme(ref).getOrDefault(Theme.BASE_THEME)
            }
        )
        flogInfo(LogTopic.THEME_MANAGER) { activeTheme.label }
        notifyCallbackReceivers()
    }

    /**
     * Gets the primary and ark variants of the app with given [remotePackageName].
     * Based AnySoftKeyboard's way of getting remote colors:
     *  https://github.com/AnySoftKeyboard/AnySoftKeyboard/blob/master/ime/overlay/src/main/java/com/anysoftkeyboard/overlay/OverlyDataCreatorForAndroid.java
     *
     * @param remotePackageName The package name from which the colors should be extracted.
     */
    fun updateRemoteColorValues(remotePackageName: String) {
        if (!isAdaptiveThemeEnabled) return
        try {
            val tempRemote = remoteCache.find { it.packageName == remotePackageName }
            if (tempRemote != null) {
                remote = tempRemote
                return
            }
            val colorPrimary: Int?
            val colorPrimaryVariant: Int?
            val colorSecondary: Int?
            val pm = packageManager ?: return
            val remoteApp = pm.getLaunchIntentForPackage(remotePackageName)?.component ?: return
            val activityInfo = pm.getActivityInfo(remoteApp, PackageManager.GET_META_DATA)
            val remoteContext = applicationContext.createPackageContext(
                remoteApp.packageName,
                Context.CONTEXT_IGNORE_SECURITY
            )
            remoteContext.setTheme(activityInfo.themeResource)
            val res = remoteContext.resources
            val attrs = intArrayOf(
                res.getIdentifier("colorPrimary", "attr", remotePackageName),
                android.R.attr.colorPrimary,
                res.getIdentifier("colorPrimaryDark", "attr", remotePackageName),
                android.R.attr.colorPrimaryDark,
                res.getIdentifier("colorPrimaryVariant", "attr", remotePackageName),
                res.getIdentifier("colorAccent", "attr", remotePackageName),
                android.R.attr.colorAccent,
                res.getIdentifier("colorSecondary", "attr", remotePackageName)
            )
            val typedValue = TypedValue()
            colorPrimary =
                getColorFromThemeAttribute(remoteContext, typedValue, attrs[0]) ?:
                getColorFromThemeAttribute(remoteContext, typedValue, attrs[1])
            colorPrimaryVariant =
                getColorFromThemeAttribute(remoteContext, typedValue, attrs[2]) ?:
                getColorFromThemeAttribute(remoteContext, typedValue, attrs[3]) ?:
                getColorFromThemeAttribute(remoteContext, typedValue, attrs[4])
            colorSecondary =
                getColorFromThemeAttribute(remoteContext, typedValue, attrs[5]) ?:
                getColorFromThemeAttribute(remoteContext, typedValue, attrs[6]) ?:
                getColorFromThemeAttribute(remoteContext, typedValue, attrs[7])
            val newRemote = RemoteColors(
                packageName = remotePackageName,
                colorPrimary = colorPrimary?.let { ThemeValue.SolidColor(it or Color.BLACK) },
                colorPrimaryVariant = colorPrimaryVariant?.let { ThemeValue.SolidColor(it or Color.BLACK) },
                colorSecondary = colorSecondary?.let { ThemeValue.SolidColor(it or Color.BLACK) }
            )
            remoteCache.add(newRemote)
            remote = newRemote
        } catch (e: Exception) {
            remote = RemoteColors.DEFAULT
            flogError(LogTopic.THEME_MANAGER) { e.toString() }
        }
        notifyCallbackReceivers()
    }

    private fun getColorFromThemeAttribute(
        context: Context, typedValue: TypedValue, @AttrRes attr: Int
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

    /**
     * Sends a theme update to the given [onThemeUpdatedListener], regardless if it is currently
     * registered or not.
     */
    fun requestThemeUpdate(onThemeUpdatedListener: OnThemeUpdatedListener): Boolean {
        onThemeUpdatedListener.onThemeUpdated(activeTheme)
        return true
    }

    @Synchronized
    fun registerOnThemeUpdatedListener(onThemeUpdatedListener: OnThemeUpdatedListener): Boolean {
        val ret = callbackReceivers.addIfAbsent(onThemeUpdatedListener)
        onThemeUpdatedListener.onThemeUpdated(activeTheme)
        return ret
    }

    @Synchronized
    fun unregisterOnThemeUpdatedListener(onThemeUpdatedListener: OnThemeUpdatedListener): Boolean {
        return callbackReceivers.remove(onThemeUpdatedListener)
    }

    @Synchronized
    fun notifyCallbackReceivers() {
        callbackReceivers.forEach {
            it.onThemeUpdated(activeTheme)
        }
    }

    fun deleteTheme(ref: FlorisRef): Result<Unit> {
        return assetManager.deleteAsset(ref)
    }

    fun loadTheme(ref: FlorisRef): Result<Theme> {
        return assetManager.loadJsonAsset(ref)
    }

    fun loadTheme(uri: Uri): Result<Theme> {
        return assetManager.loadJsonAsset(uri, THEME_MAX_SIZE)
    }

    fun writeTheme(ref: FlorisRef, theme: Theme): Result<Unit> {
        return assetManager.writeJsonAsset(ref, theme)
    }

    fun writeTheme(uri: Uri, theme: Theme): Result<Unit> {
        return assetManager.writeJsonAsset(uri, theme)
    }

    private fun evaluateActiveThemeRef(): FlorisRef {
        flogInfo(LogTopic.THEME_MANAGER) { prefs.theme.mode.toString() }
        flogInfo(LogTopic.THEME_MANAGER) { prefs.theme.dayThemeRef }
        flogInfo(LogTopic.THEME_MANAGER) { prefs.theme.nightThemeRef }
        return FlorisRef.from(
            when (prefs.theme.mode) {
                ThemeMode.ALWAYS_DAY -> {
                    isAdaptiveThemeEnabled = prefs.theme.dayThemeAdaptToApp
                    prefs.theme.dayThemeRef
                }
                ThemeMode.ALWAYS_NIGHT -> {
                    isAdaptiveThemeEnabled = prefs.theme.nightThemeAdaptToApp
                    prefs.theme.nightThemeRef
                }
                ThemeMode.FOLLOW_SYSTEM -> if (applicationContext.resources.configuration.uiMode and
                    Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
                ) {
                    isAdaptiveThemeEnabled = prefs.theme.nightThemeAdaptToApp
                    prefs.theme.nightThemeRef
                } else {
                    isAdaptiveThemeEnabled = prefs.theme.dayThemeAdaptToApp
                    prefs.theme.dayThemeRef
                }
                ThemeMode.FOLLOW_TIME -> {
                    val current = TimeUtil.currentLocalTime()
                    val sunrise = TimeUtil.decode(prefs.theme.sunriseTime)
                    val sunset = TimeUtil.decode(prefs.theme.sunsetTime)
                    if (TimeUtil.isNightTime(sunrise, sunset, current)) {
                        isAdaptiveThemeEnabled = prefs.theme.nightThemeAdaptToApp
                        prefs.theme.nightThemeRef
                    } else {
                        isAdaptiveThemeEnabled = prefs.theme.dayThemeAdaptToApp
                        prefs.theme.dayThemeRef
                    }
                }
            }
        )
    }

    private fun indexThemeRefs() {
        indexedDayThemeRefs.clear()
        indexedNightThemeRefs.clear()
        assetManager.listAssets<ThemeMetaOnly>(
            FlorisRef.assets(THEME_PATH_REL)
        ).onSuccess {
            for ((ref, themeMetaOnly) in it) {
                if (themeMetaOnly.isNightTheme) {
                    indexedNightThemeRefs[ref] = themeMetaOnly
                } else {
                    indexedDayThemeRefs[ref] = themeMetaOnly
                }
            }
        }.onFailure {
            flogError(LogTopic.THEME_MANAGER) { it.toString() }
        }
        assetManager.listAssets<ThemeMetaOnly>(
            FlorisRef.internal(THEME_PATH_REL)
        ).onSuccess {
            for ((ref, themeMetaOnly) in it) {
                if (themeMetaOnly.isNightTheme) {
                    indexedNightThemeRefs[ref] = themeMetaOnly
                } else {
                    indexedDayThemeRefs[ref] = themeMetaOnly
                }
            }
        }.onFailure {
            flogError(LogTopic.THEME_MANAGER) { it.toString() }
        }
    }

    /**
     * Creates a new inline suggestion UI bundle based on the attributes of the given [theme].
     *
     * @param context The context of the parent view/controller.
     * @param theme The theme from which the color attributes should be fetched. Defaults to [activeTheme].
     *
     * @return A bundle containing all necessary attributes for the inline suggestion views to properly display.
     */
    @SuppressLint("RestrictedApi")
    @RequiresApi(Build.VERSION_CODES.R)
    fun createInlineSuggestionUiStyleBundle(context: Context, theme: Theme = activeTheme): Bundle {
        val bgColor = theme.getAttr(Theme.Attr.SMARTBAR_BUTTON_BACKGROUND).toSolidColor().color
        val fgColor = theme.getAttr(Theme.Attr.SMARTBAR_BUTTON_FOREGROUND).toSolidColor().color
        val bgDrawableId = R.drawable.chip_background
        val stylesBuilder = UiVersions.newStylesBuilder()
        val style = InlineSuggestionUi.newStyleBuilder()
            .setSingleIconChipStyle(
                ViewStyle.Builder()
                    .setBackground(
                        Icon.createWithResource(context, bgDrawableId).setTint(bgColor)
                    )
                    .setPadding(0, 0, 0, 0)
                    .build()
            )
            .setChipStyle(
                ViewStyle.Builder()
                    .setBackground(
                        Icon.createWithResource(context, bgDrawableId).setTint(bgColor)
                    )
                    .setPadding(
                        context.resources.getDimension(R.dimen.suggestion_chip_bg_padding_start).toInt(),
                        context.resources.getDimension(R.dimen.suggestion_chip_bg_padding_top).toInt(),
                        context.resources.getDimension(R.dimen.suggestion_chip_bg_padding_end).toInt(),
                        context.resources.getDimension(R.dimen.suggestion_chip_bg_padding_bottom).toInt(),
                    )
                    .build()
            )
            .setStartIconStyle(
                ImageViewStyle.Builder()
                    .setLayoutMargin(0, 0, 0, 0)
                    .build()
            )
            .setTitleStyle(
                TextViewStyle.Builder()
                    .setLayoutMargin(
                        context.resources.getDimension(R.dimen.suggestion_chip_fg_title_margin_start).toInt(),
                        context.resources.getDimension(R.dimen.suggestion_chip_fg_title_margin_top).toInt(),
                        context.resources.getDimension(R.dimen.suggestion_chip_fg_title_margin_end).toInt(),
                        context.resources.getDimension(R.dimen.suggestion_chip_fg_title_margin_bottom).toInt(),
                    )
                    .setTextColor(fgColor)
                    .setTextSize(16f)
                    .build()
            )
            .setSubtitleStyle(
                TextViewStyle.Builder()
                    .setLayoutMargin(
                        context.resources.getDimension(R.dimen.suggestion_chip_fg_subtitle_margin_start).toInt(),
                        context.resources.getDimension(R.dimen.suggestion_chip_fg_subtitle_margin_top).toInt(),
                        context.resources.getDimension(R.dimen.suggestion_chip_fg_subtitle_margin_end).toInt(),
                        context.resources.getDimension(R.dimen.suggestion_chip_fg_subtitle_margin_bottom).toInt(),
                    )
                    .setTextColor(ColorUtils.setAlphaComponent(fgColor, 150))
                    .setTextSize(14f)
                    .build()
            )
            .setEndIconStyle(
                ImageViewStyle.Builder()
                    .setLayoutMargin(0, 0, 0, 0)
                    .build()
            )
            .build()
        stylesBuilder.addStyle(style)
        return stylesBuilder.build()
    }

    data class RemoteColors(
        val packageName: String,
        val colorPrimary: ThemeValue.SolidColor?,
        val colorPrimaryVariant: ThemeValue.SolidColor?,
        val colorSecondary: ThemeValue.SolidColor?
    ) {
        companion object {
            val DEFAULT = RemoteColors("undefined", null, null, null)
        }
    }

    /**
     * Functional interface which should be implemented by event listeners to be able to receive
     * theme updates.
     */
    fun interface OnThemeUpdatedListener {
        fun onThemeUpdated(theme: Theme)
    }
}
