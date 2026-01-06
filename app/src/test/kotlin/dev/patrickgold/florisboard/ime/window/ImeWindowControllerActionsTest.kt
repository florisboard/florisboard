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
import app.cash.turbine.test
import dev.patrickgold.florisboard.app.FlorisPreferenceModel
import dev.patrickgold.florisboard.plusOrMinus
import dev.patrickgold.florisboard.shouldBeGreaterThanOrEqualTo
import dev.patrickgold.florisboard.shouldBeLessThanOrEqualTo
import dev.patrickgold.jetpref.datastore.jetprefDataStoreOf
import io.kotest.assertions.assertSoftly
import io.kotest.core.spec.style.FunSpec
import io.kotest.engine.coroutines.backgroundScope
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.property.Arb
import io.kotest.property.arbitrary.enum
import io.kotest.property.arbitrary.filter
import io.kotest.property.checkAll

class ImeWindowControllerActionsTest : FunSpec({
    val tolerance = 1e-3f.dp

    coroutineTestScope = true

    test("toggleFloatingWindow()") {
        checkAll(
            Arb.enum<ImeWindowMode>(),
            Arb.enum<ImeWindowMode.Fixed>(),
            Arb.enum<ImeWindowMode.Floating>(),
        ) { mode, fixedMode, floatingMode ->
            val prefs by jetprefDataStoreOf(FlorisPreferenceModel::class)
            val windowController = ImeWindowController(prefs, backgroundScope)
            val config = ImeWindowConfig(mode, fixedMode = fixedMode, floatingMode = floatingMode)

            windowController.activeWindowSpec.test {
                skipItems(1)
                windowController.updateWindowConfig { config }
                val specBefore = awaitItem()
                windowController.actions.toggleFloatingWindow()
                val specAfter = awaitItem()
                assertSoftly {
                    when (specBefore) {
                        is ImeWindowSpec.Fixed -> specAfter.shouldBeInstanceOf<ImeWindowSpec.Floating>()
                        is ImeWindowSpec.Floating -> specAfter.shouldBeInstanceOf<ImeWindowSpec.Fixed>()
                    }
                }
            }
        }
    }

    test("toggleCompactLayout()") {
        checkAll(
            Arb.enum<ImeWindowMode>(),
            Arb.enum<ImeWindowMode.Fixed>(),
            Arb.enum<ImeWindowMode.Floating>(),
        ) { mode, fixedMode, floatingMode ->
            val prefs by jetprefDataStoreOf(FlorisPreferenceModel::class)
            val windowController = ImeWindowController(prefs, backgroundScope)
            val config = ImeWindowConfig(mode, fixedMode = fixedMode, floatingMode = floatingMode)

            windowController.activeWindowSpec.test {
                skipItems(1)
                windowController.updateWindowConfig { config }
                val specBefore = awaitItem()
                windowController.actions.toggleCompactLayout()
                val specAfter = awaitItem()
                assertSoftly {
                    when (specBefore) {
                        is ImeWindowSpec.Fixed -> when (specBefore.fixedMode) {
                            ImeWindowMode.Fixed.COMPACT -> specAfter.shouldBeFixedNormal()
                            else -> specAfter.shouldBeFixedCompact()
                        }
                        is ImeWindowSpec.Floating -> specAfter.shouldBeFixedCompact()
                    }
                }
            }
        }
    }

    test("compactLayoutToLeft()") {
        checkAll(Arb.rootInsets()) { rootInsets ->
            val prefs by jetprefDataStoreOf(FlorisPreferenceModel::class)
            val windowController = ImeWindowController(prefs, backgroundScope)

            windowController.activeWindowSpec.test {
                skipItems(1)
                windowController.updateRootInsets(rootInsets)
                skipItems(1)
                windowController.actions.compactLayoutToLeft()
                val spec = awaitItem()
                assertSoftly {
                    val spec = spec.shouldBeFixedCompact()
                    spec.props.paddingLeft.shouldBeLessThanOrEqualTo(spec.props.paddingRight, tolerance)
                }
            }
        }
    }

    test("compactLayoutToRight()") {
        checkAll(Arb.rootInsets()) { rootInsets ->
            val prefs by jetprefDataStoreOf(FlorisPreferenceModel::class)
            val windowController = ImeWindowController(prefs, backgroundScope)

            windowController.activeWindowSpec.test {
                skipItems(1)
                windowController.updateRootInsets(rootInsets)
                skipItems(1)
                windowController.actions.compactLayoutToRight()
                val spec = awaitItem()
                assertSoftly {
                    val spec = spec.shouldBeFixedCompact()
                    spec.props.paddingLeft.shouldBeGreaterThanOrEqualTo(spec.props.paddingRight, tolerance)
                }
            }
        }
    }

    test("compactLayoutFlipSide()") {
        checkAll(
            Arb.rootInsets().filter { rootInsets ->
                ImeWindowConstraints.of(rootInsets, ImeWindowMode.Fixed.COMPACT).minPaddingHorizontal > 0.dp
            },
        ) { rootInsets ->
            val prefs by jetprefDataStoreOf(FlorisPreferenceModel::class)
            val windowController = ImeWindowController(prefs, backgroundScope)

            fun paddingsShouldBeFlipped(a: ImeWindowProps.Fixed, b: ImeWindowProps.Fixed) {
                a.paddingLeft shouldBe b.paddingRight.plusOrMinus(tolerance)
                a.paddingRight shouldBe b.paddingLeft.plusOrMinus(tolerance)
            }

            windowController.activeWindowSpec.test {
                skipItems(1)
                windowController.updateRootInsets(rootInsets)
                skipItems(1)
                windowController.actions.compactLayoutToRight()
                val specBefore = awaitItem()
                windowController.actions.compactLayoutFlipSide()
                val specAfterSplit1 = awaitItem()
                windowController.actions.compactLayoutFlipSide()
                val specAfterSplit2 = awaitItem()
                windowController.actions.compactLayoutFlipSide()
                val specAfterSplit3 = awaitItem()
                assertSoftly {
                    val specBefore = specBefore.shouldBeFixedCompact()
                    val specAfterSplit1 = specAfterSplit1.shouldBeFixedCompact()
                    val specAfterSplit2 = specAfterSplit2.shouldBeFixedCompact()
                    val specAfterSplit3 = specAfterSplit3.shouldBeFixedCompact()
                    paddingsShouldBeFlipped(specBefore.props, specAfterSplit1.props)
                    paddingsShouldBeFlipped(specAfterSplit1.props, specAfterSplit2.props)
                    paddingsShouldBeFlipped(specAfterSplit2.props, specAfterSplit3.props)
                }
            }
        }
    }

    test("resetFixedSize()") {
        checkAll(Arb.rootInsets(), Arb.enum<ImeWindowMode.Fixed>()) { rootInsets, fixedMode ->
            val config = ImeWindowConfig(
                mode = ImeWindowMode.FIXED,
                fixedMode = fixedMode,
                fixedProps = mapOf(fixedMode to ImeWindowProps.Fixed(
                    keyboardHeight = 100.dp,
                    paddingLeft = 0.dp,
                    paddingRight = 0.dp,
                    paddingBottom = 0.dp,
                )),
            )

            val prefs by jetprefDataStoreOf(FlorisPreferenceModel::class)
            prefs.keyboard.windowConfig.set(mapOf(rootInsets.formFactor.typeGuess to config))
            val windowController = ImeWindowController(prefs, backgroundScope)

            windowController.activeWindowSpec.test {
                skipItems(1)
                windowController.updateRootInsets(rootInsets)
                val specBefore = awaitItem()
                windowController.actions.resetFixedSize()
                val specAfter = awaitItem()
                assertSoftly {
                    specBefore.props shouldNotBe specAfter
                }
            }
        }
    }

    test("resetFloatingSize()") {
        checkAll(Arb.rootInsets(), Arb.enum<ImeWindowMode.Floating>()) { rootInsets, floatingMode ->
            val config = ImeWindowConfig(
                mode = ImeWindowMode.FLOATING,
                floatingMode = floatingMode,
                floatingProps = mapOf(floatingMode to ImeWindowProps.Floating(
                    keyboardHeight = 100.dp,
                    keyboardWidth = 100.dp,
                    offsetLeft = 40.dp,
                    offsetBottom = 40.dp,
                )),
            )

            val prefs by jetprefDataStoreOf(FlorisPreferenceModel::class)
            prefs.keyboard.windowConfig.set(mapOf(rootInsets.formFactor.typeGuess to config))
            val windowController = ImeWindowController(prefs, backgroundScope)

            windowController.activeWindowSpec.test {
                skipItems(1)
                windowController.updateRootInsets(rootInsets)
                val specBefore = awaitItem()
                windowController.actions.resetFloatingSize()
                val specAfter = awaitItem()
                assertSoftly {
                    specBefore.props shouldNotBe specAfter
                    // TODO how to test offset not changed?
                    //  but allow a change if constrains must move the window onscreen
                }
            }
        }
    }
})
