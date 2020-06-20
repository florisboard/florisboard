package dev.patrickgold.florisboard.settings

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatSpinner
import androidx.appcompat.widget.AppCompatTextView
import androidx.fragment.app.Fragment
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.ime.core.PrefHelper
import dev.patrickgold.florisboard.util.LocaleUtils
import java.util.*

class KeyboardFragment : Fragment() {
    private lateinit var prefs: PrefHelper
    private lateinit var rootView: LinearLayout
    private lateinit var subtypeListView: LinearLayout
    private lateinit var subtypeAddButton: AppCompatButton
    private lateinit var subtypeNotConfiguredView: AppCompatTextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        prefs = (activity as SettingsMainActivity).prefs
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        rootView = inflater.inflate(R.layout.settings_fragment_keyboard, container, false) as LinearLayout
        subtypeListView = rootView.findViewById(R.id.settings__keyboard__subtype_list)
        subtypeNotConfiguredView = rootView.findViewById(R.id.settings__keyboard__subtype_not_conf_warning)
        subtypeAddButton = rootView.findViewById(R.id.subtype_add_btn)
        subtypeAddButton.setOnClickListener { showAddSubtypeDialog() }

        updateSubtypeListView()

        val transaction = childFragmentManager.beginTransaction()
        transaction.replace(
            R.id.settings__keyboard__frame_container,
            SettingsMainActivity.PrefFragment.createFromResource(R.xml.prefs_correction)
        )
        transaction.commit()

        return rootView
    }

    private fun showAddSubtypeDialog() {
        val dialogView =
            View.inflate(context, R.layout.settings_fragment_keyboard_subtype_dialog, null)
        val languageSpinner = dialogView.findViewById<AppCompatSpinner>(R.id.language_dropdown)
        val layoutSpinner = dialogView.findViewById<AppCompatSpinner>(R.id.layout_dropdown)
        AlertDialog.Builder(context).apply {
            setTitle(R.string.settings__keyboard__subtype_add_title)
            setCancelable(true)
            setView(dialogView)
            setPositiveButton(R.string.settings__keyboard__subtype_add) { _, _ ->
                val languageCode = languageSpinner.selectedItem.toString()
                val layoutName = layoutSpinner.selectedItem.toString()
                prefs.keyboard.addSubtype(LocaleUtils.stringToLocale(languageCode), layoutName)
                updateSubtypeListView()
            }
            setNegativeButton(R.string.settings__keyboard__subtype_cancel) { _, _ -> }
            create()
            show()
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun showEditSubtypeDialog(id: Int) {
        var subtypeIndex = -1
        val subtypes = prefs.keyboard.subtypes
        for ((i, subtype) in subtypes.withIndex()) {
            if (subtype.id == id) {
                subtypeIndex = i
                break
            }
        }
        if (subtypeIndex < 0) {
            return
        }
        val dialogView =
            View.inflate(context, R.layout.settings_fragment_keyboard_subtype_dialog, null)
        val languageSpinner = dialogView.findViewById<AppCompatSpinner>(R.id.language_dropdown)
        languageSpinner.setSelection((languageSpinner.adapter as ArrayAdapter<String>).getPosition(subtypes[subtypeIndex].locale.toString()))
        val layoutSpinner = dialogView.findViewById<AppCompatSpinner>(R.id.layout_dropdown)
        layoutSpinner.setSelection((layoutSpinner.adapter as ArrayAdapter<String>).getPosition(subtypes[subtypeIndex].layoutName))
        AlertDialog.Builder(context).apply {
            setTitle(R.string.settings__keyboard__subtype_edit_title)
            setCancelable(true)
            setView(dialogView)
            setPositiveButton(R.string.settings__keyboard__subtype_apply) { _, _ ->
                val languageCode = languageSpinner.selectedItem.toString()
                val layoutName = layoutSpinner.selectedItem.toString()
                subtypes[subtypeIndex].locale = LocaleUtils.stringToLocale(languageCode)
                subtypes[subtypeIndex].layoutName = layoutName
                prefs.keyboard.subtypes = subtypes
                updateSubtypeListView()
            }
            setNeutralButton(R.string.settings__keyboard__subtype_delete) { _, _ ->
                prefs.keyboard.removeSubtype(subtypes[subtypeIndex])
                updateSubtypeListView()
            }
            setNegativeButton(R.string.settings__keyboard__subtype_cancel) { _, _ -> }
            create()
            show()
        }
    }

    private fun updateSubtypeListView() {
        val subtypes = prefs.keyboard.subtypes
        subtypeListView.removeAllViews()
        if (subtypes.isEmpty()) {
            subtypeNotConfiguredView.visibility = View.VISIBLE
        } else {
            subtypeNotConfiguredView.visibility = View.GONE
            for (subtype in subtypes) {
                val itemView =
                    View.inflate(context, R.layout.settings_fragment_keyboard_subtype_list_item, null)
                itemView.findViewById<TextView>(R.id.titleText)?.text =
                    subtype.locale.displayName
                itemView.findViewById<TextView>(R.id.captionText)?.text =
                    subtype.layoutName.toUpperCase(Locale.getDefault())
                itemView.setOnClickListener { showEditSubtypeDialog(subtype.id) }
                subtypeListView.addView(itemView)
            }
        }
    }
}
