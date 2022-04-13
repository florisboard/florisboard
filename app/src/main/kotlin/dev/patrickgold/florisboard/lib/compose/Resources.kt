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

package dev.patrickgold.florisboard.lib.compose

import android.content.Context
import android.view.View
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.LayoutDirection
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.lib.kotlin.CurlyArg
import dev.patrickgold.florisboard.lib.kotlin.curlyFormat

private val LocalResourcesContext = staticCompositionLocalOf<Context> {
    error("resources context not initialized!!")
}

private val LocalAppNameString = staticCompositionLocalOf {
    "FlorisBoard"
}

@Composable
fun ProvideLocalizedResources(
    resourcesContext: Context,
    forceLayoutDirection: LayoutDirection? = null,
    content: @Composable () -> Unit,
) {
    val layoutDirection = forceLayoutDirection ?: when (resourcesContext.resources.configuration.layoutDirection) {
        View.LAYOUT_DIRECTION_LTR -> LayoutDirection.Ltr
        View.LAYOUT_DIRECTION_RTL -> LayoutDirection.Rtl
        else -> error("Given configuration specifies invalid layout direction!")
    }
    CompositionLocalProvider(
        LocalResourcesContext provides resourcesContext,
        LocalLayoutDirection provides layoutDirection,
        LocalAppNameString provides stringResource(R.string.floris_app_name),
    ) {
        content()
    }
}

@Composable
fun stringRes(
    @StringRes id: Int,
    vararg args: CurlyArg,
): String {
    val string = LocalResourcesContext.current.resources
        .getString(id)
    return formatString(string, args)
}

@Composable
fun pluralsRes(
    @PluralsRes id: Int,
    quantity: Int,
    vararg args: CurlyArg,
): String {
    val string = LocalResourcesContext.current.resources
        .getQuantityString(id, quantity)
    return formatString(string, args)
}

@Composable
private fun formatString(
    string: String,
    args: Array<out CurlyArg>,
): String {
    return string.curlyFormat(
        "app_name" to LocalAppNameString.current,
        *args
    )
}
