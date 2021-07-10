/*
 * Copyright (C) 2021 Patrick Goldinger
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

package dev.patrickgold.florisboard.common

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.viewbinding.ViewBinding
import com.google.android.material.snackbar.Snackbar
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.ime.core.Preferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel

abstract class FlorisActivity<V : ViewBinding> : AppCompatActivity(), CoroutineScope by MainScope() {
    private var _binding: V? = null
    protected val binding: V
        get() = _binding!!
    protected val prefs: Preferences get() = Preferences.default()

    private var errorDialog: AlertDialog? = null
    private var errorSnackbar: Snackbar? = null
    private var errorThrowable: Throwable? = null
    private var messageSnackbar: Snackbar? = null

    protected abstract fun onCreateBinding(): V

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        onCreateBinding().let {
            _binding = it
            setContentView(it.root)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cancel()
        _binding = null
        errorDialog?.dismiss()
        errorDialog = null
        errorSnackbar?.dismiss()
        errorSnackbar = null
        errorThrowable = null
        messageSnackbar?.dismiss()
        messageSnackbar = null
    }

    fun showMessage(@StringRes snackbarMessageResId: Int) {
        val snackbarMessage = resources.getString(snackbarMessageResId)
        showMessage(snackbarMessage)
    }

    fun showMessage(snackbarMessage: String) {
        messageSnackbar?.dismiss()
        messageSnackbar = Snackbar.make(binding.root, snackbarMessage, Snackbar.LENGTH_LONG).apply {
            setAction(android.R.string.ok) {
                messageSnackbar?.dismiss()
            }
            show() }
    }

    fun showError(throwable: Throwable) {
        val snackbarMessage = resources.getString(R.string.assets__error__snackbar_message)
        showError(snackbarMessage, throwable)
    }

    fun showError(@StringRes snackbarMessageResId: Int, throwable: Throwable) {
        val snackbarMessage = resources.getString(snackbarMessageResId)
        showError(snackbarMessage, throwable)
    }

    fun showError(snackbarMessage: String, throwable: Throwable) {
        errorDialog?.dismiss()
        errorDialog = null
        errorSnackbar?.dismiss()
        errorSnackbar = Snackbar.make(binding.root, snackbarMessage, Snackbar.LENGTH_LONG).apply {
            setAction(R.string.assets__error__details) {
                showErrorDialog()
            }
            show()
        }
        errorThrowable = throwable
    }

    fun showErrorDialog(throwable: Throwable? = errorThrowable) {
        errorDialog?.dismiss()
        errorDialog = AlertDialog.Builder(this@FlorisActivity).run {
            setTitle(R.string.assets__error__details)
            setMessage(throwable?.stackTraceToString())
            setPositiveButton(android.R.string.ok, null)
            setNeutralButton(R.string.crash_dialog__copy_to_clipboard) { _, _ ->
                val clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE)
                if (clipboardManager != null && clipboardManager is ClipboardManager) {
                    clipboardManager.setPrimaryClip(ClipData.newPlainText(throwable.toString(), throwable.toString()))
                }
            }
            create()
            show()
        }
    }
}
