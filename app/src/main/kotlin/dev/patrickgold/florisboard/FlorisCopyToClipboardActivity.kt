package dev.patrickgold.florisboard

import android.content.ClipData
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material.BottomSheetValue
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.SwipeableDefaults
import androidx.compose.material.rememberSwipeableState
import androidx.compose.material.swipeable
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import dev.patrickgold.florisboard.ime.sheet.BottomSheetHostUi
import dev.patrickgold.florisboard.ime.theme.FlorisImeTheme
import dev.patrickgold.florisboard.ime.theme.FlorisImeUi
import dev.patrickgold.florisboard.lib.android.AndroidClipboardManager
import dev.patrickgold.florisboard.lib.android.stringRes
import dev.patrickgold.florisboard.lib.android.systemService
import dev.patrickgold.florisboard.lib.compose.ProvideLocalizedResources
import dev.patrickgold.florisboard.lib.compose.stringRes
import dev.patrickgold.florisboard.lib.snygg.ui.snyggBackground
import dev.patrickgold.florisboard.lib.snygg.ui.snyggClip
import dev.patrickgold.florisboard.lib.snygg.ui.solidColor
import dev.patrickgold.florisboard.lib.snygg.ui.spSize
import kotlin.math.roundToInt

class FlorisCopyToClipboardActivity : ComponentActivity() {
    private var error: CopyToClipboardError? = null
    private var bitmap: Bitmap? = null

    internal enum class CopyToClipboardError {
        UNKNOWN_ERROR,
        ANDROID_VERSION_TO_OLD_ERROR,
        TYPE_NOT_SUPPORTED_ERROR;

        @Composable
        fun showError(): String {
            val textId = when (this) {
                UNKNOWN_ERROR -> R.string.send_to_clipboard__unknown_error
                TYPE_NOT_SUPPORTED_ERROR -> R.string.send_to_clipboard__type_not_supported_error
                ANDROID_VERSION_TO_OLD_ERROR -> R.string.send_to_clipboard__android_version_to_old_error
            }
            return stringRes(id = textId)
        }
    }

    override fun onPause() {
        finish()
        super.onPause()
    }

    @OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val systemClipboardManager = this.systemService(AndroidClipboardManager::class)
        val type = intent.type
        val action = intent.action

        if (Intent.ACTION_SEND != action || type == null) {
            error = CopyToClipboardError.UNKNOWN_ERROR
        } else {
            if (type.startsWith("image/")) {
                val hasExtraStream = intent.hasExtra(Intent.EXTRA_STREAM)
                if (!hasExtraStream) {
                    error = CopyToClipboardError.TYPE_NOT_SUPPORTED_ERROR
                } else {
                    // pasting images via virtual keyboard only available since Android 7.1 (API 25)
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1) {
                        error = CopyToClipboardError.ANDROID_VERSION_TO_OLD_ERROR
                    } else {
                        val uri: Uri? =
                            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                                intent.getParcelableExtra(Intent.EXTRA_STREAM)
                            } else {
                                intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
                            }
                        val clip = ClipData.newUri(contentResolver, "image", uri)
                        systemClipboardManager.setPrimaryClip(clip)
                        bitmap = MediaStore.Images.Media.getBitmap(this.contentResolver, uri)
                    }
                }
            } else {
                error = CopyToClipboardError.TYPE_NOT_SUPPORTED_ERROR
            }
        }

        setContent {
            ProvideLocalizedResources(this, forceLayoutDirection = LayoutDirection.Ltr) {
                FlorisImeTheme {
                    BottomSheetHostUi(isShowing = true, onHide = { finish() }) {
                        val panelStyle = FlorisImeTheme.style.get(FlorisImeUi.SmartbarActionsEditor)
                        val headerStyle = FlorisImeTheme.style.get(FlorisImeUi.SmartbarActionsEditorHeader)
                        val subheaderStyle = FlorisImeTheme.style.get(FlorisImeUi.SmartbarActionsEditorSubheader)
                        val context = LocalContext.current
                        Swipable {
                            Column(
                                modifier = Modifier
                                    .snyggBackground(
                                        context,
                                        panelStyle,
                                        fallbackColor = FlorisImeTheme.fallbackSurfaceColor()
                                    )
                                    .snyggClip(panelStyle)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .snyggBackground(context, headerStyle),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Spacer(modifier = Modifier.weight(1F))
                                    BottomSheetDefaults.DragHandle(
                                        color = headerStyle.foreground.solidColor(
                                            context,
                                            default = FlorisImeTheme.fallbackContentColor()
                                        ),
                                    )
                                    Spacer(modifier = Modifier.weight(1F))
                                }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .snyggBackground(context, headerStyle),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        text = error?.showError()
                                            ?: bitmap?.let { stringRes(id = R.string.send_to_clipboard__description__copied_image_to_clipboard) }
                                            ?: stringRes(R.string.send_to_clipboard__unknown_error),
                                        color = headerStyle.foreground.solidColor(
                                            context,
                                            default = FlorisImeTheme.fallbackContentColor()
                                        ),
                                        fontSize = headerStyle.fontSize.spSize(),
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.weight(1f),
                                    )
                                    Spacer(Modifier.height(48.dp))
                                }
                                bitmap?.let {
                                    Image(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(8.dp),
                                        bitmap = bitmap!!.asImageBitmap(),
                                        contentDescription = null
                                    )
                                }
                                Button(
                                    onClick = { finish() },
                                    modifier = Modifier.align(alignment = Alignment.End),
                                    colors = ButtonDefaults.textButtonColors(
                                        //containerColor = buttonContainer.background.solidColor(context = context),
                                        contentColor = subheaderStyle.foreground.solidColor(context = context),
                                    )
                                ) {
                                    Text(text = stringRes(id = R.string.action__ok))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @OptIn(ExperimentalMaterialApi::class)
    @Composable
    internal fun Swipable(
        content: @Composable () -> Unit
    ) {
        val swipeableState = rememberSwipeableState(
            initialValue = BottomSheetValue.Expanded,
            confirmStateChange = {
                if (it == BottomSheetValue.Collapsed) {
                    finish()
                }
                true
            }
        )
        BoxWithConstraints {
            val constraintsScope = this
            val maxHeight = with(LocalDensity.current) {
                constraintsScope.maxHeight.toPx()
            }
            Box(
                modifier = Modifier
                    .swipeable(
                        state = swipeableState,
                        orientation = Orientation.Vertical,
                        anchors = mapOf(
                            0f to BottomSheetValue.Expanded,
                            maxHeight to BottomSheetValue.Collapsed,
                        ),
                        resistance = SwipeableDefaults.resistanceConfig(
                            anchors = setOf(0f, maxHeight),
                            factorAtMin = 0F
                        )
                    )
                    .offset {
                        IntOffset(
                            x = 0,
                            y = swipeableState.offset.value.roundToInt()
                        )
                    }
            ) {
                content()
            }
        }
    }
}



