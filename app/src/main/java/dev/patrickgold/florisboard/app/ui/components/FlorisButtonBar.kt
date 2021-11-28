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

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp

@Composable
fun FlorisButtonBar(content: @Composable FlorisButtonBarScope.() -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colors.primary)
            .padding(vertical = 4.dp, horizontal = 16.dp),
    ) {
        val scope = FlorisButtonBarScope(this)
        content(scope)
    }
}

class FlorisButtonBarScope(rowScope: RowScope) : RowScope by rowScope {
    @Composable
    fun ButtonBarButton(
        text: String,
        modifier: Modifier = Modifier,
        enabled: Boolean = true,
        @DrawableRes iconId: Int? = null,
        onClick: () -> Unit,
    ) {
        TextButton(
            modifier = modifier,
            enabled = enabled,
            colors = ButtonDefaults.buttonColors(
                contentColor = MaterialTheme.colors.onPrimary,
            ),
            onClick = onClick,
        ) {
            if (iconId != null) {
                Icon(
                    modifier = Modifier.padding(end = 8.dp),
                    painter = painterResource(iconId),
                    contentDescription = null,
                )
            }
            Text(text = text.uppercase())
        }
    }

    @Composable
    fun ButtonBarSpacer() {
        Spacer(modifier = Modifier.weight(1f))
    }
}
