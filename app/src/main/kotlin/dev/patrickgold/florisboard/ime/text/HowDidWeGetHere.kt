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

package dev.patrickgold.florisboard.ime.text

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import dev.patrickgold.florisboard.extensionManager
import dev.patrickgold.florisboard.ime.keyboard.FlorisImeSizing
import dev.patrickgold.florisboard.keyboardManager
import dev.patrickgold.florisboard.lib.util.launchUrl
import org.florisboard.lib.snygg.SnyggPropertySet
import org.florisboard.lib.snygg.ui.SnyggButton
import org.florisboard.lib.snygg.ui.SnyggSurface
import org.florisboard.lib.snygg.value.SnyggRoundedCornerDpShapeValue
import org.florisboard.lib.snygg.value.SnyggStaticColorValue

@Composable
fun HowDidWeGetHere() {
    val context = LocalContext.current
    val extensionManager by context.extensionManager()
    val keyboardManager by context.keyboardManager()

    /*val style = SnyggPropertySet(mapOf(
        "background" to SnyggStaticColorValue(Color.Yellow),
        "foreground" to SnyggStaticColorValue(Color.Black),
        "shape" to SnyggRoundedCornerDpShapeValue(16.dp, 16.dp, 16.dp, 16.dp, RoundedCornerShape(16.dp)),
    ))

    @Composable
    fun ColoredText(text: String) {
        Text(
            text = text,
            color = style.foreground.solidColor(context),
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(FlorisImeSizing.keyboardUiHeight())
            .padding(8.dp),
    ) {
        SnyggSurface(style = style) {
            Column(modifier = Modifier.padding(8.dp)) {
                ColoredText(text = "Challenge Complete! - How did we get here?\n")
                ColoredText(text = "You landed in a state which shouldn't be reachable, possibly related to the \"All keys invisible\" bug. Please report this bug and the steps to reproduce to the devs using the button below. Thanks!")
                Row {
                    SnyggButton(
                        onClick = {
                            keyboardManager.activeState.rawValue = 0u
                            extensionManager.init()
                        },
                        text = "Try reset keyboard",
                        style = style,
                    )
                    SnyggButton(
                        onClick = {
                            context.launchUrl("https://github.com/florisboard/florisboard/issues/2362")
                        },
                        text = "Report bug to devs",
                        style = style,
                    )
                }
            }
        }
    }*/
}
