package dev.patrickgold.florisboard.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import dev.patrickgold.florisboard.R

class KeyboardFragment : Fragment() {
    private lateinit var rootView: LinearLayout

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        rootView = inflater.inflate(R.layout.settings_fragment_keyboard, container, false) as LinearLayout

        val transaction = childFragmentManager.beginTransaction()
        transaction.replace(
            R.id.settings__keyboard__frame_container,
            SettingsMainActivity.PrefFragment.createFromResource(R.xml.prefs_keyboard)
        )
        transaction.commit()

        return rootView
    }
}
