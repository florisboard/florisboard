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

package dev.patrickgold.florisboard.ime.window

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import dev.patrickgold.florisboard.shouldBeGreaterThanOrEqualTo
import dev.patrickgold.florisboard.shouldBeLessThanOrEqualTo
import io.kotest.assertions.assertSoftly
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.comparables.shouldBeLessThanOrEqualTo
import io.kotest.property.Arb
import io.kotest.property.arbitrary.enum
import io.kotest.property.checkAll

class ImeWindowConstraintsTest : FunSpec({
    val tolerance = 1e-3f.dp

    context("for all root insets and fixed modes") {
        test("default props are fully visible") {
            checkAll(Arb.rootInsets(), Arb.enum<ImeWindowMode.Fixed>()) { rootInsets, fixedMode ->
                val constraints = ImeWindowConstraints.of(rootInsets, fixedMode)
                val props = constraints.defaultProps()
                val rootBounds = rootInsets.boundsDp

                assertSoftly {
                    // TODO width enforcement test
                    props.paddingBottom.shouldBeGreaterThanOrEqualTo(0.dp, tolerance)
                    (props.paddingBottom + props.keyboardHeight).shouldBeLessThanOrEqualTo(rootBounds.height, tolerance)
                }
            }
        }

        test("0.dp <= min <= def <= max keyboard width") {
            checkAll(Arb.rootInsets(), Arb.enum<ImeWindowMode.Fixed>()) { rootInsets, fixedMode ->
                val constraints = ImeWindowConstraints.of(rootInsets, fixedMode)

                assertSoftly {
                    // no tolerance here, potential rounding errors must be mitigated by constraints itself
                    0.dp.shouldBeLessThanOrEqualTo(constraints.minKeyboardWidth)
                    constraints.minKeyboardWidth.shouldBeLessThanOrEqualTo(constraints.defKeyboardWidth)
                    constraints.defKeyboardWidth.shouldBeLessThanOrEqualTo(constraints.maxKeyboardWidth)
                }
            }
        }

        test("0.dp <= min <= def <= max keyboard height") {
            checkAll(Arb.rootInsets(), Arb.enum<ImeWindowMode.Fixed>()) { rootInsets, fixedMode ->
                val constraints = ImeWindowConstraints.of(rootInsets, fixedMode)

                assertSoftly {
                    // no tolerance here, potential rounding errors must be mitigated by constraints itself
                    0.dp.shouldBeLessThanOrEqualTo(constraints.minKeyboardHeight)
                    constraints.minKeyboardHeight.shouldBeLessThanOrEqualTo(constraints.defKeyboardHeight)
                    constraints.defKeyboardHeight.shouldBeLessThanOrEqualTo(constraints.maxKeyboardHeight)
                }
            }
        }
    }

    context("for all root insets and floating modes") {
        test("default props are fully visible") {
            checkAll(Arb.rootInsets(), Arb.enum<ImeWindowMode.Floating>()) { rootInsets, floatingMode ->
                val constraints = ImeWindowConstraints.of(rootInsets, floatingMode)
                val props = constraints.defaultProps()
                val rootBounds = rootInsets.boundsDp

                assertSoftly {
                    props.offsetLeft.shouldBeGreaterThanOrEqualTo(0.dp, tolerance)
                    (props.offsetLeft + props.keyboardWidth).shouldBeLessThanOrEqualTo(rootBounds.width, tolerance)
                    props.offsetBottom.shouldBeGreaterThanOrEqualTo(0.dp, tolerance)
                    (props.offsetBottom + props.keyboardHeight).shouldBeLessThanOrEqualTo(rootBounds.height, tolerance)
                }
            }
        }

        test("0.dp <= min <= def <= max keyboard width") {
            checkAll(Arb.rootInsets(), Arb.enum<ImeWindowMode.Floating>()) { rootInsets, floatingMode ->
                val constraints = ImeWindowConstraints.of(rootInsets, floatingMode)

                assertSoftly {
                    // no tolerance here, potential rounding errors must be mitigated by constraints itself
                    0.dp.shouldBeLessThanOrEqualTo(constraints.minKeyboardWidth)
                    constraints.minKeyboardWidth.shouldBeLessThanOrEqualTo(constraints.defKeyboardWidth)
                    constraints.defKeyboardWidth.shouldBeLessThanOrEqualTo(constraints.maxKeyboardWidth)
                }
            }
        }

        test("0.dp <= min <= def <= max keyboard height") {
            checkAll(Arb.rootInsets(), Arb.enum<ImeWindowMode.Floating>()) { rootInsets, floatingMode ->
                val constraints = ImeWindowConstraints.of(rootInsets, floatingMode)

                assertSoftly {
                    // no tolerance here, potential rounding errors must be mitigated by constraints itself
                    0.dp.shouldBeLessThanOrEqualTo(constraints.minKeyboardHeight)
                    constraints.minKeyboardHeight.shouldBeLessThanOrEqualTo(constraints.defKeyboardHeight)
                    constraints.defKeyboardHeight.shouldBeLessThanOrEqualTo(constraints.maxKeyboardHeight)
                }
            }
        }
    }
})
