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

import android.app.TimePickerDialog
import android.content.Context
import android.util.AttributeSet
import androidx.preference.Preference
import androidx.preference.Preference.OnPreferenceChangeListener
import androidx.preference.Preference.OnPreferenceClickListener
import androidx.preference.PreferenceManager
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.util.TimeUtil

/**
 * Custom preference which allows to set a time and save it to a integer preference. Uses the
 * [TimePickerDialog] for showing the actual dialog.
 *
 * @see R.styleable.DialogSeekBarPreferenceAttrs for which xml attributes this preference accepts
 *  besides the default Preference attributes.
 *
 * @property defaultValue The default value of this preference.
 */
class TimePickerDialogPreference : Preference {
    private var defaultValue: Int = 0

    @Suppress("unused")
    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, R.attr.dialogPreferenceStyle)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        context.obtainStyledAttributes(attrs, R.styleable.TimePickerDialogPreferenceAttrs).apply {
            defaultValue = getInt(R.styleable.TimePickerDialogPreferenceAttrs_android_defaultValue, defaultValue)
            recycle()
        }
        onPreferenceChangeListener = OnPreferenceChangeListener { _, newValue ->
            summary = getTextForValue(newValue)
            true
        }
        onPreferenceClickListener = OnPreferenceClickListener {
            showTimePickerDialog()
            true
        }
    }

    override fun onAttachedToHierarchy(preferenceManager: PreferenceManager?) {
        super.onAttachedToHierarchy(preferenceManager)
        summary = getTextForValue(sharedPreferences.getInt(key, defaultValue))
    }

    /**
     * Generates the text for the given [value].
     */
    private fun getTextForValue(value: Any): String {
        if (value !is Int) {
            return "invalid time"
        }
        return TimeUtil.asString(TimeUtil.decode(value.toInt()))
    }

    /**
     * Shows the time picker dialog.
     */
    private fun showTimePickerDialog() {
        val v = sharedPreferences.getInt(key, defaultValue)
        val time = TimeUtil.decode(v)
        val timePickerDialog = TimePickerDialog(context, { _, newHour, newMinute ->
            val newValue = TimeUtil.encode(newHour, newMinute)
            sharedPreferences.edit().putInt(key, newValue).apply()
            summary = getTextForValue(newValue)
        }, time.hour, time.minute, true)
        timePickerDialog.show()
    }
}
