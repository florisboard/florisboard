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

package dev.patrickgold.florisboard.settings.components

import android.app.AlertDialog
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import androidx.annotation.IdRes
import androidx.core.view.forEach
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.databinding.ThemeEditorAttrViewBinding
import dev.patrickgold.florisboard.databinding.ThemeEditorGroupDialogBinding
import dev.patrickgold.florisboard.databinding.ThemeEditorGroupViewBinding
import dev.patrickgold.florisboard.ime.theme.Theme
import dev.patrickgold.florisboard.ime.theme.ThemeValue
import dev.patrickgold.florisboard.settings.ThemeEditorActivity

class ThemeAttrGroupView : LinearLayout {
    private val layoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
    private lateinit var binding: ThemeEditorGroupViewBinding
    var themeEditorActivity: ThemeEditorActivity? = null

    var groupName: String = ""
        set(v) {
            field = v
            binding.groupName.text = Theme.getUiGroupNameString(context, v)
            refreshTheme()
        }

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    init {
        id = View.generateViewId()
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        binding = ThemeEditorGroupViewBinding.bind(this)
        binding.addAttrBtn.setOnClickListener { addAttr() }
        binding.editGroupBtn.setOnClickListener { showGroupEditDialog() }
    }

    fun addAttr(name: String? = null, value: ThemeValue? = null): ThemeEditorAttrViewBinding {
        val attrView = ThemeEditorAttrViewBinding.inflate(layoutInflater)
        attrView.root.themeAttrGroupView = this
        binding.root.addView(attrView.root)
        if (name == null) {
            attrView.root.showAttrAddDialog()
        } else {
            attrView.root.attrName = name
            attrView.root.attrValue = value ?: ThemeValue.SolidColor(0)
        }
        return attrView
    }

    fun deleteAttr(@IdRes id: Int) {
        binding.root.findViewById<View>(id)?.let {
            binding.root.removeView(it)
        }
        refreshTheme()
    }

    fun hasAttr(@IdRes id: Int, name: String): Boolean {
        if (name.isEmpty()) {
            return false
        }
        binding.root.forEach { attrView ->
            if (attrView is ThemeAttrView) {
                if (attrView.attrName == name && attrView.id != id) {
                    return true
                }
            }
        }
        return false
    }

    fun refreshTheme() {
        themeEditorActivity?.refreshTheme()
    }

    fun showGroupAddDialog() = showGroupDialog(false)
    private fun showGroupEditDialog() = showGroupDialog(true)
    private fun showGroupDialog(isEditDialog: Boolean) {
        val dialogView = ThemeEditorGroupDialogBinding.inflate(layoutInflater)
        dialogView.groupName.setText(groupName)
        val dialog: AlertDialog
        AlertDialog.Builder(context).apply {
            setTitle(resources.getString(if (isEditDialog) {
                R.string.settings__theme_editor__edit_group_dialog_title
            } else {
                R.string.settings__theme_editor__add_group_dialog_title
            }))
            setCancelable(true)
            setView(dialogView.root)
            setPositiveButton(android.R.string.ok, null)
            if (isEditDialog) {
                setNegativeButton(android.R.string.cancel, null)
                setNeutralButton(R.string.assets__action__delete) { _, _ ->
                    themeEditorActivity?.deleteGroup(id)
                }
            } else {
                setNegativeButton(android.R.string.cancel) { _, _ ->
                    themeEditorActivity?.deleteGroup(id)
                }
                setOnCancelListener {
                    themeEditorActivity?.deleteGroup(id)
                }
            }
            create()
            dialog = show()
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setOnClickListener {
                val tempGroupName = dialogView.groupName.text.toString().trim()
                val groupUnique = themeEditorActivity?.hasGroup(id, tempGroupName) != true
                if (Theme.validateField(Theme.ValidationField.GROUP_NAME, tempGroupName) && groupUnique) {
                    groupName = tempGroupName
                    dialog.dismiss()
                } else {
                    dialogView.groupNameLabel.error = resources.getString(when {
                        !groupUnique -> R.string.settings__theme_editor__error_group_name_already_exists
                        tempGroupName.isEmpty() -> R.string.settings__theme_editor__error_group_name_empty
                        else -> R.string.settings__theme_editor__error_group_name
                    })
                    dialogView.groupNameLabel.isErrorEnabled = true
                }
                themeEditorActivity?.sortGroups()
                themeEditorActivity?.focusGroup(id)
            }
        }
    }
}
