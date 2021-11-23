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

package dev.patrickgold.florisboard.ime.popup

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.ime.keyboard.Key
import dev.patrickgold.florisboard.ime.theme.FlorisImeTheme
import dev.patrickgold.florisboard.ime.theme.FlorisImeUi
import dev.patrickgold.florisboard.snygg.ui.SnyggSurface
import dev.patrickgold.florisboard.snygg.ui.solidColor
import dev.patrickgold.florisboard.snygg.ui.spSize

@Composable
fun PopupBox(
    modifier: Modifier = Modifier,
    key: Key,
    fontSizeMultiplier: Float,
    shouldIndicateExtendedPopups: Boolean,
): Unit = with(LocalDensity.current) {
    val popupStyle = FlorisImeTheme.style.get(
        element = FlorisImeUi.KeyPopup,
    )
    val fontSize = popupStyle.fontSize.spSize() * fontSizeMultiplier
    SnyggSurface(
        modifier = modifier,
        background = popupStyle.background,
        shape = popupStyle.shape,
        clip = true,
    ) {
        key.label?.let { label ->
            Text(
                modifier = Modifier
                    .wrapContentSize()
                    .align(Alignment.TopCenter)
                    .padding(top = key.visibleBounds.height.dp * 0.06f),
                text = label,
                color = popupStyle.foreground.solidColor(),
                fontSize = fontSize,
                maxLines = 1,
                softWrap = false,
            )
        }
        if (shouldIndicateExtendedPopups) {
            Icon(
                modifier = Modifier
                    .requiredSize(fontSize.toDp() * 0.65f)
                    .align(Alignment.CenterEnd),
                painter = painterResource(R.drawable.ic_more_horiz),
                contentDescription = null,
                tint = popupStyle.foreground.solidColor(),
            )
        }
    }
}
