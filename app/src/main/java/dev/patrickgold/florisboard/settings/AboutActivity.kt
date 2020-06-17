package dev.patrickgold.florisboard.settings

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.MenuItem
import android.webkit.WebView
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import dev.patrickgold.florisboard.R
import java.lang.Exception

class AboutActivity : AppCompatActivity() {
    private var licensesAlertDialog: AlertDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.about_activity)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        val appVersionView = findViewById<TextView>(R.id.about__app_version)
        val licenseButton = findViewById<Button>(R.id.about__view_licenses)
        val privacyPolicyButton = findViewById<Button>(R.id.about__view_privacy_policy)
        val sourceCodeButton = findViewById<Button>(R.id.about__view_source_code)

        // Set app version string
        appVersionView.text = try {
            packageManager.getPackageInfo(packageName, 0).versionName
        } catch (e: Exception) {
            "undefined"
        }

        // Set onClickListeners for buttons
        privacyPolicyButton.setOnClickListener {
            val browserIntent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse(resources.getString(R.string.florisboard__privacy_policy_url))
            )
            startActivity(browserIntent)
        }
        licenseButton.setOnClickListener {
            val webView = WebView(this)
            webView.loadUrl("file:///android_asset/license/open_source_licenses.html")
            licensesAlertDialog = AlertDialog.Builder(this, R.style.SettingsTheme)
                .setTitle(R.string.about__license__title)
                .setView(webView)
                .setPositiveButton(android.R.string.ok, null)
                .show()
        }
        sourceCodeButton.setOnClickListener {
            val browserIntent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse(resources.getString(R.string.florisboard__repo_url))
            )
            startActivity(browserIntent)
        }

        supportActionBar?.setTitle(R.string.about__title)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
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
