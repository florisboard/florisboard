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
import io.kotest.assertions.assertSoftly
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.comparables.shouldBeGreaterThanOrEqualTo
import io.kotest.matchers.comparables.shouldBeLessThanOrEqualTo
import io.kotest.property.Arb
import io.kotest.property.arbitrary.enum
import io.kotest.property.checkAll

class ImeWindowConstraintsTest : FunSpec({
    context("Fixed") {
        test("for all root insets default props are fully visible") {
            checkAll(rootInsetsArb, Arb.enum<ImeWindowMode.Fixed>()) { rootInsets, fixedMode ->
                val constraints = ImeWindowConstraints.of(rootInsets, fixedMode)
                val props = constraints.defaultProps()
                val rootBounds = rootInsets.boundsDp

                assertSoftly {
                    props.paddingBottom shouldBeGreaterThanOrEqualTo 0.dp
                    (props.paddingBottom + props.keyboardHeight) shouldBeLessThanOrEqualTo (rootBounds.height + epsilon)
                }
            }
        }
    }

    context("Floating") {
        test("for all root insets default props are fully visible") {
            checkAll(rootInsetsArb, Arb.enum<ImeWindowMode.Floating>()) { rootInsets, floatingMode ->
                val constraints = ImeWindowConstraints.of(rootInsets, floatingMode)
                val props = constraints.defaultProps()
                val rootBounds = rootInsets.boundsDp

                assertSoftly {
                    props.offsetLeft shouldBeGreaterThanOrEqualTo 0.dp
                    (props.offsetLeft + props.keyboardWidth) shouldBeLessThanOrEqualTo (rootBounds.width + epsilon)
                    props.offsetBottom shouldBeGreaterThanOrEqualTo 0.dp
                    (props.offsetBottom + props.keyboardHeight) shouldBeLessThanOrEqualTo (rootBounds.height + epsilon)
                }
            }
        }
    }
})
