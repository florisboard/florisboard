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
import dev.patrickgold.florisboard.app.FlorisPreferenceModel
import dev.patrickgold.florisboard.plusOrMinus
import dev.patrickgold.jetpref.datastore.jetprefDataStoreOf
import io.kotest.assertions.assertSoftly
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.engine.coroutines.backgroundScope
import io.kotest.matchers.comparables.shouldBeGreaterThanOrEqualTo
import io.kotest.matchers.comparables.shouldBeLessThanOrEqualTo
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.property.Arb
import io.kotest.property.arbitrary.enum
import io.kotest.property.checkAll
import kotlinx.coroutines.flow.first

class ImeWindowControllerEditorTest : FunSpec({
    val tolerance = 1e-3f.dp

    coroutineTestScope = true

    context("for all root insets and fixed modes") {
        // TODO move tests

        // TODO resize tests
    }

    context("for all root insets and floating modes") {
        test("for all upward moves") {
            checkAll(
                Arb.rootInsetsWithUpwardOffset(),
                Arb.enum<ImeWindowMode.Floating>(),
            ) { (rootInsets, offset), floatingMode ->
                val prefs by jetprefDataStoreOf(FlorisPreferenceModel::class)
                val windowController = ImeWindowController(prefs, backgroundScope)
                windowController.updateRootInsets(rootInsets)
                windowController.updateWindowConfig {
                    ImeWindowConfig(ImeWindowMode.FLOATING, floatingMode = floatingMode)
                }

                windowController.editor.beginMoveGesture()
                val specBefore = windowController.activeWindowSpec.first { it is ImeWindowSpec.Floating }
                windowController.editor.moveBy(offset)
                val specAfter = windowController.activeWindowSpec.value
                windowController.editor.endMoveGesture()

                assertSoftly {
                    val specBefore = specBefore.shouldBeInstanceOf<ImeWindowSpec.Floating>()
                    val specAfter = specAfter.shouldBeInstanceOf<ImeWindowSpec.Floating>()
                    withClue("move operations must not alter keyboard height") {
                        specAfter.props.keyboardHeight shouldBe specBefore.props.keyboardHeight
                    }
                    withClue("move operations must not alter keyboard width") {
                        specAfter.props.keyboardWidth shouldBe specBefore.props.keyboardWidth
                    }
                    withClue("move operations upward must not alter offset left") {
                        specAfter.props.offsetLeft shouldBe specBefore.props.offsetLeft
                    }
                    withClue("move operations upward must not decrease offset bottom") {
                        specAfter.props.offsetBottom.shouldBeGreaterThanOrEqualTo(specBefore.props.offsetBottom)
                    }
                    withClue("move operations upward must not push window out of root bounds") {
                        specAfter.shouldBeConstrainedTo(rootInsets.boundsDp, tolerance)
                    }
                }
            }
        }

        test("for all downward moves") {
            checkAll(
                Arb.rootInsetsWithDownwardOffset(),
                Arb.enum<ImeWindowMode.Floating>(),
            ) { (rootInsets, offset), floatingMode ->
                val prefs by jetprefDataStoreOf(FlorisPreferenceModel::class)
                val windowController = ImeWindowController(prefs, backgroundScope)
                windowController.updateRootInsets(rootInsets)
                windowController.updateWindowConfig {
                    ImeWindowConfig(ImeWindowMode.FLOATING, floatingMode = floatingMode)
                }

                windowController.editor.beginMoveGesture()
                val specBefore = windowController.activeWindowSpec.first { it is ImeWindowSpec.Floating }
                windowController.editor.moveBy(offset)
                val specAfter = windowController.activeWindowSpec.value
                windowController.editor.endMoveGesture()

                assertSoftly {
                    val specBefore = specBefore.shouldBeInstanceOf<ImeWindowSpec.Floating>()
                    val specAfter = specAfter.shouldBeInstanceOf<ImeWindowSpec.Floating>()
                    withClue("move operations must not alter keyboard height") {
                        specAfter.props.keyboardHeight shouldBe specBefore.props.keyboardHeight
                    }
                    withClue("move operations must not alter keyboard width") {
                        specAfter.props.keyboardWidth shouldBe specBefore.props.keyboardWidth
                    }
                    withClue("move operations downward must not alter offset left") {
                        specAfter.props.offsetLeft shouldBe specBefore.props.offsetLeft
                    }
                    withClue("move operations downward must not increase offset bottom") {
                        specAfter.props.offsetBottom.shouldBeLessThanOrEqualTo(specBefore.props.offsetBottom)
                    }
                    withClue("move operations downward must not push window out of root bounds") {
                        specAfter.shouldBeConstrainedTo(rootInsets.boundsDp, tolerance)
                    }
                }
            }
        }

        test("for all leftward moves") {
            checkAll(
                Arb.rootInsetsWithLeftwardOffset(),
                Arb.enum<ImeWindowMode.Floating>(),
            ) { (rootInsets, offset), floatingMode ->
                val prefs by jetprefDataStoreOf(FlorisPreferenceModel::class)
                val windowController = ImeWindowController(prefs, backgroundScope)
                windowController.updateRootInsets(rootInsets)
                windowController.updateWindowConfig {
                    ImeWindowConfig(ImeWindowMode.FLOATING, floatingMode = floatingMode)
                }

                windowController.editor.beginMoveGesture()
                val specBefore = windowController.activeWindowSpec.first { it is ImeWindowSpec.Floating }
                windowController.editor.moveBy(offset)
                val specAfter = windowController.activeWindowSpec.value
                windowController.editor.endMoveGesture()

                assertSoftly {
                    val specBefore = specBefore.shouldBeInstanceOf<ImeWindowSpec.Floating>()
                    val specAfter = specAfter.shouldBeInstanceOf<ImeWindowSpec.Floating>()
                    withClue("move operations must not alter keyboard height") {
                        specAfter.props.keyboardHeight shouldBe specBefore.props.keyboardHeight
                    }
                    withClue("move operations must not alter keyboard width") {
                        specAfter.props.keyboardWidth shouldBe specBefore.props.keyboardWidth
                    }
                    withClue("move operations leftward must not increase offset left") {
                        specAfter.props.offsetLeft.shouldBeLessThanOrEqualTo(specBefore.props.offsetLeft)
                    }
                    withClue("move operations leftward must not alter offset bottom") {
                        specAfter.props.offsetBottom shouldBe specBefore.props.offsetBottom
                    }
                    withClue("move operations leftward must not push window out of root bounds") {
                        specAfter.shouldBeConstrainedTo(rootInsets.boundsDp, tolerance)
                    }
                }
            }
        }

        test("for all rightward moves") {
            checkAll(
                Arb.rootInsetsWithRightwardOffset(),
                Arb.enum<ImeWindowMode.Floating>(),
            ) { (rootInsets, offset), floatingMode ->
                val prefs by jetprefDataStoreOf(FlorisPreferenceModel::class)
                val windowController = ImeWindowController(prefs, backgroundScope)
                windowController.updateRootInsets(rootInsets)
                windowController.updateWindowConfig {
                    ImeWindowConfig(ImeWindowMode.FLOATING, floatingMode = floatingMode)
                }

                windowController.editor.beginMoveGesture()
                val specBefore = windowController.activeWindowSpec.first { it is ImeWindowSpec.Floating }
                windowController.editor.moveBy(offset)
                val specAfter = windowController.activeWindowSpec.value
                windowController.editor.endMoveGesture()

                assertSoftly {
                    val specBefore = specBefore.shouldBeInstanceOf<ImeWindowSpec.Floating>()
                    val specAfter = specAfter.shouldBeInstanceOf<ImeWindowSpec.Floating>()
                    withClue("move operations must not alter keyboard height") {
                        specAfter.props.keyboardHeight shouldBe specBefore.props.keyboardHeight
                    }
                    withClue("move operations must not alter keyboard width") {
                        specAfter.props.keyboardWidth shouldBe specBefore.props.keyboardWidth
                    }
                    withClue("move operations rightward must not decrease offset left") {
                        specAfter.props.offsetLeft.shouldBeGreaterThanOrEqualTo(specBefore.props.offsetLeft)
                    }
                    withClue("move operations rightward must not alter offset bottom") {
                        specAfter.props.offsetBottom shouldBe specBefore.props.offsetBottom
                    }
                    withClue("move operations rightward must not push window out of root bounds") {
                        specAfter.shouldBeConstrainedTo(rootInsets.boundsDp, tolerance)
                    }
                }
            }
        }

        test("for all moves with inversion") {
            checkAll(
                Arb.rootInsetsWithAnyOffset(),
                Arb.enum<ImeWindowMode.Floating>(),
            ) { (rootInsets, offset), floatingMode ->
                val prefs by jetprefDataStoreOf(FlorisPreferenceModel::class)
                val windowController = ImeWindowController(prefs, backgroundScope)
                windowController.updateRootInsets(rootInsets)
                windowController.updateWindowConfig {
                    ImeWindowConfig(ImeWindowMode.FLOATING, floatingMode = floatingMode)
                }

                windowController.editor.beginMoveGesture()
                val specBefore = windowController.activeWindowSpec.first { it is ImeWindowSpec.Floating }
                var unconsumed = offset
                unconsumed -= windowController.editor.moveBy(unconsumed)
                unconsumed += offset.copy(-offset.x, -offset.y)
                unconsumed -= windowController.editor.moveBy(unconsumed)
                val specAfter = windowController.activeWindowSpec.value
                windowController.editor.endMoveGesture()

                assertSoftly {
                    val specBefore = specBefore.shouldBeInstanceOf<ImeWindowSpec.Floating>()
                    val specAfter = specAfter.shouldBeInstanceOf<ImeWindowSpec.Floating>()
                    withClue("move operations with inversion must not alter keyboard height") {
                        specAfter.props.keyboardHeight shouldBe specBefore.props.keyboardHeight
                    }
                    withClue("move operations with inversion must not alter keyboard width") {
                        specAfter.props.keyboardWidth shouldBe specBefore.props.keyboardWidth
                    }
                    withClue("move operations with inversion must not alter offset left") {
                        specAfter.props.offsetLeft shouldBe specBefore.props.offsetLeft
                    }
                    withClue("move operations with inversion must not alter offset bottom") {
                        specAfter.props.offsetBottom shouldBe specBefore.props.offsetBottom
                    }
                    withClue("move operations with inversion must not push window out of root bounds") {
                        specAfter.shouldBeConstrainedTo(rootInsets.boundsDp, tolerance)
                    }
                    withClue("move operations with inversion should not have unconsumed offset left") {
                        unconsumed.x shouldBe 0.dp.plusOrMinus(tolerance)
                        unconsumed.y shouldBe 0.dp.plusOrMinus(tolerance)
                    }
                }
            }
        }

        // TODO resize tests
    }
})
