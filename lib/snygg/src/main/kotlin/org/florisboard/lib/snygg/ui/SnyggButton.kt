/*
 * Copyright (C) 2025 The FlorisBoard Contributors
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

package org.florisboard.lib.snygg.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import org.florisboard.lib.snygg.SnyggQueryAttributes
import org.florisboard.lib.snygg.SnyggSelector

@Composable
fun SnyggButton(
    elementName: String? = null,
    attributes: SnyggQueryAttributes = emptyMap(),
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: Painter? = null,
    text: String,
    enabled: Boolean = true,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val selector = if (enabled) SnyggSelector.NONE else SnyggSelector.DISABLED
    SnyggBox(
        elementName = elementName,
        attributes = attributes,
        selector = selector,
        modifier = modifier.semantics { role = Role.Button },
        clickAndSemanticsModifier = Modifier
            .clickable(interactionSource, ripple(), enabled, null, Role.Button, onClick),
    ) {
        Row(
            modifier = Modifier.padding(ButtonDefaults.ContentPadding),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) {
                SnyggIcon(
                    modifier = Modifier
                        .padding(end = ButtonDefaults.IconSpacing)
                        .size(ButtonDefaults.IconSize),
                    painter = icon,
                    contentDescription = null,
                )
            }
            SnyggText(text = text, modifier = modifier)
        }
    }
}
