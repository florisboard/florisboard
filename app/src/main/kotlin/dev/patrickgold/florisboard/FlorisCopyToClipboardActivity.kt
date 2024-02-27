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
import dev.patrickgold.florisboard.lib.snygg.ui.solidColor


class FlorisCopyToClipboardActivity : ComponentActivity() {

    enum class CopyToClipboardError {
        UNKNOWN_ERROR,
        TO_OLD_ANDROID_ERROR,
        TYPE_NOT_SUPPORTED_ERROR;


        @Composable
        fun showError() {
            val textStyle = FlorisImeTheme.style.get(element = FlorisImeUi.GlideTrail)
            val context = LocalContext.current

            val text = when (this) {
                UNKNOWN_ERROR -> "An unknown Error occured. Pleas try again!"
                TYPE_NOT_SUPPORTED_ERROR -> "This media type is not supported."
                TO_OLD_ANDROID_ERROR -> "The version of android is to old for this feature."
            }
            Text(
                text = text,
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
        var clip: ClipData? = null
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
                        error = CopyToClipboardError.TO_OLD_ANDROID_ERROR
                    } else {
                        val uri: Uri? =
                            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                                intent.getParcelableExtra(Intent.EXTRA_STREAM)
                            } else {
                                intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
                            }
                        clip = ClipData.newUri(contentResolver, "image", uri)
                        systemClipboardManager.setPrimaryClip(clip)
                        bitmap = MediaStore.Images.Media.getBitmap(this.contentResolver, uri)
                    }
                }
            } else {
                error = CopyToClipboardError.TYPE_NOT_SUPPORTED_ERROR
            }
        }




        setContent {
            CopyToClipboardBottomSheet(
                onDismiss = {
                    println("Dismissed");finish()
                }
            ) {
                val textStyle = FlorisImeTheme.style.get(element = FlorisImeUi.KeyHint)
                val buttonContentStyle = FlorisImeTheme.style.get(element = FlorisImeUi.GlideTrail)
                val context = LocalContext.current

                error?.showError()
                bitmap?.let {
                    Text(
                        text = "Copied below image to clipboard!",
                        color = textStyle.foreground.solidColor(context = context)
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Image(
                        modifier = Modifier.fillMaxWidth(),
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Picture"
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
                    Text(text = "Ok")
                }
            }
        }
    }

}
