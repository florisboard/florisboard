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
import dev.patrickgold.florisboard.ime.keyboard.FlorisImeSizing
import dev.patrickgold.florisboard.keyboardManager
import dev.patrickgold.florisboard.lib.util.launchUrl
import org.florisboard.lib.snygg.SnyggPropertySet
import org.florisboard.lib.snygg.ui.SnyggButton
import org.florisboard.lib.snygg.ui.SnyggSurface
import org.florisboard.lib.snygg.ui.solidColor
import org.florisboard.lib.snygg.value.SnyggRoundedCornerDpShapeValue
import org.florisboard.lib.snygg.value.SnyggSolidColorValue

@Composable
fun HowDidWeGetHere() {
    val context = LocalContext.current
    val keyboardManager by context.keyboardManager()

    val style = SnyggPropertySet(mapOf(
        "background" to SnyggSolidColorValue(Color.Yellow),
        "foreground" to SnyggSolidColorValue(Color.Black),
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
    }
}
