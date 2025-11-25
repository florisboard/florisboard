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

package dev.patrickgold.florisboard.ime.gpt

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.gptManager

/**
 * Activity for picking images to be used with AI vision features.
 */
class AiImagePickerActivity : Activity() {
    companion object {
        private const val REQUEST_IMAGE_PICK = 1001
    }

    private val gptManager by gptManager()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Launch the image picker intent
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "image/*"
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        
        @Suppress("DEPRECATION")
        startActivityForResult(
            Intent.createChooser(intent, getString(R.string.gpt__select_image_for_ai)),
            REQUEST_IMAGE_PICK
        )
    }

    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        when (requestCode) {
            REQUEST_IMAGE_PICK -> {
                if (resultCode == RESULT_OK && data?.data != null) {
                    val uri = data.data!!
                    val success = gptManager.setImageFromUri(uri)
                    if (success) {
                        Toast.makeText(this, R.string.gpt__image_selected, Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, R.string.gpt__image_load_failed, Toast.LENGTH_SHORT).show()
                    }
                }
                finish()
            }
        }
    }
}
