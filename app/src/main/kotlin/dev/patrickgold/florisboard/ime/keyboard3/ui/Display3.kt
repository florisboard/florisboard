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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import org.florisboard.lib.snygg.ui.SnyggText
import org.k3lp.lib.text.K3Descriptor
import org.k3lp.lib.text.K3String
import org.k3lp.lib.text.K3StringOrDescriptor

@Composable
fun Display3(
    display: K3StringOrDescriptor,
    modifier: Modifier = Modifier,
    elementName: String? = null,
) {
    when (display) {
        is K3String -> {
            val text = remember(display) { display.toText() }
            SnyggText(
                text = text,
                modifier = modifier,
                elementName = elementName,
            )
        }
        is K3Descriptor -> {
            Icon3(
                value = display,
                modifier = modifier,
                elementName = elementName,
            )
        }
    }
}
