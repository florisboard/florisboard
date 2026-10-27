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

package org.florisboard.lib.snygg.value

import androidx.compose.ui.unit.dp
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import kotlin.test.assertEquals

class SnyggShapeValueTest {
    @Nested
    inner class SimpleShape {
        private val circleEncoder = SnyggCircleShapeValue
        private val rectangleEncoder = SnyggRectangleShapeValue

        @Test
        fun `test default values`() {
            val pairs = listOf(
                SnyggRectangleShapeValue() to rectangleEncoder.defaultValue(),
                SnyggCircleShapeValue() to circleEncoder.defaultValue(),
                SnyggRectangleShapeValue().encoder() to rectangleEncoder,
                SnyggCircleShapeValue().encoder() to circleEncoder,
            )
            assertAll(pairs.map { (expected, actual) ->
                {
                    assertEquals(expected, actual)
                }
            })
        }

        @Test
        fun `deserialize rectangle shape values`() {
            val pairs = listOf(
                //valid
                "rectangle()" to SnyggRectangleShapeValue(),
                //invalid
                "lelek" to null
            )
            assertAll(pairs.map { (raw, expected) ->
                {
                    assertEquals(expected, rectangleEncoder.deserialize(raw).getOrNull())
                }
            })
        }

        @Test
        fun `deserialize circle shape values`() {
            val pairs = listOf(
                //valid
                "circle()" to SnyggCircleShapeValue(),
                //invalid
                "lelek" to null
            )
            assertAll(pairs.map { (raw, expected) ->
                {
                    assertEquals(expected, circleEncoder.deserialize(raw).getOrNull())
                }
            })
        }

        @Test
        fun `serialize circle shape values`() {
            val pairs = listOf(
                //valid
                SnyggCircleShapeValue() to "circle()",
                //invalid
                SnyggDefinedVarValue("shenanigans") to null
            )
            assertAll(pairs.map { (raw, expected) ->
                {
                    assertEquals(expected, circleEncoder.serialize(raw).getOrNull())
                }
            })
        }

        @Test
        fun `serialize rectangle shape values`() {
            val pairs = listOf(
                //valid
                SnyggRectangleShapeValue() to "rectangle()",
                //invalid
                SnyggDefinedVarValue("shenanigans") to null
            )
            assertAll(pairs.map { (raw, expected) ->
                {
                    assertEquals(expected, rectangleEncoder.serialize(raw).getOrNull())
                }
            })
        }
    }

    @Nested
    inner class CutCornerShape {
        val dpEncoder = SnyggCutCornerDpShapeValue
        val percentageEncoder = SnyggCutCornerPercentShapeValue

        @Test
        fun `test default values`() {
            val pairs = listOf(
                SnyggCutCornerDpShapeValue(0.dp, 0.dp, 0.dp, 0.dp) to dpEncoder.defaultValue(),
                SnyggCutCornerPercentShapeValue(0, 0, 0, 0) to percentageEncoder.defaultValue(),
                SnyggCutCornerDpShapeValue(0.dp, 0.dp, 0.dp, 0.dp).encoder() to dpEncoder,
                SnyggCutCornerPercentShapeValue(0, 0, 0, 0).encoder() to percentageEncoder,
            )
            assertAll(pairs.map { (expected, actual) ->
                {
                    assertEquals(expected, actual)
                }
            })
        }

        @Test
        fun `deserialize dp shape values`() {
            val pairs = listOf(
                //valid
                "cut-corner(4dp,5dp,2dp,5dp)" to SnyggCutCornerDpShapeValue(4.dp, 5.dp, 2.dp, 5.dp),
                //invalid
                "lelek" to null
            )
            assertAll(pairs.map { (raw, expected) ->
                {
                    assertEquals(expected, dpEncoder.deserialize(raw).getOrNull())
                }
            })
        }

        @Test
        fun `deserialize percentage shape values`() {
            val pairs = listOf(
                //valid
                "cut-corner(4%,5%,2%,5%)" to SnyggCutCornerPercentShapeValue(4, 5, 2, 5),
                //invalid
                "lelek" to null
            )
            assertAll(pairs.map { (raw, expected) ->
                {
                    assertEquals(expected, percentageEncoder.deserialize(raw).getOrNull())
                }
            })
        }

        @Test
        fun `serialize percentage shape values`() {
            val pairs = listOf(
                //valid
                SnyggCutCornerPercentShapeValue(4, 5, 2, 5) to "cut-corner(4%,5%,2%,5%)",
                //invalid
                SnyggDefinedVarValue("shenanigans") to null
            )
            assertAll(pairs.map { (raw, expected) ->
                {
                    assertEquals(expected, percentageEncoder.serialize(raw).getOrNull())
                }
            })
        }

        @Test
        fun `serialize dp shape values`() {
            val pairs = listOf(
                //valid
                SnyggCutCornerDpShapeValue(4.dp, 5.dp, 2.dp, 5.dp) to "cut-corner(4dp,5dp,2dp,5dp)",
                //invalid
                SnyggDefinedVarValue("shenanigans") to null
            )
            assertAll(pairs.map { (raw, expected) ->
                {
                    assertEquals(expected, dpEncoder.serialize(raw).getOrNull())
                }
            })
        }
    }

    @Nested
    inner class RoundedCorner {
        val dpEncoder = SnyggRoundedCornerDpShapeValue
        val percentageEncoder = SnyggRoundedCornerPercentShapeValue

        @Test
        fun `test default values`() {
            val pairs = listOf(
                SnyggRoundedCornerDpShapeValue(0.dp, 0.dp, 0.dp, 0.dp) to dpEncoder.defaultValue(),
                SnyggRoundedCornerPercentShapeValue(0, 0, 0, 0) to percentageEncoder.defaultValue(),
                SnyggRoundedCornerDpShapeValue(0.dp, 0.dp, 0.dp, 0.dp).encoder() to dpEncoder,
                SnyggRoundedCornerPercentShapeValue(0, 0, 0, 0).encoder() to percentageEncoder,
            )
            assertAll(pairs.map { (expected, actual) ->
                {
                    assertEquals(expected, actual)
                }
            })
        }

        @Test
        fun `deserialize dp shape values`() {
            val pairs = listOf(
                //valid
                "rounded-corner(4dp,5dp,2dp,5dp)" to SnyggRoundedCornerDpShapeValue(4.dp, 5.dp, 2.dp, 5.dp),
                //invalid
                "lelek" to null
            )
            assertAll(pairs.map { (raw, expected) ->
                {
                    assertEquals(expected, dpEncoder.deserialize(raw).getOrNull())
                }
            })
        }

        @Test
        fun `deserialize percentage shape values`() {
            val pairs = listOf(
                //valid
                "rounded-corner(4%,5%,2%,5%)" to SnyggRoundedCornerPercentShapeValue(4, 5, 2, 5),
                //invalid
                "lelek" to null
            )
            assertAll(pairs.map { (raw, expected) ->
                {
                    assertEquals(expected, percentageEncoder.deserialize(raw).getOrNull())
                }
            })
        }

        @Test
        fun `serialize percentage shape values`() {
            val pairs = listOf(
                //valid
                SnyggRoundedCornerPercentShapeValue(4, 5, 2, 5) to "rounded-corner(4%,5%,2%,5%)",
                //invalid
                SnyggDefinedVarValue("shenanigans") to null
            )
            assertAll(pairs.map { (raw, expected) ->
                {
                    assertEquals(expected, percentageEncoder.serialize(raw).getOrNull())
                }
            })
        }

        @Test
        fun `serialize dp shape values`() {
            val pairs = listOf(
                //valid
                SnyggRoundedCornerDpShapeValue(4.dp, 5.dp, 2.dp, 5.dp) to "rounded-corner(4dp,5dp,2dp,5dp)",
                //invalid
                SnyggDefinedVarValue("shenanigans") to null
            )
            assertAll(pairs.map { (raw, expected) ->
                {
                    assertEquals(expected, dpEncoder.serialize(raw).getOrNull())
                }
            })
        }
    }
}
