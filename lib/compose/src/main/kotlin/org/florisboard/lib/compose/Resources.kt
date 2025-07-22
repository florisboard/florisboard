/*
 * Copyright (C) 2021-2025 The FlorisBoard Contributors
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

package org.florisboard.lib.compose

import android.content.Context
import android.view.View
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.LayoutDirection
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.*
import org.florisboard.lib.kotlin.CurlyArg
import org.florisboard.lib.kotlin.curlyFormat

private val LocalResourcesContext = staticCompositionLocalOf<Context> {
    error("resources context not initialized!!")
}

private val LocalAppNameString = staticCompositionLocalOf {
    "FlorisBoard"
}

val LocalLocalizedDateTimeFormatter = staticCompositionLocalOf {
    DateTimeFormatter
        .ofLocalizedDateTime(FormatStyle.MEDIUM)
        .withLocale(Locale.ROOT)
        .withZone(ZoneId.systemDefault())
}

@Composable
fun ProvideLocalizedResources(
    resourcesContext: Context,
    @StringRes appName: Int,
    forceLayoutDirection: LayoutDirection? = null,
    content: @Composable () -> Unit,
) {
    val layoutDirection = forceLayoutDirection ?: when (resourcesContext.resources.configuration.layoutDirection) {
        View.LAYOUT_DIRECTION_LTR -> LayoutDirection.Ltr
        View.LAYOUT_DIRECTION_RTL -> LayoutDirection.Rtl
        else -> error("Given configuration specifies invalid layout direction!")
    }
    val localeList = resourcesContext.resources.configuration.locales
    val dateTimeFormatter = remember(resourcesContext) {
        DateTimeFormatter
            .ofLocalizedDateTime(FormatStyle.MEDIUM)
            .withLocale(if (localeList.isEmpty) Locale.getDefault() else localeList.get(0))
            .withZone(ZoneId.systemDefault())
    }
    CompositionLocalProvider(
        LocalResourcesContext provides resourcesContext,
        LocalLocalizedDateTimeFormatter provides dateTimeFormatter,
        LocalLayoutDirection provides layoutDirection,
        LocalAppNameString provides stringResource(appName),
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
