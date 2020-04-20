package dev.patrickgold.florisboard.settings

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import dev.patrickgold.florisboard.R

class SettingsMainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings)
        val btn1 = findViewById<Button>(R.id.settings_switch_kbd)
        btn1.setOnClickListener { v ->
            startActivity(Intent(this, SettingsKbdActivity::class.java))
        }
    }
}
