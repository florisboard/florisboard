/*
 * Copyright (C) 2026 The FlorisBoard Contributors
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

package dev.patrickgold.florisboard.ime.keyboard3.ui

import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.patrickgold.florisboard.ime.theme.FlorisImeUi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import org.florisboard.lib.snygg.SnyggQueryAttributes
import org.florisboard.lib.snygg.ui.SnyggBox
import org.florisboard.lib.snygg.ui.SnyggIcon

val GlobalStateNumPopupsShowing = MutableStateFlow(0)

@Composable
fun LongPressBox(
    longPress: LongPress,
    modifier: Modifier = Modifier,
    attributes: SnyggQueryAttributes = emptyMap(),
) {
    DisposableEffect(Unit) {
        GlobalStateNumPopupsShowing.update { it + 1 }
        onDispose {
            GlobalStateNumPopupsShowing.update { it - 1 }
        }
    }

    if (longPress.shouldShowSimplePopup()) {
        SnyggBox(
            elementName = FlorisImeUi.KeyPopupBox.elementName,
            attributes = attributes,
            modifier = modifier.layoutNormalized(longPress.simpleBounds),
        ) {
            Display3(
                display = longPress.simpleLabel,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 10.dp),
            )
            if (longPress.simpleIndicateExtended) {
                SnyggIcon(
                    elementName = FlorisImeUi.KeyPopupExtendedIndicator.elementName,
                    attributes = attributes,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .offset(y = (-8).dp),
                    imageVector = Icons.Default.MoreHoriz,
                )
            }
        }
    }

    if (longPress.shouldShowExtendedPopup()) {
        SnyggBox(
            elementName = FlorisImeUi.KeyPopupBox.elementName,
            attributes = attributes,
            modifier = modifier.layoutNormalized(longPress.extendedBounds),
        ) {
            // TODO
        }
    }
}
