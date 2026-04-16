/*
 * Copyright (C) 2025 The FlorisBoard Contributors
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

package dev.patrickgold.florisboard

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.kotest.matchers.Matcher
import io.kotest.matchers.MatcherResult
import io.kotest.matchers.floats.FloatToleranceMatcher
import io.kotest.matchers.floats.gt
import io.kotest.matchers.floats.gte
import io.kotest.matchers.floats.lt
import io.kotest.matchers.floats.lte
import io.kotest.matchers.shouldBe

fun Dp.shouldBeLessThan(other: Dp, tolerance: Dp = 0.dp): Dp {
    this.value shouldBe lt((other + tolerance).value)
    return this
}

fun Dp.shouldBeLessThanOrEqualTo(other: Dp, tolerance: Dp = 0.dp): Dp {
    this.value shouldBe lte((other + tolerance).value)
    return this
}

fun Dp.shouldBeGreaterThan(other: Dp, tolerance: Dp = 0.dp): Dp {
    this.value shouldBe gt((other - tolerance).value)
    return this
}

fun Dp.shouldBeGreaterThanOrEqualTo(other: Dp, tolerance: Dp = 0.dp): Dp {
    this.value shouldBe gte((other - tolerance).value)
    return this
}

infix fun Dp.plusOrMinus(tolerance: Dp): DpToleranceMatcher = DpToleranceMatcher(this, tolerance)

class DpToleranceMatcher(expected: Dp, tolerance: Dp) : Matcher<Dp> {
    val matcher = FloatToleranceMatcher(expected.value, tolerance.value)

    override fun test(value: Dp): MatcherResult {
        return matcher.test(value.value)
    }
}
