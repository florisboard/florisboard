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

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp

@Composable
fun FlorisButtonBar(content: @Composable FlorisButtonBarScope.() -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colors.background,
        elevation = 8.dp,
    ) {
        Column {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp),
                color = MaterialTheme.colors.surface,
            ) {}
            Row(
                modifier = Modifier
                    .padding(top = 4.dp, bottom = 4.dp, start = 0.dp, end = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val scope = FlorisButtonBarScope(this)
                content(scope)
            }
        }
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
        Button(
            modifier = modifier.padding(start = 16.dp),
            enabled = enabled,
            colors = ButtonDefaults.buttonColors(
                backgroundColor = MaterialTheme.colors.primary,
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
            Text(text = text)
        }
    }

    @Composable
    fun ButtonBarTextButton(
        text: String,
        modifier: Modifier = Modifier,
        enabled: Boolean = true,
        @DrawableRes iconId: Int? = null,
        onClick: () -> Unit,
    ) {
        TextButton(
            modifier = modifier.padding(start = 16.dp),
            enabled = enabled,
            colors = ButtonDefaults.textButtonColors(
                contentColor = MaterialTheme.colors.primary,
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
            Text(text = text)
        }
    }

    @Composable
    fun ButtonBarSpacer() {
        Spacer(modifier = Modifier.weight(1f))
    }
}
