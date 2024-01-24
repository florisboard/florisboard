package dev.patrickgold.florisboard

import android.app.Activity
import android.content.ClipData
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import dev.patrickgold.florisboard.lib.android.AndroidClipboardManager
import dev.patrickgold.florisboard.lib.android.systemService


class FlorisCopyToClipboardActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val systemClipboardManager = this.systemService(AndroidClipboardManager::class)
        val type = intent.type
        val action = intent.action
        if (Intent.ACTION_SEND != action || type == null) {
            finish()
            return
        }
        if (type.startsWith("image/")) {
            val hasExtraStream = intent.hasExtra(Intent.EXTRA_STREAM)
            if (!hasExtraStream) {
                finish()
            }
            // pasting images via virtual keyboard only available since Android 7.1 (API 25)
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1) {
                finish()
            }
            val uri: Uri? =
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(Intent.EXTRA_STREAM)
                } else {
                    intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
                }

            val clip = ClipData.newUri(contentResolver, "image", uri)
            systemClipboardManager.setPrimaryClip(clip)
        }
        finish()
    }

}
