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
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.florisboard.lib.snygg.SnyggQueryAttributes
import org.florisboard.lib.snygg.SnyggSelector
import org.florisboard.lib.snygg.SnyggStylesheet

/**
 * Simple layout composable that places its children in a horizontal sequence.
 *
 * This composable infers its style from the current [SnyggTheme][org.florisboard.lib.snygg.SnyggTheme], which is
 * required to be provided by [ProvideSnyggTheme].
 *
 * @param elementName The name of this element. If `null` the style will be inherited from the parent element.
 * @param attributes The attributes of the element used to refine the query.
 * @param selector A specific SnyggSelector to query the style for.
 * @param modifier The modifier to be applied to the Row.
 * @param onClick The action that is executed on click
 * @param enabled Weather the chip is enabled
 * @param imageVector Icon
 * @param text Text
 *
 * @since 0.5.0-alpha01
 *
 * @see [SnyggRow]
 * @see [SnyggText]
 * @see [SnyggIcon]
 */
@Composable
fun SnyggChip(
    elementName: String? = null,
    attributes: SnyggQueryAttributes = emptyMap(),
    selector: SnyggSelector? = null,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    enabled: Boolean = true,
    imageVector: ImageVector? = null,
    text: String,
) {
    SnyggChip(
        elementName,
        attributes,
        selector,
        modifier,
        onClick,
        enabled,
        icon = if (imageVector == null) null else ({
            SnyggIcon(
                elementName = "$elementName-icon",
                attributes = attributes,
                selector = selector,
                imageVector = imageVector,
            )
        }),
        text = {
            SnyggText(
                elementName = "$elementName-text",
                attributes = attributes,
                selector = selector,
                text = text,
            )
        },
    )
}

@Composable
internal fun SnyggChip(
    elementName: String? = null,
    attributes: SnyggQueryAttributes = emptyMap(),
    selector: SnyggSelector? = null,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    enabled: Boolean = true,
    icon: (@Composable () -> Unit)? = null,
    text: (@Composable () -> Unit)? = null,
) {
    SnyggRow(
        elementName = elementName,
        attributes = attributes,
        selector = selector,
        modifier = modifier,
        clickAndSemanticsModifier = Modifier
            .clickable(
                interactionSource = null,
                indication = ripple(),
                enabled = enabled,
                onClick = onClick,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        icon?.invoke()
        text?.invoke()
    }
}

@Preview
@Composable
private fun SimpleSnyggChip() {
    val stylesheet = SnyggStylesheet.v2 {
        "preview-row" {
            background = rgbaColor(255, 255, 255)
            foreground = rgbaColor(255, 0, 0)
            padding = padding(10.dp)
        }
        "chip" {
            background = rgbaColor(255, 0, 0)
            foreground = rgbaColor(255, 255, 255)
            shape = roundedCornerShape(20, 20, 20, 20)
        }
    }
    val theme = rememberSnyggTheme(stylesheet)

    ProvideSnyggTheme(theme) {
        SnyggRow("preview-row") {
            SnyggChip(
                elementName = "chip",
                onClick = {},
                text = "Hello",
            )
        }
    }
}
