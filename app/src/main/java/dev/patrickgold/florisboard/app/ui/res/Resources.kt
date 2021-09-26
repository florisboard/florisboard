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

package dev.patrickgold.florisboard.app.ui.res

import android.content.Context
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.res.stringResource
import dev.patrickgold.florisboard.R

private val LocalResourcesContext = staticCompositionLocalOf<Context> {
    error("resources context not initialized!!")
}

private val LocalAppNameString = staticCompositionLocalOf {
    "FlorisBoard"
}

@Composable
fun ProvideLocalizedResources(
    resourcesContext: Context,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalResourcesContext provides resourcesContext,
        LocalAppNameString provides stringResource(R.string.floris_app_name),
    ) {
        content()
    }
}

@Composable
fun stringRes(
    @StringRes id: Int,
    vararg args: Pair<String, Any?>,
): String {
    val string = LocalResourcesContext.current.resources
        .getString(id)
    return formatString(string, args)
}

@Composable
fun pluralsRes(
    @PluralsRes id: Int,
    quantity: Int,
    vararg args: Pair<String, Any?>,
): String {
    val string = LocalResourcesContext.current.resources
        .getQuantityString(id, quantity)
    return formatString(string, args)
}

@Composable
private fun formatString(
    string: String,
    args: Array<out Pair<String, Any?>>,
): String {
    var ret = string
    ret = ret.replace("{app_name}", LocalAppNameString.current)
    for (arg in args) {
        ret = ret.replace("{${arg.first}}", arg.second.toString())
    }
    return ret
}
