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

package dev.patrickgold.florisboard.ime.clipboard

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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.app.apptheme.FlorisAppTheme
import dev.patrickgold.florisboard.app.florisPreferenceModel
import dev.patrickgold.florisboard.lib.compose.ProvideLocalizedResources
import dev.patrickgold.florisboard.lib.compose.stringRes
import dev.patrickgold.jetpref.datastore.model.observeAsState
import org.florisboard.lib.android.AndroidClipboardManager
import org.florisboard.lib.android.stringRes
import org.florisboard.lib.android.systemService

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val systemClipboardManager = this.systemService(AndroidClipboardManager::class)
        val type = intent.type
        val action = intent.action

        val prefs by florisPreferenceModel()

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
                                @Suppress("DEPRECATION")
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
                val theme by prefs.other.settingsTheme.observeAsState()
                FlorisAppTheme(theme) {
                    BottomSheet {
                        Row {
                            Text(
                                text = error?.showError()
                                    ?: bitmap?.let { stringRes(id = R.string.send_to_clipboard__description__copied_image_to_clipboard) }
                                    ?: stringRes(R.string.send_to_clipboard__unknown_error),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.weight(1f),
                            )
                        }
                        bitmap?.let {
                            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                Image(
                                    modifier = Modifier
                                        .padding(start = 64.dp, end = 64.dp, top = 32.dp, bottom = 8.dp),
                                    bitmap = bitmap!!.asImageBitmap(),
                                    contentDescription = null
                                )
                            }
                        }
                    }
                }
            }
        }
    }


    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    internal fun BottomSheet(
        content: @Composable ColumnScope.() -> Unit,
    ) {
        ModalBottomSheet(
            modifier = Modifier.navigationBarsPadding(),
            onDismissRequest = { finish() }
        ) {
            Column {
                content()
                Button(
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(16.dp),
                    onClick = { finish() },
                    colors = ButtonDefaults.textButtonColors(
                        //containerColor = buttonContainer.background.solidColor(context = context),
                    )
                ) {
                    Text(text = stringRes(id = R.string.action__ok))
                }
            }
        }
    }
}
