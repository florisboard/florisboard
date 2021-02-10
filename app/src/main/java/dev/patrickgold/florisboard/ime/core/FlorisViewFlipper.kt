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

package dev.patrickgold.florisboard.ime.core

import android.content.Context
import android.util.AttributeSet
import android.widget.ViewFlipper

/**
 * Custom ViewFlipper class used to prevent an unnecessary exception to be thrown when it is
 * detached from a window.
 *
 * Based on the solution of this SO answer: https://stackoverflow.com/a/8208874/6801193
 */
class FlorisViewFlipper : ViewFlipper {
    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    override fun onDetachedFromWindow() {
        try {
            super.onDetachedFromWindow()
        } catch (e: IllegalArgumentException) {
            stopFlipping()
        }
    }
}
