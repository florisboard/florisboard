package dev.patrickgold.florisboard.lib.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import dev.patrickgold.florisboard.ime.theme.FlorisImeTheme
import dev.patrickgold.florisboard.ime.theme.FlorisImeUi
import dev.patrickgold.florisboard.lib.snygg.ui.solidColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CopyToClipboardBottomSheet(
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.(() -> Unit) -> Unit,
) {
    val modalBottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    FlorisImeTheme {
        val containerStyle = FlorisImeTheme.style.get(element = FlorisImeUi.Keyboard)
        val dragHandleStyle = FlorisImeTheme.style.get(element = FlorisImeUi.Key)
        ModalBottomSheet(
            modifier = Modifier.fillMaxWidth(),
            onDismissRequest = { onDismiss() },
            sheetState = modalBottomSheetState,
            dragHandle = {
                BottomSheetDefaults.DragHandle(
                    color = dragHandleStyle.background.solidColor(context)
                )
            },
            containerColor = containerStyle.background.solidColor(context),
            contentColor = containerStyle.foreground.solidColor(context)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
                    //.background(Color.Cyan)
                    .wrapContentHeight()
            ) {
                Column(
                    modifier = Modifier.padding(start = 8.dp, end = 8.dp)
                ) {
                    content(onDismiss)
                }
            }
        }
    }
}
