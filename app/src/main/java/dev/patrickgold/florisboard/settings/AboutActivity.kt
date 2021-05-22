/*
 * Copyright (C) 2020 Patrick Goldinger
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

package dev.patrickgold.florisboard.settings

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.MenuItem
import android.webkit.WebView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.databinding.AboutActivityBinding
import dev.patrickgold.florisboard.ime.clip.FlorisClipboardManager
import dev.patrickgold.florisboard.util.AppVersionUtils
import dev.patrickgold.florisboard.util.checkIfImeIsSelected

class AboutActivity : AppCompatActivity() {
    private lateinit var binding: AboutActivityBinding
    private var licensesAlertDialog: AlertDialog? = null

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = AboutActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        // Set app version string
        val appVersion = "v" + AppVersionUtils.getRawVersionName(this)
        binding.appVersion.text = appVersion

        // Set setOnLongClickListener for copying the version string
        binding.headArea.setOnLongClickListener {
            val isImeSelected = checkIfImeIsSelected(this)
            if (isImeSelected) {
                FlorisClipboardManager.getInstance().addNewPlaintext(appVersion)
            } else {
                val clipboard: ClipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("Florisboard version", appVersion)
                clipboard.setPrimaryClip(clip)
            }
            Toast.makeText(this, R.string.about__version_copied__title, Toast.LENGTH_LONG).show()
            true
        }

        // Set onClickListeners for buttons
        binding.privacyPolicyButton.setOnClickListener {
            val browserIntent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse(resources.getString(R.string.florisboard__privacy_policy_url))
            )
            startActivity(browserIntent)
        }
        binding.licenseButton.setOnClickListener {
            val webView = WebView(this)
            webView.loadUrl("file:///android_asset/license/open_source_licenses.html")
            licensesAlertDialog = AlertDialog.Builder(this, R.style.SettingsTheme)
                .setTitle(R.string.about__license__title)
                .setView(webView)
                .setPositiveButton(android.R.string.ok, null)
                .show()
        }
        binding.sourceCodeButton.setOnClickListener {
            val browserIntent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse(resources.getString(R.string.florisboard__repo_url))
            )
            startActivity(browserIntent)
        }

        supportActionBar?.setTitle(R.string.about__title)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun onDestroy() {
        if (licensesAlertDialog?.isShowing == true) {
            licensesAlertDialog?.dismiss()
        }
        super.onDestroy()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
