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
import io.kotest.matchers.Matcher
import io.kotest.matchers.MatcherResult
import io.kotest.matchers.comparables.gt
import io.kotest.matchers.comparables.gte
import io.kotest.matchers.comparables.lt
import io.kotest.matchers.comparables.lte
import io.kotest.matchers.floats.FloatToleranceMatcher
import io.kotest.matchers.shouldBe

fun Dp.shouldBeLessThan(other: Dp, tolerance: Dp): Dp {
    this shouldBe lt(other + tolerance)
    return this
}

fun Dp.shouldBeLessThanOrEqualTo(other: Dp, tolerance: Dp): Dp {
    this shouldBe lte(other + tolerance)
    return this
}

fun Dp.shouldBeGreaterThan(other: Dp, tolerance: Dp): Dp {
    this shouldBe gt(other - tolerance)
    return this
}

fun Dp.shouldBeGreaterThanOrEqualTo(other: Dp, tolerance: Dp): Dp {
    this shouldBe gte(other - tolerance)
    return this
}

infix fun Dp.plusOrMinus(tolerance: Dp): DpToleranceMatcher = DpToleranceMatcher(this, tolerance)

class DpToleranceMatcher(expected: Dp, tolerance: Dp) : Matcher<Dp> {
    val matcher = FloatToleranceMatcher(expected.value, tolerance.value)

    override fun test(value: Dp): MatcherResult {
        return matcher.test(value.value)
    }
}
