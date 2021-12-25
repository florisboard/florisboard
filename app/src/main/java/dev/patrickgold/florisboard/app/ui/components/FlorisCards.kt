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

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.patrickgold.florisboard.R

private val IconRequiredSize = 24.dp
private val IconEndPadding = 8.dp

private val CardContentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)

private val OutlinedBoxShape = RoundedCornerShape(8.dp)

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun FlorisSimpleCard(
    modifier: Modifier = Modifier,
    text: String,
    secondaryText: String? = null,
    backgroundColor: Color = MaterialTheme.colors.surface,
    contentColor: Color = contentColorFor(backgroundColor),
    contentPadding: PaddingValues = CardContentPadding,
    icon: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    Card(
        onClick = onClick ?: { },
        enabled = onClick != null,
        modifier = modifier.fillMaxWidth(),
        backgroundColor = backgroundColor,
        contentColor = contentColor,
    ) {
        Row(
            modifier = Modifier.padding(contentPadding),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) {
                icon()
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = if (icon != null) 16.dp else 0.dp),
            ) {
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = text,
                    style = MaterialTheme.typography.subtitle1,
                )
                if (secondaryText != null) {
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        text = secondaryText,
                        style = MaterialTheme.typography.subtitle2,
                    )
                }
            }
        }
    }
}

@Composable
fun FlorisErrorCard(
    text: String,
    modifier: Modifier = Modifier,
    showIcon: Boolean = true,
    contentPadding: PaddingValues = CardContentPadding,
    onClick: (() -> Unit)? = null,
) {
    FlorisSimpleCard(
        modifier = modifier,
        backgroundColor = Color(0xFFCC0000),
        contentColor = Color.White,
        onClick = onClick,
        icon = if (showIcon) ({ Icon(
            modifier = Modifier
                .padding(end = IconEndPadding)
                .requiredSize(IconRequiredSize),
            painter = painterResource(R.drawable.ic_error_outline),
            contentDescription = null,
        ) }) else null,
        text = text,
        contentPadding = contentPadding,
    )
}

@Composable
fun FlorisWarningCard(
    text: String,
    modifier: Modifier = Modifier,
    showIcon: Boolean = true,
    contentPadding: PaddingValues = CardContentPadding,
    onClick: (() -> Unit)? = null,
) {
    FlorisSimpleCard(
        modifier = modifier,
        backgroundColor = Color.Yellow,
        contentColor = Color.Black,
        onClick = onClick,
        icon = if (showIcon) ({ Icon(
            modifier = Modifier
                .padding(end = IconEndPadding)
                .requiredSize(IconRequiredSize),
            painter = painterResource(R.drawable.ic_warning_outline),
            contentDescription = null,
        ) }) else null,
        text = text,
        contentPadding = contentPadding,
    )
}

@Composable
fun FlorisInfoCard(
    text: String,
    modifier: Modifier = Modifier,
    showIcon: Boolean = true,
    contentPadding: PaddingValues = CardContentPadding,
    onClick: (() -> Unit)? = null,
) {
    FlorisSimpleCard(
        modifier = modifier,
        onClick = onClick,
        icon = if (showIcon) ({ Icon(
            modifier = Modifier
                .padding(end = IconEndPadding)
                .requiredSize(IconRequiredSize),
            painter = painterResource(R.drawable.ic_info),
            contentDescription = null,
        ) }) else null,
        text = text,
        contentPadding = contentPadding,
    )
}

@Composable
fun FlorisOutlinedBox(
    modifier: Modifier = Modifier,
    title: (@Composable () -> Unit)? = null,
    onTitleClick: (() -> Unit)? = null,
    borderWidth: Dp = 1.dp,
    borderColor: Color = MaterialTheme.colors.onSurface.copy(alpha = 0.12f),
    shape: Shape = OutlinedBoxShape,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .padding(top = if (title != null) 11.dp else 0.dp),
    ) {
        Box(
            modifier = Modifier
                .border(borderWidth, borderColor, shape)
                .clip(shape)
                .padding(top = if (title != null) 11.dp else 0.dp),
            content = content,
        )
        if (title != null) {
            Box(
                modifier = Modifier
                    .height(23.dp)
                    .offset(x = 10.dp, y = (-12).dp)
                    .background(MaterialTheme.colors.background)
                    .rippleClickable(enabled = onTitleClick != null) {
                        onTitleClick!!()
                    }
                    .padding(horizontal = 6.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                title()
            }
        }
    }
}
