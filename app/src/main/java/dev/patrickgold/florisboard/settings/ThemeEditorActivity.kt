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

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.annotation.IdRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.children
import androidx.core.view.forEach
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.databinding.ThemeEditorActivityBinding
import dev.patrickgold.florisboard.databinding.ThemeEditorGroupViewBinding
import dev.patrickgold.florisboard.databinding.ThemeEditorMetaDialogBinding
import dev.patrickgold.florisboard.ime.core.Subtype
import dev.patrickgold.florisboard.ime.keyboard.ComputingEvaluator
import dev.patrickgold.florisboard.ime.keyboard.DefaultComputingEvaluator
import dev.patrickgold.florisboard.ime.keyboard.KeyData
import dev.patrickgold.florisboard.ime.text.key.CurrencySet
import dev.patrickgold.florisboard.ime.text.key.KeyCode
import dev.patrickgold.florisboard.ime.text.keyboard.KeyboardMode
import dev.patrickgold.florisboard.ime.text.keyboard.TextKeyData
import dev.patrickgold.florisboard.ime.text.keyboard.TextKeyboardIconSet
import dev.patrickgold.florisboard.ime.text.layout.LayoutManager
import dev.patrickgold.florisboard.ime.theme.Theme
import dev.patrickgold.florisboard.ime.theme.ThemeManager
import dev.patrickgold.florisboard.ime.theme.ThemeValue
import dev.patrickgold.florisboard.res.FlorisRef
import dev.patrickgold.florisboard.settings.components.ThemeAttrGroupView
import dev.patrickgold.florisboard.settings.components.ThemeAttrView
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

/**
 * This class is the main Ui activity for directly editing a theme used by FlorisBoard. It provides
 * a base for group and attr views to operate in and also shows a preview of the current changes.
 */
class ThemeEditorActivity : AppCompatActivity() {
    private lateinit var binding: ThemeEditorActivityBinding
    private val mainScope = MainScope()
    private lateinit var layoutManager: LayoutManager
    private val themeManager: ThemeManager = ThemeManager.default()

    private lateinit var textKeyboardIconSet: TextKeyboardIconSet
    private val textComputingEvaluator = object : ComputingEvaluator by DefaultComputingEvaluator {
        override fun evaluateVisible(data: KeyData): Boolean {
            return data.code != KeyCode.SWITCH_TO_MEDIA_CONTEXT
        }

        override fun isSlot(data: KeyData): Boolean {
            return CurrencySet.isCurrencySlot(data.code)
        }

        override fun getSlotData(data: KeyData): KeyData {
            return TextKeyData(label = "$")
        }
    }

    private var editedTheme: Theme = Theme.empty()
    private var editedThemeRef: FlorisRef? = null
    private var isSaved: Boolean = false

    private var themeLabel: String = ""
        set(v) {
            field = v
            binding.themeNameLabel.text = v
        }

    companion object {
        /** Constant code for a theme saved activity result. */
        const val RESULT_CODE_THEME_EDIT_SAVED: Int = 0xFBADC1

        /** Constant code for a theme cancelled activity result. */
        const val RESULT_CODE_THEME_EDIT_CANCELLED: Int = 0xFBADC2

        /** Constant key for passing the reference to the theme to edit. */
        const val EXTRA_THEME_REF: String = "theme_ref"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ThemeEditorActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        layoutManager = LayoutManager()

        FlorisRef.from(intent.getStringExtra(EXTRA_THEME_REF) ?: "").takeIf { it.isValid }?.let { ref ->
            editedThemeRef = ref
            themeManager.loadTheme(ref).onSuccess { theme ->
                editedTheme = theme.copy()
            }
        }

        binding.themeNameEditBtn.setOnClickListener { showMetaEditDialog() }

        textKeyboardIconSet = TextKeyboardIconSet.new(this)
        binding.keyboardPreview.setIconSet(textKeyboardIconSet)
        binding.keyboardPreview.setComputingEvaluator(textComputingEvaluator)
        binding.keyboardPreview.sync()

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        supportActionBar?.title = resources.getString(R.string.settings__theme_editor__title)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        buildUi()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.help_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            R.id.settings__help -> {
                val browserIntent = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse(resources.getString(R.string.florisboard__theme_editor_wiki_url))
                )
                startActivity(browserIntent)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    /**
     * Callback function to handle clicks on the buttons in the bottom bar of this activity.
     */
    fun onActionClicked(view: View) {
        when (view.id) {
            R.id.add_group_btn -> addGroup()
            R.id.theme_cancel_btn -> onBackPressed()
            R.id.theme_save_btn -> {
                val ref = editedThemeRef
                if (ref != null) {
                    themeManager.writeTheme(
                        ref, editedTheme.copy(label = themeLabel)
                    )
                    isSaved = true
                    finish()
                }
            }
        }
    }

    /**
     * Shows a cancel confirmation dialog when the back key is pressed.
     */
    override fun onBackPressed() {
        AlertDialog.Builder(this).apply {
            setTitle(R.string.assets__action__cancel_confirm_title)
            setCancelable(true)
            setMessage(R.string.assets__action__cancel_confirm_message)
            setPositiveButton(R.string.assets__action__yes) { _, _ ->
                finish()
            }
            setNegativeButton(R.string.assets__action__no, null)
            create()
            show()
        }
    }

    /**
     * Set the result just before this activity finishes according to [isSaved].
     */
    override fun finish() {
        setResult(
            if (isSaved) {
                RESULT_CODE_THEME_EDIT_SAVED
            } else {
                RESULT_CODE_THEME_EDIT_CANCELLED
            }
        )
        super.finish()
    }

    /**
     * Add a new group view to the Ui with the specified group [name]. Returns a binding to the
     * created view class. If [name] is null, this method assumes that a new group should be
     * instantiated and will show an add group dialog.
     *
     * @param name The group name or null for a new group.
     * @return The binding to the created group view.
     */
    private fun addGroup(name: String? = null): ThemeEditorGroupViewBinding {
        val groupView = ThemeEditorGroupViewBinding.inflate(layoutInflater)
        groupView.root.themeEditorActivity = this
        binding.themeAttributes.addView(groupView.root)
        if (name == null) {
            groupView.root.showGroupAddDialog()
        } else {
            groupView.root.groupName = name
        }
        return groupView
    }

    /**
     * Deletes a view from the current Ui stack. Refreshes the theme preview afterwards.
     *
     * @param id The id of the group view to remove.
     */
    fun deleteGroup(@IdRes id: Int) {
        binding.themeAttributes.findViewById<View>(id)?.let {
            binding.themeAttributes.removeView(it)
        }
        refreshTheme()
    }

    /**
     * This method tries to focus the specified group view (causes the nested scroll view to jump
     * to the specified group view).
     *
     * @param id The id of the group view to focus.
     */
    fun focusGroup(@IdRes id: Int) {
        binding.themeAttributes.findViewById<View>(id)?.let {
            binding.themeAttributes.requestChildFocus(it, it)
        }
    }

    /**
     * Checks if the current Ui stack has a group view with [name], excluding the group view
     * specified by [id] to prevent the check on the view that initiated the request.
     *
     * @param id The group view to exclude from the check.
     * @param name The group name to check for.
     * @return True if the group name exists (except in the group view with [id]), false otherwise.
     */
    fun hasGroup(@IdRes id: Int, name: String): Boolean {
        if (name.isEmpty()) {
            return false
        }
        binding.themeAttributes.forEach { groupView ->
            if (groupView is ThemeAttrGroupView) {
                if (groupView.groupName == name && groupView.id != id) {
                    return true
                }
            }
        }
        return false
    }

    /**
     * Sorts the group views alphabetically by the group, with the exception that "window" and
     * "keyboard" are always on first and second position if they exist in the current stack.
     */
    fun sortGroups() {
        val baseMap = mutableMapOf<Int, String>()
        for (groupView in binding.themeAttributes.children) {
            if (groupView is ThemeAttrGroupView) {
                baseMap[groupView.id] = groupView.groupName
            }
        }
        val sortedMap = baseMap.toList().sortedBy { (_, v) -> v }.toMap().toMutableMap()
        val groupIds = sortedMap.keys.toMutableList()
        val groupNames = sortedMap.values.toMutableList()
        listOf(
            Pair("keyboard", true),
            Pair("window", true),
            Pair("extractEditLayout", false),
            Pair("extractActionButton", false),
        ).forEach { (groupName, addFirst) ->
            if (groupNames.contains(groupName)) {
                val groupId = groupIds[groupNames.indexOf(groupName)]
                groupIds.remove(groupId)
                groupNames.remove(groupName)
                if (addFirst) {
                    groupIds.add(0, groupId)
                    groupNames.add(0, groupName)
                } else {
                    groupIds.add(groupId)
                    groupNames.add(groupName)
                }
            }
        }
        for ((n, groupId) in groupIds.withIndex()) {
            binding.themeAttributes.findViewById<ThemeAttrGroupView>(groupId)?.let { groupView ->
                binding.themeAttributes.removeView(groupView)
                binding.themeAttributes.addView(groupView, n)
            }
        }
    }

    /**
     * Refreshes the cached theme object and applies it to the preview keyboard view.
     */
    fun refreshTheme() {
        val tempMap = mutableMapOf<String, Map<String, ThemeValue>>()
        for (groupView in binding.themeAttributes.children) {
            if (groupView is ThemeAttrGroupView) {
                val groupAttrs = mutableMapOf<String, ThemeValue>()
                for (attrView in groupView.children) {
                    if (attrView is ThemeAttrView) {
                        groupAttrs[attrView.attrName] = attrView.attrValue
                    }
                }
                tempMap[groupView.groupName] = groupAttrs.toMap()
            }
        }
        editedTheme = editedTheme.copy(
            label = themeLabel,
            attributes = tempMap.toMap()
        )
        binding.keyboardPreview.onThemeUpdated(editedTheme)
    }

    /**
     * Builds the Ui for the current [editedTheme]. Also sorts the groups afterwards.
     */
    private fun buildUi() {
        themeLabel = editedTheme.label
        for ((groupName, groupAttrs) in editedTheme.attributes) {
            val groupView = addGroup(groupName).root
            for ((attrName, attrValue) in groupAttrs) {
                groupView.addAttr(attrName, attrValue)
            }
        }
        mainScope.launch {
            binding.keyboardPreview.setComputedKeyboard(layoutManager.computeKeyboardAsync(
                KeyboardMode.CHARACTERS, Subtype.DEFAULT
            ).await())
            binding.keyboardPreview.onThemeUpdated(editedTheme)
        }
        sortGroups()
    }

    private fun showMetaEditDialog() {
        val dialogView = ThemeEditorMetaDialogBinding.inflate(layoutInflater)
        dialogView.metaName.setText(editedTheme.label)
        val dialog: AlertDialog
        AlertDialog.Builder(this).apply {
            setTitle(R.string.settings__theme_editor__edit_theme_name_dialog_title)
            setCancelable(true)
            setView(dialogView.root)
            setPositiveButton(android.R.string.ok, null)
            setNegativeButton(android.R.string.cancel, null)
            create()
            dialog = show()
            dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE)?.setOnClickListener {
                val tempThemeLabel = dialogView.metaName.text.toString().trim()
                if (Theme.validateField(Theme.ValidationField.THEME_LABEL, tempThemeLabel)) {
                    themeLabel = tempThemeLabel
                    dialog.dismiss()
                } else {
                    dialogView.metaNameLabel.error = resources.getString(R.string.settings__theme_editor__error_theme_label_empty)
                    dialogView.metaNameLabel.isErrorEnabled = true
                }
            }
        }
    }
}
