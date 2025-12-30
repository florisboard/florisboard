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

import dev.patrickgold.florisboard.app.FlorisPreferenceModel
import dev.patrickgold.jetpref.datastore.jetprefDataStoreOf
import io.kotest.core.spec.style.FunSpec
import io.kotest.engine.coroutines.backgroundScope
import io.kotest.matchers.shouldBe

class ImeWindowControllerTest : FunSpec({
    coroutineTestScope = true

    context("isWindowShown state") {
        test("simple onShown onHidden") {
            val dataStore = jetprefDataStoreOf(FlorisPreferenceModel::class)
            val prefs by dataStore
            val windowController = ImeWindowController(prefs, backgroundScope)

            windowController.isWindowShown.value shouldBe false
            windowController.onWindowShown() shouldBe true
            windowController.isWindowShown.value shouldBe true
            windowController.onWindowHidden() shouldBe true
            windowController.isWindowShown.value shouldBe false
        }

        test("duplicate onWindowShown is detected") {
            val dataStore = jetprefDataStoreOf(FlorisPreferenceModel::class)
            val prefs by dataStore
            val windowController = ImeWindowController(prefs, backgroundScope)

            windowController.isWindowShown.value shouldBe false
            windowController.onWindowShown() shouldBe true
            windowController.isWindowShown.value shouldBe true
            windowController.onWindowShown().shouldBe(false, "duplicate onWindowShown is detected")
            windowController.isWindowShown.value shouldBe true
        }

        test("duplicate onWindowHidden is detected") {
            val dataStore = jetprefDataStoreOf(FlorisPreferenceModel::class)
            val prefs by dataStore
            val windowController = ImeWindowController(prefs, backgroundScope)

            windowController.isWindowShown.value shouldBe false
            windowController.onWindowShown() shouldBe true
            windowController.isWindowShown.value shouldBe true
            windowController.onWindowHidden() shouldBe true
            windowController.isWindowShown.value shouldBe false
            windowController.onWindowHidden().shouldBe(false, "duplicate onWindowHidden should fail")
            windowController.isWindowShown.value shouldBe false
        }
    }
})
