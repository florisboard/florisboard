/*
 * Copyright (C) 2021 Patrick Goldinger
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

package dev.patrickgold.florisboard.ime.keyboard

import android.graphics.drawable.Drawable
import androidx.annotation.DrawableRes
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/**
 * Abstract class definition for providing icons to a keyboard view. This class has been introduced to remove the need
 * for keyboard vies to re-fetch drawable resources every time they draw on the canvas. The exact implementation is
 * dependent on the subclass.
 */
abstract class KeyboardIconSet {
    /**
     * Get the drawable for the given [id].
     *
     * @param id The Android resource id of the drawable which should be returned.
     *
     * @return The drawable for given [id] or null if this icon set does not contain a drawable for this id.
     */
    abstract fun getDrawable(@DrawableRes id: Int): Drawable?

    /**
     * Performs [block] on the drawable with the given [id]. If no drawable for the id exists,[block] will not be
     * called at all.
     *
     * @param id The Android resource id of the drawable which should be used to execute block with.
     * @param block The block which should be executed with the returned drawable.
     */
    inline fun withDrawable(@DrawableRes id: Int, block: Drawable.() -> Unit) {
        contract {
            callsInPlace(block, InvocationKind.AT_MOST_ONCE)
        }
        val drawable = getDrawable(id)
        if (drawable != null) {
            synchronized(drawable) {
                block(drawable)
            }
        }
    }
}
