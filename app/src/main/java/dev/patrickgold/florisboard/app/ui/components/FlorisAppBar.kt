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

package dev.patrickgold.florisboard.app.ui.components

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.app.LocalNavController

@Composable
fun FlorisAppBar(
    title: String,
    backArrowVisible: Boolean,
    actions: @Composable RowScope.() -> Unit = { }
) {
    TopAppBar(
        navigationIcon = backNavBtn(backArrowVisible),
        title = { Text(text = title) },
        actions = actions,
        backgroundColor = Color.Transparent,
    )
}

@Composable
private fun backNavBtn(backArrowVisible: Boolean): @Composable (() -> Unit)? {
    if (!backArrowVisible) return null
    val navController = LocalNavController.current
    return {
        IconButton(
            onClick = { navController.popBackStack() }
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_arrow_back),
                contentDescription = "Back",
            )
        }
    }
}
