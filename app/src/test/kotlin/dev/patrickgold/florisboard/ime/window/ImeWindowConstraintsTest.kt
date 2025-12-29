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
import net.jqwik.api.ForAll
import net.jqwik.api.Group
import net.jqwik.api.Property
import org.junit.jupiter.api.assertAll
import kotlin.test.assertTrue

class ImeWindowConstraintsTest : ImeWindowDomain() {
    @Group
    inner class Floating {
        @Property
        fun `for all root insets default props are fully visible`(
            @ForAll("rootInsets") rootInsets: ImeInsets.Root,
            @ForAll floatingMode: ImeWindowMode.Floating,
        ) {
            val constraints = ImeWindowConstraints.of(rootInsets, floatingMode)
            val props = constraints.defaultProps()
            println(props)
            val rootBounds = rootInsets.boundsDp

            assertAll(
                {
                    assertTrue("width fits screen") {
                        props.offsetLeft >= 0.dp && (props.offsetLeft + props.keyboardWidth) <= rootBounds.width
                    }
                },
                {
                    assertTrue("height fits screen") {
                        props.offsetBottom >= 0.dp && (props.offsetBottom + props.keyboardHeight) <= rootBounds.height
                    }
                },
            )
        }
    }
}
