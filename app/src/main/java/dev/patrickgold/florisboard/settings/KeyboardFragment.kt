package dev.patrickgold.florisboard.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceFragmentCompat
import dev.patrickgold.florisboard.R

class KeyboardFragment : Fragment() {
    private lateinit var rootView: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        rootView = inflater.inflate(R.layout.settings_fragment_keyboard, container, false) as LinearLayout

        val transaction = childFragmentManager.beginTransaction()
        transaction.replace(R.id.settings__keyboard__frame_container, PrefFragment())
        //transaction.addToBackStack(null)
        transaction.commit()

        return rootView
    }

    class PrefFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.prefs_keyboard, rootKey)
        }
    }
}
