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

package dev.patrickgold.florisboard.ime.keyboard

import android.content.res.Resources
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.app.prefs.florisPreferenceModel
import dev.patrickgold.florisboard.common.ViewUtils
import dev.patrickgold.florisboard.common.isOrientationLandscape
import dev.patrickgold.florisboard.ime.onehanded.OneHandedMode
import dev.patrickgold.jetpref.datastore.model.PreferenceData
import dev.patrickgold.jetpref.datastore.model.PreferenceObserver
import dev.patrickgold.jetpref.datastore.model.observeAsState

@Composable
private fun <V : Any, R : Any> PreferenceData<V>.observeAsState2(transform: (V) -> R): State<R> {
    val lifecycleOwner = LocalLifecycleOwner.current
    val state = remember(key) { mutableStateOf(transform(get())) }
    DisposableEffect(this, lifecycleOwner) {
        val observer = PreferenceObserver<V> { newValue -> state.value = transform(newValue) }
        observe(lifecycleOwner, observer)
        onDispose { removeObserver(observer) }
    }
    return state
}

val LocalKeyboardRowBaseHeight = staticCompositionLocalOf { 65.dp }

@Composable
fun ProvideKeyboardRowBaseHeight(content: @Composable () -> Unit) {
    val prefs by florisPreferenceModel()
    val resources = LocalContext.current.resources
    val configuration = LocalConfiguration.current

    val heightFactorPortrait by prefs.keyboard.heightFactorPortrait.observeAsState2 { it.toFloat() / 100f }
    val heightFactorLandscape by prefs.keyboard.heightFactorLandscape.observeAsState2 { it.toFloat() / 100f }
    val oneHandedMode by prefs.keyboard.oneHandedMode.observeAsState()
    val oneHandedModeScaleFactor by prefs.keyboard.oneHandedModeScaleFactor.observeAsState2 { it.toFloat() / 100f }

    val baseRowHeight = remember(
        configuration, resources, heightFactorPortrait, heightFactorLandscape,
        oneHandedMode, oneHandedModeScaleFactor
    ) {
        calcInputViewHeight(resources) * when {
            configuration.isOrientationLandscape() -> heightFactorLandscape
            else -> heightFactorPortrait * (if (oneHandedMode != OneHandedMode.OFF) oneHandedModeScaleFactor else 1f)
        }
    }

    CompositionLocalProvider(LocalKeyboardRowBaseHeight provides ViewUtils.px2dp(baseRowHeight).dp) {
        content()
    }
}

/**
 * Calculates the input view height based on the current screen dimensions and the auto
 * selected dimension values.
 *
 * This method and the fraction values have been inspired by [OpenBoard](https://github.com/dslul/openboard)
 * but are not 1:1 the same. This implementation differs from the
 * [original](https://github.com/dslul/openboard/blob/90ae4c8aec034a8935e1fd02b441be25c7dba6ce/app/src/main/java/org/dslul/openboard/inputmethod/latin/utils/ResourceUtils.java)
 * by calculating the average of the min and max height values, then taking at least the input
 * view base height and return this resulting value.
 */
private fun calcInputViewHeight(resources: Resources): Float {
    val dm = resources.displayMetrics
    val minBaseSize = when {
        resources.configuration.isOrientationLandscape() -> resources.getFraction(
            R.fraction.inputView_minHeightFraction, dm.heightPixels, dm.heightPixels
        )
        else -> resources.getFraction(
            R.fraction.inputView_minHeightFraction, dm.widthPixels, dm.widthPixels
        )
    }
    val maxBaseSize = resources.getFraction(
        R.fraction.inputView_maxHeightFraction, dm.heightPixels, dm.heightPixels
    )
    return ((minBaseSize + maxBaseSize) / 2.0f).coerceAtLeast(
        resources.getDimension(R.dimen.inputView_baseHeight)
    ) * 0.21f
}
