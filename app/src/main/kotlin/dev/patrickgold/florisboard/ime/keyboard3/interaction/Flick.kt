/*
 * Copyright (C) 2026 The FlorisBoard Contributors
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

package dev.patrickgold.florisboard.ime.keyboard3.interaction

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import dev.patrickgold.florisboard.ime.keyboard3.touch.TouchFlickSegment
import org.k3lp.lib.text.K3String
import org.k3lp.lib.text.K3StringOrDescriptor
import org.k3lp.model.flick.K3FlickDirection
import kotlin.math.atan2
import kotlin.math.sqrt

data class Flick(
    val anchorBounds: Rect = Rect.Zero,
    val simpleBounds: Rect = Rect.Zero,
    val simpleLabel: K3StringOrDescriptor = K3String.Empty,
    val flicks: List<TouchFlickSegment> = emptyList(),
    val direction: K3FlickDirection? = null,
) {
    fun shouldShowPopup(): Boolean {
        return !simpleBounds.isEmpty
    }

    companion object {
        val None = Flick()
    }
}

fun Offset.toFlickDirection(
    hitbox: Rect,
    deadZone: Float = 0.15f,
): K3FlickDirection? {
    val delta = this - hitbox.center

    val normalizedX = delta.x / (hitbox.width / 2f)
    val normalizedY = delta.y / (hitbox.height / 2f)

    if (sqrt(
            normalizedX * normalizedX + normalizedY * normalizedY
        ) < deadZone) {
        return null
    }

    val angle = Math.toDegrees(atan2(delta.y.toDouble(), delta.x.toDouble()))
        .let { (it + 360.0) % 360.0 }

    return when {
        angle >= 337.5 || angle < 22.5 -> K3FlickDirection.EAST
        angle < 67.5 -> K3FlickDirection.SOUTH_EAST
        angle < 112.5 -> K3FlickDirection.SOUTH
        angle < 157.5 -> K3FlickDirection.SOUTH_WEST
        angle < 202.5 -> K3FlickDirection.WEST
        angle < 247.5 -> K3FlickDirection.NORTH_WEST
        angle < 292.5 -> K3FlickDirection.NORTH
        else -> K3FlickDirection.NORTH_EAST
    }
}
