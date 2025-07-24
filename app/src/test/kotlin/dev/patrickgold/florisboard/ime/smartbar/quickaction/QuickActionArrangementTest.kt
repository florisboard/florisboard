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

package dev.patrickgold.florisboard.ime.smartbar.quickaction

import dev.patrickgold.florisboard.ime.text.keyboard.TextKeyData
import org.junit.jupiter.api.assertAll
import kotlin.test.Test
import kotlin.test.assertEquals

class QuickActionArrangementTest {
    @Test
    fun `contains behavior`() {
        val arrangementToAction = listOf(
            Triple(
                QuickActionArrangement(
                    stickyAction = null,
                    dynamicActions = listOf(),
                    hiddenActions = listOf(),
                ),
                QuickAction.InsertKey(TextKeyData.SETTINGS),
                false,
            ),
            Triple(
                QuickActionArrangement(
                    stickyAction = QuickAction.InsertKey(TextKeyData.SETTINGS),
                    dynamicActions = listOf(),
                    hiddenActions = listOf(),
                ),
                QuickAction.InsertKey(TextKeyData.SETTINGS),
                true,
            ),
            Triple(
                QuickActionArrangement(
                    stickyAction = null,
                    dynamicActions = listOf(QuickAction.InsertKey(TextKeyData.SETTINGS)),
                    hiddenActions = listOf(),
                ),
                QuickAction.InsertKey(TextKeyData.SETTINGS),
                true,
            ),
            Triple(
                QuickActionArrangement(
                    stickyAction = null,
                    dynamicActions = listOf(),
                    hiddenActions = listOf(QuickAction.InsertKey(TextKeyData.SETTINGS)),
                ),
                QuickAction.InsertKey(TextKeyData.SETTINGS),
                true,
            ),
        )

        assertAll(arrangementToAction.map { (arrangement, action, expectedContains) -> {
            assertEquals(expectedContains, arrangement.contains(action))
        } })
    }

    @Test
    fun `distinct behavior`() {
        val beforeToAfterDistinct = listOf(
            QuickActionArrangement(
                stickyAction = null,
                dynamicActions = listOf(),
                hiddenActions = listOf(),
            ) to QuickActionArrangement(
                stickyAction = null,
                dynamicActions = listOf(),
                hiddenActions = listOf(),
            ),
            QuickActionArrangement(
                stickyAction = QuickAction.InsertKey(TextKeyData.SETTINGS),
                dynamicActions = listOf(),
                hiddenActions = listOf(),
            ) to QuickActionArrangement(
                stickyAction = QuickAction.InsertKey(TextKeyData.SETTINGS),
                dynamicActions = listOf(),
                hiddenActions = listOf(),
            ),
            QuickActionArrangement(
                stickyAction = QuickAction.InsertKey(TextKeyData.SETTINGS),
                dynamicActions = listOf(
                    QuickAction.InsertKey(TextKeyData.SETTINGS),
                ),
                hiddenActions = listOf(),
            ) to QuickActionArrangement(
                stickyAction = QuickAction.InsertKey(TextKeyData.SETTINGS),
                dynamicActions = listOf(),
                hiddenActions = listOf(),
            ),
            QuickActionArrangement(
                stickyAction = QuickAction.InsertKey(TextKeyData.SETTINGS),
                dynamicActions = listOf(
                    QuickAction.InsertKey(TextKeyData.SETTINGS),
                    QuickAction.InsertKey(TextKeyData.SETTINGS),
                ),
                hiddenActions = listOf(),
            ) to QuickActionArrangement(
                stickyAction = QuickAction.InsertKey(TextKeyData.SETTINGS),
                dynamicActions = listOf(),
                hiddenActions = listOf(),
            ),
            QuickActionArrangement(
                stickyAction = QuickAction.InsertKey(TextKeyData.SETTINGS),
                dynamicActions = listOf(
                    QuickAction.InsertKey(TextKeyData.CLIPBOARD_SELECT_ALL),
                ),
                hiddenActions = listOf(
                    QuickAction.InsertKey(TextKeyData.VIEW_SYMBOLS),
                ),
            ) to QuickActionArrangement(
                stickyAction = QuickAction.InsertKey(TextKeyData.SETTINGS),
                dynamicActions = listOf(
                    QuickAction.InsertKey(TextKeyData.CLIPBOARD_SELECT_ALL),
                ),
                hiddenActions = listOf(
                    QuickAction.InsertKey(TextKeyData.VIEW_SYMBOLS),
                ),
            ),
            QuickActionArrangement(
                stickyAction = null,
                dynamicActions = listOf(
                    QuickAction.InsertKey(TextKeyData.CLIPBOARD_SELECT_ALL),
                ),
                hiddenActions = listOf(
                    QuickAction.InsertKey(TextKeyData.CLIPBOARD_SELECT_ALL),
                    QuickAction.InsertKey(TextKeyData.VIEW_SYMBOLS),
                ),
            ) to QuickActionArrangement(
                stickyAction = null,
                dynamicActions = listOf(
                    QuickAction.InsertKey(TextKeyData.CLIPBOARD_SELECT_ALL),
                ),
                hiddenActions = listOf(
                    QuickAction.InsertKey(TextKeyData.VIEW_SYMBOLS),
                ),
            ),
            QuickActionArrangement(
                stickyAction = null,
                dynamicActions = listOf(
                    QuickAction.InsertKey(TextKeyData.CLIPBOARD_SELECT_ALL),
                ),
                hiddenActions = listOf(
                    QuickAction.InsertKey(TextKeyData.VIEW_SYMBOLS),
                    QuickAction.InsertKey(TextKeyData.CLIPBOARD_SELECT_ALL),
                ),
            ) to QuickActionArrangement(
                stickyAction = null,
                dynamicActions = listOf(
                    QuickAction.InsertKey(TextKeyData.CLIPBOARD_SELECT_ALL),
                ),
                hiddenActions = listOf(
                    QuickAction.InsertKey(TextKeyData.VIEW_SYMBOLS),
                ),
            ),
        )

        assertAll(beforeToAfterDistinct.map { (beforeDistinct, afterDistinct) -> {
            assertEquals(afterDistinct, beforeDistinct.distinct())
        } })
    }
}
