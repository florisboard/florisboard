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

package dev.patrickgold.florisboard.lib

import androidx.compose.runtime.Stable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.IntOffset
import kotlin.math.absoluteValue
import kotlin.math.max
import kotlin.math.min

class FlorisRect private constructor(
    var left: Float,
    var top: Float,
    var right: Float,
    var bottom: Float,
) {
    companion object {
        fun empty() = FlorisRect(0.0f, 0.0f, 0.0f, 0.0f)

        fun new(
            left: Float = 0.0f,
            top: Float = 0.0f,
            right: Float = 0.0f,
            bottom: Float = 0.0f,
        ) = FlorisRect(left, top, right, bottom)

        fun new(
            width: Float,
            height: Float,
        ) = FlorisRect(0.0f, 0.0f, width, height)

        fun from(r: FlorisRect) = FlorisRect(r.left, r.top, r.right, r.bottom)
    }

    fun applyFrom(other: FlorisRect): FlorisRect {
        left = other.left
        top = other.top
        right = other.right
        bottom = other.bottom
        return this
    }

    fun isEmpty(): Boolean {
        return left >= right || top >= bottom
    }

    fun isNotEmpty(): Boolean {
        return !isEmpty()
    }

    fun intersectWith(other: FlorisRect) {
        left = max(left, other.left)
        top = max(top, other.top)
        right = min(right, other.right)
        bottom = min(bottom, other.bottom)
    }

    fun intersectedWith(other: FlorisRect): FlorisRect {
        return FlorisRect(
            max(left, other.left),
            max(top, other.top),
            min(right, other.right),
            min(bottom, other.bottom)
        )
    }

    fun overlaps(other: FlorisRect): Boolean {
        if (right <= other.left || other.right <= left)
            return false
        if (bottom <= other.top || other.bottom <= top)
            return false
        return true
    }

    fun contains(offset: Offset): Boolean {
        return offset.x >= left && offset.x < right && offset.y >= top && offset.y < bottom
    }

    fun contains(offsetX: Float, offsetY: Float): Boolean {
        return offsetX >= left && offsetX < right && offsetY >= top && offsetY < bottom
    }

    var width: Float
        get() = right - left
        set(v) { right = left + v }

    var height: Float
        get() = bottom - top
        set(v) { bottom = top + v }

    val size: Size
        get() = Size(width, height)

    val minDimension: Float
        get() = min(width.absoluteValue, height.absoluteValue)

    val maxDimension: Float
        get() = max(width.absoluteValue, height.absoluteValue)

    fun translateBy(offset: Offset) {
        left += offset.x
        top += offset.y
        right += offset.x
        bottom += offset.y
    }

    fun translateBy(translateX: Float, translateY: Float) {
        left += translateX
        top += translateY
        right += translateX
        bottom += translateY
    }

    fun translatedBy(offset: Offset): FlorisRect {
        return FlorisRect(
            left = left + offset.x,
            top = top + offset.y,
            right = right + offset.x,
            bottom = bottom + offset.y,
        )
    }

    fun translatedBy(translateX: Float, translateY: Float): FlorisRect {
        return FlorisRect(
            left = left + translateX,
            top = top + translateY,
            right = right + translateX,
            bottom = bottom + translateY,
        )
    }

    fun inflateBy(delta: Float) {
        left -= delta
        top -= delta
        right += delta
        bottom += delta
    }

    fun inflateBy(deltaX: Float, deltaY: Float) {
        left -= deltaX
        top -= deltaY
        right += deltaX
        bottom += deltaY
    }

    fun inflatedBy(delta: Float): FlorisRect {
        return FlorisRect(
            left = left - delta,
            top = top - delta,
            right = right + delta,
            bottom = bottom + delta,
        )
    }

    fun inflatedBy(deltaX: Float, deltaY: Float): FlorisRect {
        return FlorisRect(
            left = left - deltaX,
            top = top - deltaY,
            right = right + deltaX,
            bottom = bottom + deltaY,
        )
    }

    fun deflateBy(delta: Float) = inflateBy(-delta)

    fun deflateBy(deltaX: Float, deltaY: Float) = inflateBy(-deltaX, -deltaY)

    fun deflatedBy(delta: Float) = inflatedBy(-delta)

    fun deflatedBy(deltaX: Float, deltaY: Float) = inflatedBy(-deltaX, -deltaY)

    val topLeft: Offset
        get() = Offset(left, top)

    val topCenter: Offset
        get() = Offset(left + width / 2.0f, top)

    val topRight: Offset
        get() = Offset(right, top)

    val centerLeft: Offset
        get() = Offset(left, top + height / 2.0f)

    val center: Offset
        get() = Offset(left + width / 2.0f, top + height / 2.0f)

    val centerRight: Offset
        get() = Offset(right, top + height / 2.0f)

    val bottomLeft: Offset
        get() = Offset(left, bottom)

    val bottomCenter: Offset
        get() = Offset(left + width / 2.0f, bottom)

    val bottomRight: Offset
        get() = Offset(right, bottom)

    fun toAndroidRect(): android.graphics.Rect {
        return android.graphics.Rect(left.toInt(), top.toInt(), right.toInt(), bottom.toInt())
    }

    override fun toString(): String {
        return "FlorisRect(left = $left, top = $top, right = $right, bottom = $bottom)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as FlorisRect

        if (left != other.left) return false
        if (top != other.top) return false
        if (right != other.right) return false
        if (bottom != other.bottom) return false

        return true
    }

    override fun hashCode(): Int {
        var result = left.hashCode()
        result = 31 * result + top.hashCode()
        result = 31 * result + right.hashCode()
        result = 31 * result + bottom.hashCode()
        return result
    }
}

@Suppress("NOTHING_TO_INLINE")
@Stable
inline fun Offset.toIntOffset() = IntOffset(x.toInt(), y.toInt())
