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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.patrickgold.florisboard.ime.theme.FlorisImeTheme
import dev.patrickgold.florisboard.ime.theme.FlorisImeUi
import dev.patrickgold.florisboard.lib.android.AndroidClipboardManager
import dev.patrickgold.florisboard.lib.android.systemService
import dev.patrickgold.florisboard.lib.compose.CopyToClipboardBottomSheet
import dev.patrickgold.florisboard.lib.compose.ProvideLocalizedResources
import dev.patrickgold.florisboard.lib.compose.stringRes
import dev.patrickgold.florisboard.lib.snygg.ui.solidColor


class FlorisCopyToClipboardActivity : ComponentActivity() {

    enum class CopyToClipboardError {
        UNKNOWN_ERROR,
        ANDROID_VERSION_TO_OLD_ERROR,
        TYPE_NOT_SUPPORTED_ERROR;

        @Composable
        fun showError() {
            val textStyle = FlorisImeTheme.style.get(element = FlorisImeUi.GlideTrail)
            val context = LocalContext.current

            val textId = when (this) {
                UNKNOWN_ERROR -> R.string.send_to_clipboard__unknown_error
                TYPE_NOT_SUPPORTED_ERROR -> R.string.send_to_clipboard__type_not_supported_error
                ANDROID_VERSION_TO_OLD_ERROR -> R.string.send_to_clipboard__android_version_to_old_error
            }
            Text(
                text = stringRes(textId),
                color = textStyle.foreground.solidColor(context = context),
                fontSize = 18.sp
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val systemClipboardManager = this.systemService(AndroidClipboardManager::class)
        val type = intent.type
        val action = intent.action
        var error: CopyToClipboardError? = null
        var bitmap: Bitmap? = null
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
            ProvideLocalizedResources(this) {
                CopyToClipboardBottomSheet(
                    onDismiss = {
                        finish()
                    }
                ) {
                    val textStyle = FlorisImeTheme.style.get(element = FlorisImeUi.KeyHint)
                    val buttonContentStyle = FlorisImeTheme.style.get(element = FlorisImeUi.GlideTrail)
                    val context = LocalContext.current

                    error?.showError()
                    bitmap?.let {
                        Text(
                            text = stringRes(id = R.string.send_to_clipboard__description__copied_image_to_clipboard),
                            color = textStyle.foreground.solidColor(context = context)
                        )
                        Spacer(modifier = Modifier.size(8.dp))
                        Image(
                            modifier = Modifier.fillMaxWidth(),
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = null
                        )
                    }
                    Button(
                        onClick = { it() },
                        modifier = Modifier.align(alignment = Alignment.End),
                        colors = ButtonDefaults.textButtonColors(
                            //containerColor = buttonContainer.background.solidColor(context = context),
                            contentColor = buttonContentStyle.foreground.solidColor(context = context),
                        )
                    ) {
                        Text(text = stringRes(id = R.string.action__ok))
                    }
                }
            }
        }
    }
}
