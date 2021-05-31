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

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.jaredrummler.android.colorpicker.ColorPickerDialog
import com.jaredrummler.android.colorpicker.ColorPickerDialogListener
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.databinding.ThemeEditorAttrDialogBinding
import dev.patrickgold.florisboard.databinding.ThemeEditorAttrViewBinding
import dev.patrickgold.florisboard.ime.theme.Theme
import dev.patrickgold.florisboard.ime.theme.ThemeValue
import dev.patrickgold.florisboard.common.ViewUtils
import dev.patrickgold.florisboard.util.getActivity

class ThemeAttrView : LinearLayout {
    private val layoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
    private lateinit var binding: ThemeEditorAttrViewBinding
    private val previewDrawableSolid = GradientDrawable().apply {
        cornerRadius = ViewUtils.dp2px(6.0f)
    }
    private val previewDrawableGradient = GradientDrawable().apply {
        cornerRadius = ViewUtils.dp2px(6.0f)
    }
    var themeAttrGroupView: ThemeAttrGroupView? = null

    var attrName: String = ""
        set(v) {
            field = v
            binding.title.text = Theme.getUiAttrNameString(context, v)
            themeAttrGroupView?.refreshTheme()
        }
    var attrValue: ThemeValue = ThemeValue.Other("")
        set(v) {
            field = v
            binding.summary.text = v.toSummaryString(context)
            generateThemeValuePreview()
            themeAttrGroupView?.refreshTheme()
        }

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    init {
        id = View.generateViewId()
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        binding = ThemeEditorAttrViewBinding.bind(this)
        binding.root.setOnClickListener { showAttrEditDialog() }
    }

    private fun generateThemeValuePreview() {
        when (val attrValue = attrValue) {
            is ThemeValue.Reference -> {
                binding.attrValuePreview.setImageDrawable(
                    ContextCompat.getDrawable(context, R.drawable.ic_link))
                binding.attrValuePreview.background = null
            }
            is ThemeValue.SolidColor -> {
                binding.attrValuePreview.setImageDrawable(
                    ContextCompat.getDrawable(context, R.drawable.ic_placeholder))
                binding.attrValuePreview.background = previewDrawableSolid.apply {
                    setTint(attrValue.color)
                }
            }
            is ThemeValue.LinearGradient -> {
                binding.attrValuePreview.setImageDrawable(null)
                binding.attrValuePreview.background = null
            }
            is ThemeValue.RadialGradient -> {
                binding.attrValuePreview.setImageDrawable(null)
                binding.attrValuePreview.background = null
            }
            is ThemeValue.OnOff -> {
                binding.attrValuePreview.setImageDrawable(when (attrValue.state) {
                    true -> ContextCompat.getDrawable(context, R.drawable.ic_toggle_on)
                    else -> ContextCompat.getDrawable(context, R.drawable.ic_toggle_off)
                })
                binding.attrValuePreview.background = null
            }
            is ThemeValue.Other -> {
                binding.attrValuePreview.setImageDrawable(
                    ContextCompat.getDrawable(context, R.drawable.ic_title))
                binding.attrValuePreview.background = null
            }
        }
    }

    fun showAttrAddDialog() = showAttrDialog(false)
    private fun showAttrEditDialog() = showAttrDialog(true)
    @SuppressLint("ClickableViewAccessibility")
    private fun showAttrDialog(isEditDialog: Boolean) {
        val dialogView = ThemeEditorAttrDialogBinding.inflate(layoutInflater)
        dialogView.attrName.setText(attrName)
        val typeAdapter = ArrayAdapter(
            context,
            android.R.layout.simple_spinner_dropdown_item,
            ThemeValue.UI_STRING_MAP.values.map { resources.getString(it) }
        )
        dialogView.attrType.adapter = typeAdapter
        if (isEditDialog) {
            dialogView.attrType.setSelection(
                ThemeValue.UI_STRING_MAP.keys.indexOf(attrValue::class.simpleName!!)
                    .coerceAtLeast(0)
            )
            configureDialogUi(dialogView, attrValue)
        } else {
            dialogView.attrType.setSelection(
                ThemeValue.UI_STRING_MAP.keys.indexOf(ThemeValue.SolidColor::class.simpleName!!)
                    .coerceAtLeast(0)
            )
            configureDialogUi(dialogView, ThemeValue.SolidColor(Color.BLACK))
        }
        var userTouched = false
        dialogView.attrType.setOnTouchListener { _, _ ->
            userTouched = true
            false
        }
        dialogView.attrType.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                if (!userTouched) return
                userTouched = false
                ThemeValue.UI_STRING_MAP.keys.toList().getOrNull(position)?.let { attrType ->
                    configureDialogUi(dialogView, when (attrType) {
                        ThemeValue.Reference::class.simpleName -> {
                            ThemeValue.Reference("", "")
                        }
                        ThemeValue.SolidColor::class.simpleName -> {
                            ThemeValue.SolidColor(Color.BLACK)
                        }
                        ThemeValue.LinearGradient::class.simpleName -> {
                            ThemeValue.LinearGradient(0)
                        }
                        ThemeValue.RadialGradient::class.simpleName -> {
                            ThemeValue.RadialGradient(0)
                        }
                        ThemeValue.OnOff::class.simpleName -> {
                            ThemeValue.OnOff(false)
                        }
                        else -> {
                            ThemeValue.Other("")
                        }
                    })
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Auto-generated stub method.
            }
        }
        val dialog: AlertDialog
        AlertDialog.Builder(context).apply {
            setTitle(resources.getString(if (isEditDialog) {
                R.string.settings__theme_editor__edit_attr_dialog_title
            } else {
                R.string.settings__theme_editor__add_attr_dialog_title
            }))
            setCancelable(true)
            setView(dialogView.root)
            setPositiveButton(android.R.string.ok, null)
            if (isEditDialog) {
                setNegativeButton(android.R.string.cancel, null)
                setNeutralButton(R.string.assets__action__delete) { _, _ ->
                    themeAttrGroupView?.deleteAttr(id)
                }
            } else {
                setNegativeButton(android.R.string.cancel) { _, _ ->
                    themeAttrGroupView?.deleteAttr(id)
                }
                setOnCancelListener {
                    themeAttrGroupView?.deleteAttr(id)
                }
            }
            create()
            dialog = show()
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setOnClickListener {
                val tempAttrName = dialogView.attrName.text.toString().trim()
                val attrUnique = themeAttrGroupView?.hasAttr(id, tempAttrName) != true
                if (Theme.validateField(Theme.ValidationField.ATTR_NAME, tempAttrName) && attrUnique) {
                    attrName = tempAttrName
                    attrValue = getThemeValueFromDialogUi(dialogView)
                    dialog.dismiss()
                } else {
                    dialogView.attrNameLabel.error = resources.getString(when {
                        !attrUnique -> R.string.settings__theme_editor__error_attr_name_already_exists
                        tempAttrName.isEmpty() -> R.string.settings__theme_editor__error_attr_name_empty
                        else -> R.string.settings__theme_editor__error_attr_name
                    })
                    dialogView.attrNameLabel.isErrorEnabled = true
                }
            }
        }
    }

    private fun configureDialogUi(dialogView: ThemeEditorAttrDialogBinding, value: ThemeValue) {
        dialogView.attrValueReference.isVisible = false
        dialogView.attrValueSolidColor.isVisible = false
        dialogView.attrValueLinGrad.isVisible = false
        dialogView.attrValueRadGrad.isVisible = false
        dialogView.attrValueOnOff.isVisible = false
        dialogView.attrValueOther.isVisible = false
        when (value) {
            is ThemeValue.Reference -> {
                dialogView.attrValueReference.isVisible = true
                dialogView.attrValueReferenceGroup.setText(value.group)
                dialogView.attrValueReferenceAttr.setText(value.attr)
            }
            is ThemeValue.SolidColor -> {
                dialogView.attrValueSolidColor.isVisible = true
                dialogView.attrValueSolidColorInt.text = value.toString()
                dialogView.attrValueSolidColorEditBtn.background.setTint(value.color)
                dialogView.attrValueSolidColorEditBtn.drawable.setTint(value.complimentaryTextColor().color)
                dialogView.attrValueSolidColorEditBtn.setOnClickListener {
                    // Method on how to create a dialog which does not have a listener in the
                    // Activity taken from the original source code for the PreferenceCompat class.
                    // https://github.com/jaredrummler/ColorPicker/blob/eb76c92f53087cebff5521e217015ba95e49ad39/library/src/main/java/com/jaredrummler/android/colorpicker/ColorPreferenceCompat.java#L74-L96
                    val colorPickerDialog = ColorPickerDialog.newBuilder().apply {
                        setColor(value.color)
                        setShowAlphaSlider(true)
                    }.create()
                    colorPickerDialog.setColorPickerDialogListener(object : ColorPickerDialogListener {
                        override fun onColorSelected(dialogId: Int, color: Int) {
                            val tempSolidColor = ThemeValue.SolidColor(color)
                            dialogView.attrValueSolidColorInt.text = tempSolidColor.toString()
                            configureDialogUi(dialogView, tempSolidColor)
                        }

                        override fun onDialogDismissed(dialogId: Int) {
                            // Auto-generated stub method.
                        }
                    })
                    context.getActivity()?.supportFragmentManager?.beginTransaction()
                        ?.add(colorPickerDialog, "TAG")?.commitAllowingStateLoss()
                }
            }
            is ThemeValue.LinearGradient -> {
                dialogView.attrValueLinGrad.isVisible = true
            }
            is ThemeValue.RadialGradient -> {
                dialogView.attrValueRadGrad.isVisible = true
            }
            is ThemeValue.OnOff -> {
                dialogView.attrValueOnOff.isVisible = true
                dialogView.attrValueOnOffState.isChecked = value.state
            }
            is ThemeValue.Other -> {
                dialogView.attrValueOther.isVisible = true
                dialogView.attrValueOtherText.setText(value.rawValue)
            }
        }
    }

    private fun getThemeValueFromDialogUi(dialogView: ThemeEditorAttrDialogBinding): ThemeValue {
        ThemeValue.UI_STRING_MAP.keys.toList().getOrNull(dialogView.attrType.selectedItemPosition)?.let { attrType ->
            return when (attrType) {
                ThemeValue.Reference::class.simpleName -> {
                    ThemeValue.Reference(
                        dialogView.attrValueReferenceGroup.text.toString(),
                        dialogView.attrValueReferenceAttr.text.toString()
                    )
                }
                ThemeValue.SolidColor::class.simpleName -> {
                    ThemeValue.fromString(dialogView.attrValueSolidColorInt.text.toString())
                }
                ThemeValue.LinearGradient::class.simpleName -> {
                    ThemeValue.LinearGradient(0)
                }
                ThemeValue.RadialGradient::class.simpleName -> {
                    ThemeValue.RadialGradient(0)
                }
                ThemeValue.OnOff::class.simpleName -> {
                    ThemeValue.OnOff(dialogView.attrValueOnOffState.isChecked)
                }
                else -> {
                    ThemeValue.Other(dialogView.attrValueOtherText.text.toString())
                }
            }
        }
        return ThemeValue.Other("")
    }
}
