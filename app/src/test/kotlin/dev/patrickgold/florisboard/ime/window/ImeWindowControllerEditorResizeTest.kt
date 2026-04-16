/*
 * Copyright (C) 2025-2026 The FlorisBoard Contributors
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
import dev.patrickgold.florisboard.shouldBeGreaterThanOrEqualTo
import dev.patrickgold.florisboard.shouldBeLessThanOrEqualTo
import dev.patrickgold.jetpref.datastore.jetprefDataStoreOf
import io.kotest.assertions.assertSoftly
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.engine.coroutines.backgroundScope
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.property.Arb
import io.kotest.property.arbitrary.enum
import io.kotest.property.checkAll
import kotlinx.coroutines.flow.first

class ImeWindowControllerEditorResizeTest : FunSpec({
    val tolerance = 1e-3f.dp
    val imeRowCount = 4
    val smartBarRowCount = 0

    coroutineTestScope = true

    context("for all root insets and fixed modes") {
        test("for all resizes on top handle") {
            checkAll(
                Arb.rootInsetsWithVerticalOffset(),
                Arb.enum<ImeWindowMode.Fixed>(),
            ) { (rootInsets, offset), fixedMode ->
                val prefs by jetprefDataStoreOf(FlorisPreferenceModel::class)
                val windowController = ImeWindowController(prefs, backgroundScope)
                windowController.updateRootInsets(rootInsets)
                windowController.updateWindowConfig {
                    ImeWindowConfig(ImeWindowMode.FIXED, fixedMode = fixedMode)
                }

                windowController.editor.beginMoveGesture()
                val specBefore = windowController.activeWindowSpec.first { it !== ImeWindowSpec.Fallback }
                val specCalculated =
                    specBefore.resizedBy(
                        offset,
                        ImeWindowResizeHandle.TOP,
                        imeRowCount,
                        smartBarRowCount,
                    )
                windowController.editor.onSpecUpdated(specCalculated)
                val specAfter = windowController.activeWindowSpec.value
                windowController.editor.endMoveGesture(specAfter)

                assertSoftly {
                    val specBefore = specBefore.shouldBeInstanceOf<ImeWindowSpec.Fixed>()
                    val specAfter = specAfter.shouldBeInstanceOf<ImeWindowSpec.Fixed>()
                    specCalculated.shouldBeInstanceOf<ImeWindowSpec.Fixed>().shouldBe(specAfter)
                    withClue("vertical resize operations") {
                        if (offset.y <= 0.dp) {
                            withClue("must not decrease keyboard height") {
                                specAfter.props.keyboardHeight.shouldBeGreaterThanOrEqualTo(
                                    specBefore.props.keyboardHeight,
                                    tolerance,
                                )
                            }
                        }
                        if (offset.y >= 0.dp) {
                            withClue("must not increase keyboard height") {
                                specAfter.props.keyboardHeight.shouldBeLessThanOrEqualTo(
                                    specBefore.props.keyboardHeight,
                                    tolerance,
                                )
                            }
                        }
                    }
                    withClue("vertical resize operations must not alter padding left") {
                        specAfter.props.paddingLeft shouldBe specBefore.props.paddingLeft.plusOrMinus(tolerance)
                    }
                    withClue("vertical resize operations must not alter padding right") {
                        specAfter.props.paddingRight shouldBe specBefore.props.paddingRight.plusOrMinus(tolerance)
                    }
                    withClue("vertical resize operations must not decrease padding bottom") {
                        specAfter.props.paddingBottom.shouldBeGreaterThanOrEqualTo(
                            specBefore.props.paddingBottom,
                            tolerance,
                        )
                    }
                    withClue("resize operations must not push window out of root bounds") {
                        specAfter.props.shouldBeConstrainedTo(specAfter.constraints, tolerance)
                    }
                }
            }
        }

        test("for all resizes on bottom handle") {
            checkAll(
                Arb.rootInsetsWithVerticalOffset(),
                Arb.enum<ImeWindowMode.Fixed>(),
            ) { (rootInsets, offset), fixedMode ->
                val prefs by jetprefDataStoreOf(FlorisPreferenceModel::class)
                val windowController = ImeWindowController(prefs, backgroundScope)
                windowController.updateRootInsets(rootInsets)
                windowController.updateWindowConfig {
                    ImeWindowConfig(ImeWindowMode.FIXED, fixedMode = fixedMode)
                }

                windowController.editor.beginMoveGesture()
                val specBefore = windowController.activeWindowSpec.first { it !== ImeWindowSpec.Fallback }
                val specCalculated =
                    specBefore.resizedBy(
                        offset,
                        ImeWindowResizeHandle.BOTTOM,
                        imeRowCount,
                        smartBarRowCount,
                    )
                windowController.editor.onSpecUpdated(specCalculated)
                val specAfter = windowController.activeWindowSpec.value
                windowController.editor.endMoveGesture(specAfter)

                assertSoftly {
                    val specBefore = specBefore.shouldBeInstanceOf<ImeWindowSpec.Fixed>()
                    val specAfter = specAfter.shouldBeInstanceOf<ImeWindowSpec.Fixed>()
                    specCalculated.shouldBeInstanceOf<ImeWindowSpec.Fixed>().shouldBe(specAfter)
                    withClue("vertical resize operations") {
                        if (offset.y <= 0.dp) {
                            withClue("must not increase keyboard height") {
                                specAfter.props.keyboardHeight.shouldBeLessThanOrEqualTo(
                                    specBefore.props.keyboardHeight,
                                    tolerance,
                                )
                            }
                        }
                        if (offset.y >= 0.dp) {
                            withClue("must not decrease keyboard height") {
                                specAfter.props.keyboardHeight.shouldBeGreaterThanOrEqualTo(
                                    specBefore.props.keyboardHeight,
                                    tolerance,
                                )
                            }
                        }
                    }
                    withClue("vertical resize operations must not alter window height") {
                        val windowHeightBefore = specBefore.props.let { it.keyboardHeight + it.paddingBottom }
                        val windowHeightAfter = specAfter.props.let { it.keyboardHeight + it.paddingBottom }
                        windowHeightAfter shouldBe windowHeightBefore.plusOrMinus(tolerance)
                    }
                    withClue("vertical resize operations must not alter padding left") {
                        specAfter.props.paddingLeft shouldBe specBefore.props.paddingLeft.plusOrMinus(tolerance)
                    }
                    withClue("vertical resize operations must not alter padding right") {
                        specAfter.props.paddingRight shouldBe specBefore.props.paddingRight.plusOrMinus(tolerance)
                    }
                    withClue("resize operations must not push window out of root bounds") {
                        specAfter.props.shouldBeConstrainedTo(specAfter.constraints, tolerance)
                    }
                }
            }
        }

        test("for all resizes on left handle") {
            checkAll(
                Arb.rootInsetsWithHorizontalOffset(),
                Arb.enum<ImeWindowMode.Fixed>(),
            ) { (rootInsets, offset), fixedMode ->
                val prefs by jetprefDataStoreOf(FlorisPreferenceModel::class)
                val windowController = ImeWindowController(prefs, backgroundScope)
                windowController.updateRootInsets(rootInsets)
                windowController.updateWindowConfig {
                    ImeWindowConfig(ImeWindowMode.FIXED, fixedMode = fixedMode)
                }

                windowController.editor.beginMoveGesture()
                val specBefore = windowController.activeWindowSpec.first { it !== ImeWindowSpec.Fallback }
                val specCalculated =
                    specBefore.resizedBy(
                        offset,
                        ImeWindowResizeHandle.LEFT,
                        imeRowCount,
                        smartBarRowCount,
                    )
                windowController.editor.onSpecUpdated(specCalculated)
                val specAfter = windowController.activeWindowSpec.value
                windowController.editor.endMoveGesture(specAfter)

                assertSoftly {
                    val specBefore = specBefore.shouldBeInstanceOf<ImeWindowSpec.Fixed>()
                    val specAfter = specAfter.shouldBeInstanceOf<ImeWindowSpec.Fixed>()
                    specCalculated.shouldBeInstanceOf<ImeWindowSpec.Fixed>().shouldBe(specAfter)
                    withClue("horizontal resize operations") {
                        if (offset.x <= 0.dp) {
                            withClue("must not increase padding left") {
                                specAfter.props.paddingLeft.shouldBeLessThanOrEqualTo(
                                    specBefore.props.paddingLeft,
                                    tolerance,
                                )
                            }
                        }
                        if (offset.x >= 0.dp) {
                            withClue("must not decrease padding left") {
                                specAfter.props.paddingLeft.shouldBeGreaterThanOrEqualTo(
                                    specBefore.props.paddingLeft,
                                    tolerance,
                                )
                            }
                        }
                    }

                    withClue("horizontal resize operations must not alter padding right") {
                        specAfter.props.paddingRight shouldBe specBefore.props.paddingRight.plusOrMinus(tolerance)
                    }
                    withClue("horizontal resize operations must not alter padding bottom") {
                        specAfter.props.paddingBottom shouldBe specBefore.props.paddingBottom.plusOrMinus(tolerance)
                    }
                    withClue("horizontal resize operations must not alter window height") {
                        val windowHeightBefore = specBefore.props.let { it.keyboardHeight + it.paddingBottom }
                        val windowHeightAfter = specAfter.props.let { it.keyboardHeight + it.paddingBottom }
                        windowHeightAfter shouldBe windowHeightBefore.plusOrMinus(tolerance)
                    }
                    withClue("resize operations must not push window out of root bounds") {
                        specAfter.props.shouldBeConstrainedTo(specAfter.constraints, tolerance)
                    }
                }
            }
        }

        test("for all resizes on right handle") {
            checkAll(
                Arb.rootInsetsWithHorizontalOffset(),
                Arb.enum<ImeWindowMode.Fixed>(),
            ) { (rootInsets, offset), fixedMode ->
                val prefs by jetprefDataStoreOf(FlorisPreferenceModel::class)
                val windowController = ImeWindowController(prefs, backgroundScope)
                windowController.updateRootInsets(rootInsets)
                windowController.updateWindowConfig {
                    ImeWindowConfig(ImeWindowMode.FIXED, fixedMode = fixedMode)
                }

                windowController.editor.beginMoveGesture()
                val specBefore = windowController.activeWindowSpec.first { it !== ImeWindowSpec.Fallback }
                val specCalculated =
                    specBefore.resizedBy(
                        offset,
                        ImeWindowResizeHandle.RIGHT,
                        imeRowCount,
                        smartBarRowCount,
                    )
                windowController.editor.onSpecUpdated(specCalculated)
                val specAfter = windowController.activeWindowSpec.value
                windowController.editor.endMoveGesture(specAfter)

                assertSoftly {
                    val specBefore = specBefore.shouldBeInstanceOf<ImeWindowSpec.Fixed>()
                    val specAfter = specAfter.shouldBeInstanceOf<ImeWindowSpec.Fixed>()
                    specCalculated.shouldBeInstanceOf<ImeWindowSpec.Fixed>().shouldBe(specAfter)
                    withClue("horizontal resize operations") {
                        if (offset.x <= 0.dp) {
                            withClue("must not decrease padding right") {
                                specAfter.props.paddingRight.shouldBeGreaterThanOrEqualTo(
                                    specBefore.props.paddingRight,
                                    tolerance,
                                )
                            }
                        }
                        if (offset.x >= 0.dp) {
                            withClue("must not decrease padding right") {
                                specAfter.props.paddingRight.shouldBeLessThanOrEqualTo(
                                    specBefore.props.paddingRight,
                                    tolerance,
                                )
                            }
                        }
                    }

                    withClue("horizontal resize operations must not alter padding left") {
                        specAfter.props.paddingLeft shouldBe specBefore.props.paddingLeft.plusOrMinus(tolerance)
                    }
                    withClue("horizontal resize operations must not alter padding bottom") {
                        specAfter.props.paddingBottom shouldBe specBefore.props.paddingBottom.plusOrMinus(tolerance)
                    }
                    withClue("horizontal resize operations must not alter window height") {
                        val windowHeightBefore = specBefore.props.let { it.keyboardHeight + it.paddingBottom }
                        val windowHeightAfter = specAfter.props.let { it.keyboardHeight + it.paddingBottom }
                        windowHeightAfter shouldBe windowHeightBefore.plusOrMinus(tolerance)
                    }
                    withClue("resize operations must not push window out of root bounds") {
                        specAfter.props.shouldBeConstrainedTo(specAfter.constraints, tolerance)
                    }
                }
            }
        }
    }

    context("for all root insets and floating modes") {
        test("for all resizes on top handle") {
            checkAll(
                Arb.rootInsetsWithVerticalOffset(),
                Arb.enum<ImeWindowMode.Floating>(),
            ) { (rootInsets, offset), floatingMode ->
                val prefs by jetprefDataStoreOf(FlorisPreferenceModel::class)
                val windowController = ImeWindowController(prefs, backgroundScope)
                windowController.updateRootInsets(rootInsets)
                windowController.updateWindowConfig {
                    ImeWindowConfig(ImeWindowMode.FLOATING, floatingMode = floatingMode)
                }

                windowController.editor.beginMoveGesture()
                val specBefore = windowController.activeWindowSpec.first { it !== ImeWindowSpec.Fallback }
                val specCalculated =
                    specBefore.resizedBy(
                        offset,
                        ImeWindowResizeHandle.TOP,
                        imeRowCount,
                        smartBarRowCount,
                    )
                windowController.editor.onSpecUpdated(specCalculated)
                val specAfter = windowController.activeWindowSpec.value
                windowController.editor.endMoveGesture(specAfter)

                assertSoftly {
                    val specBefore = specBefore.shouldBeInstanceOf<ImeWindowSpec.Floating>()
                    val specAfter = specAfter.shouldBeInstanceOf<ImeWindowSpec.Floating>()
                    specCalculated.shouldBeInstanceOf<ImeWindowSpec.Floating>().shouldBe(specAfter)
                    withClue("vertical resize operations") {
                        if (offset.y <= 0.dp) {
                            withClue("must not decrease keyboard height") {
                                specAfter.props.keyboardHeight.shouldBeGreaterThanOrEqualTo(
                                    specBefore.props.keyboardHeight,
                                    tolerance,
                                )
                            }
                        }
                        if (offset.y >= 0.dp) {
                            withClue("must not increase keyboard height") {
                                specAfter.props.keyboardHeight.shouldBeLessThanOrEqualTo(
                                    specBefore.props.keyboardHeight,
                                    tolerance,
                                )
                            }
                        }
                    }
                    withClue("vertical resize operations must not alter keyboard width") {
                        specAfter.props.keyboardWidth shouldBe specBefore.props.keyboardWidth.plusOrMinus(tolerance)
                    }
                    withClue("vertical resize operations must not alter offset left") {
                        specAfter.props.offsetLeft shouldBe specBefore.props.offsetLeft.plusOrMinus(tolerance)
                    }
                    withClue("vertical resize operations must not alter offset bottom") {
                        specAfter.props.offsetBottom shouldBe specBefore.props.offsetBottom.plusOrMinus(tolerance)
                    }
                    withClue("resize operations must not push window out of root bounds") {
                        specAfter.props.shouldBeConstrainedTo(specAfter.constraints, tolerance)
                    }
                }
            }
        }

        test("for all resizes on bottom handle") {
            checkAll(
                Arb.rootInsetsWithVerticalOffset(),
                Arb.enum<ImeWindowMode.Floating>(),
            ) { (rootInsets, offset), floatingMode ->
                val prefs by jetprefDataStoreOf(FlorisPreferenceModel::class)
                val windowController = ImeWindowController(prefs, backgroundScope)
                windowController.updateRootInsets(rootInsets)
                windowController.updateWindowConfig {
                    ImeWindowConfig(ImeWindowMode.FLOATING, floatingMode = floatingMode)
                }

                windowController.editor.beginMoveGesture()
                val specBefore = windowController.activeWindowSpec.first { it !== ImeWindowSpec.Fallback }
                val specCalculated =
                    specBefore.resizedBy(
                        offset,
                        ImeWindowResizeHandle.BOTTOM,
                        imeRowCount,
                        smartBarRowCount,
                    )
                windowController.editor.onSpecUpdated(specCalculated)
                val specAfter = windowController.activeWindowSpec.value
                windowController.editor.endMoveGesture(specAfter)

                assertSoftly {
                    val specBefore = specBefore.shouldBeInstanceOf<ImeWindowSpec.Floating>()
                    val specAfter = specAfter.shouldBeInstanceOf<ImeWindowSpec.Floating>()
                    specCalculated.shouldBeInstanceOf<ImeWindowSpec.Floating>().shouldBe(specAfter)
                    withClue("vertical resize operations") {
                        if (offset.y <= 0.dp) {
                            withClue("must not increase keyboard height") {
                                specAfter.props.keyboardHeight.shouldBeLessThanOrEqualTo(
                                    specBefore.props.keyboardHeight,
                                    tolerance,
                                )
                            }
                            withClue("must not decrease offset bottom") {
                                specAfter.props.offsetBottom.shouldBeGreaterThanOrEqualTo(
                                    specBefore.props.offsetBottom,
                                    tolerance,
                                )
                            }
                        }
                        if (offset.y >= 0.dp) {
                            withClue("must not decrease keyboard height") {
                                specAfter.props.keyboardHeight.shouldBeGreaterThanOrEqualTo(
                                    specBefore.props.keyboardHeight,
                                    tolerance,
                                )
                            }
                            withClue("must not increase offset bottom") {
                                specAfter.props.offsetBottom.shouldBeLessThanOrEqualTo(
                                    specBefore.props.offsetBottom,
                                    tolerance,
                                )
                            }
                        }
                    }
                    withClue("vertical resize operations must not alter keyboard width") {
                        specAfter.props.keyboardWidth shouldBe specBefore.props.keyboardWidth.plusOrMinus(tolerance)
                    }
                    withClue("vertical resize operations must not alter offset left") {
                        specAfter.props.offsetLeft shouldBe specBefore.props.offsetLeft.plusOrMinus(tolerance)
                    }
                    withClue("resize operations must not push window out of root bounds") {
                        specAfter.props.shouldBeConstrainedTo(specAfter.constraints, tolerance)
                    }
                }
            }
        }

        test("for all resizes on left handle") {
            checkAll(
                Arb.rootInsetsWithHorizontalOffset(),
                Arb.enum<ImeWindowMode.Floating>(),
            ) { (rootInsets, offset), floatingMode ->
                val prefs by jetprefDataStoreOf(FlorisPreferenceModel::class)
                val windowController = ImeWindowController(prefs, backgroundScope)
                windowController.updateRootInsets(rootInsets)
                windowController.updateWindowConfig {
                    ImeWindowConfig(ImeWindowMode.FLOATING, floatingMode = floatingMode)
                }

                windowController.editor.beginMoveGesture()
                val specBefore = windowController.activeWindowSpec.first { it !== ImeWindowSpec.Fallback }
                val specCalculated =
                    specBefore.resizedBy(
                        offset,
                        ImeWindowResizeHandle.LEFT,
                        imeRowCount,
                        smartBarRowCount,
                    )
                windowController.editor.onSpecUpdated(specCalculated)
                val specAfter = windowController.activeWindowSpec.value
                windowController.editor.endMoveGesture(specAfter)

                assertSoftly {
                    val specBefore = specBefore.shouldBeInstanceOf<ImeWindowSpec.Floating>()
                    val specAfter = specAfter.shouldBeInstanceOf<ImeWindowSpec.Floating>()
                    specCalculated.shouldBeInstanceOf<ImeWindowSpec.Floating>().shouldBe(specAfter)
                    withClue("horizontal resize operations") {
                        if (offset.x <= 0.dp) {
                            withClue("must not decrease keyboard width") {
                                specAfter.props.keyboardWidth.shouldBeGreaterThanOrEqualTo(
                                    specBefore.props.keyboardWidth,
                                    tolerance,
                                )
                            }
                            withClue("must not increase offset left") {
                                specAfter.props.offsetLeft.shouldBeLessThanOrEqualTo(
                                    specBefore.props.offsetLeft,
                                    tolerance,
                                )
                            }
                        }
                        if (offset.x >= 0.dp) {
                            withClue("must not increase keyboard width") {
                                specAfter.props.keyboardWidth.shouldBeLessThanOrEqualTo(
                                    specBefore.props.keyboardWidth,
                                    tolerance,
                                )
                            }
                            withClue("must not decrease offset left") {
                                specAfter.props.offsetLeft.shouldBeGreaterThanOrEqualTo(
                                    specBefore.props.offsetLeft,
                                    tolerance,
                                )
                            }
                        }
                    }
                    withClue("horizontal resize operations must not alter keyboard height") {
                        specAfter.props.keyboardHeight shouldBe specBefore.props.keyboardHeight.plusOrMinus(tolerance)
                    }
                    withClue("horizontal resize operations must not alter offset bottom") {
                        specAfter.props.offsetBottom shouldBe specBefore.props.offsetBottom.plusOrMinus(tolerance)
                    }
                    withClue("resize operations must not push window out of root bounds") {
                        specAfter.props.shouldBeConstrainedTo(specAfter.constraints, tolerance)
                    }
                }
            }
        }

        test("for all resizes on right handle") {
            checkAll(
                Arb.rootInsetsWithHorizontalOffset(),
                Arb.enum<ImeWindowMode.Floating>(),
            ) { (rootInsets, offset), floatingMode ->
                val prefs by jetprefDataStoreOf(FlorisPreferenceModel::class)
                val windowController = ImeWindowController(prefs, backgroundScope)
                windowController.updateRootInsets(rootInsets)
                windowController.updateWindowConfig {
                    ImeWindowConfig(ImeWindowMode.FLOATING, floatingMode = floatingMode)
                }

                windowController.editor.beginMoveGesture()
                val specBefore = windowController.activeWindowSpec.first { it !== ImeWindowSpec.Fallback }
                val specCalculated =
                    specBefore.resizedBy(
                        offset,
                        ImeWindowResizeHandle.RIGHT,
                        imeRowCount,
                        smartBarRowCount,
                    )
                windowController.editor.onSpecUpdated(specCalculated)
                val specAfter = windowController.activeWindowSpec.value
                windowController.editor.endMoveGesture(specAfter)

                assertSoftly {
                    val specBefore = specBefore.shouldBeInstanceOf<ImeWindowSpec.Floating>()
                    val specAfter = specAfter.shouldBeInstanceOf<ImeWindowSpec.Floating>()
                    specCalculated.shouldBeInstanceOf<ImeWindowSpec.Floating>().shouldBe(specAfter)
                    withClue("horizontal resize operations") {
                        if (offset.x <= 0.dp) {
                            withClue("must not increase keyboard width") {
                                specAfter.props.keyboardWidth.shouldBeLessThanOrEqualTo(
                                    specBefore.props.keyboardWidth,
                                    tolerance,
                                )
                            }
                        }
                        if (offset.x >= 0.dp) {
                            withClue("must not decrease keyboard width") {
                                specAfter.props.keyboardWidth.shouldBeGreaterThanOrEqualTo(
                                    specBefore.props.keyboardWidth,
                                    tolerance,
                                )
                            }
                        }
                    }
                    withClue("horizontal resize operations must not alter keyboard height") {
                        specAfter.props.keyboardHeight shouldBe specBefore.props.keyboardHeight.plusOrMinus(tolerance)
                    }
                    withClue("horizontal resize operations must not alter offset left") {
                        specAfter.props.offsetLeft shouldBe specBefore.props.offsetLeft.plusOrMinus(tolerance)
                    }
                    withClue("horizontal resize operations must not alter offset bottom") {
                        specAfter.props.offsetBottom shouldBe specBefore.props.offsetBottom.plusOrMinus(tolerance)
                    }
                    withClue("resize operations must not push window out of root bounds") {
                        specAfter.props.shouldBeConstrainedTo(specAfter.constraints, tolerance)
                    }
                }
            }
        }
    }
})
