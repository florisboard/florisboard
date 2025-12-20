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

import dev.patrickgold.florisboard.app.FlorisPreferenceStore
import dev.patrickgold.jetpref.datastore.runtime.LoadStrategy
import dev.patrickgold.jetpref.datastore.runtime.PersistStrategy
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Nested
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ImeWindowControllerTest {
    @BeforeTest
    fun initStore() = runTest {
        FlorisPreferenceStore.init(
            loadStrategy = LoadStrategy.Disabled,
            persistStrategy = PersistStrategy.Disabled,
        ).let { result ->
            assertTrue(result.isSuccess, "DataStore initialization failed")
        }
    }

    @Nested
    inner class IsWindowShownState {
        @Test
        fun `simple onShown onHidden`() = runTest {
            val windowController = ImeWindowController(backgroundScope)

            assertFalse(windowController.isWindowShown.value)
            assertTrue { windowController.onWindowShown() }
            assertTrue(windowController.isWindowShown.value)
            assertTrue { windowController.onWindowHidden() }
            assertFalse(windowController.isWindowShown.value)
        }

        @Test
        fun `duplicate onWindowShown is detected`() = runTest {
            val windowController = ImeWindowController(backgroundScope)

            assertFalse(windowController.isWindowShown.value)
            assertTrue { windowController.onWindowShown() }
            assertTrue(windowController.isWindowShown.value)
            assertFalse("duplicate onWindowShown is detected") {
                windowController.onWindowShown()
            }
            assertTrue(windowController.isWindowShown.value)
        }

        @Test
        fun `duplicate onWindowHidden is detected`() = runTest {
            val windowController = ImeWindowController(backgroundScope)

            assertFalse(windowController.isWindowShown.value)
            assertTrue { windowController.onWindowShown() }
            assertTrue(windowController.isWindowShown.value)
            assertTrue { windowController.onWindowHidden() }
            assertFalse(windowController.isWindowShown.value)
            assertFalse("duplicate onWindowHidden should fail") {
                windowController.onWindowHidden()
            }
            assertFalse(windowController.isWindowShown.value)
        }
    }
}
