package org.florisboard.lib.snygg.value

import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.assertAll
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class SnyggTextValueTest {
    @Nested
    inner class TextAlignTest {
        private val encoder = SnyggTextAlignValue

        @Test
        fun `deserialize text align values`() {
            val pairs = listOf(
                // valid
                "left" to SnyggTextAlignValue(TextAlign.Left),
                "right" to SnyggTextAlignValue(TextAlign.Right),
                "center" to SnyggTextAlignValue(TextAlign.Center),
                "justify" to SnyggTextAlignValue(TextAlign.Justify),
                "start" to SnyggTextAlignValue(TextAlign.Start),
                "end" to SnyggTextAlignValue(TextAlign.End),
                 // invalid
                "invalid" to null,
                "monospaced" to null,
                "level 3000" to null,
            )
            assertAll(pairs.map { (raw, expected) -> {
                assertEquals(expected, encoder.deserialize(raw).getOrNull())
            } })
        }

        @Test
        fun `serialize text align values`() {
            val pairs = listOf(
                // valid
                SnyggTextAlignValue(TextAlign.Left) to "left",
                SnyggTextAlignValue(TextAlign.Right) to "right",
                SnyggTextAlignValue(TextAlign.Center) to "center",
                SnyggTextAlignValue(TextAlign.Justify) to "justify",
                SnyggTextAlignValue(TextAlign.Start) to "start",
                SnyggTextAlignValue(TextAlign.End) to "end",
                // invalid
                SnyggDefinedVarValue("shenanigans") to null,
                SnyggUndefinedValue to null,
            )
            assertAll(pairs.map { (snyggValue, expected) -> {
                assertEquals(expected, encoder.serialize(snyggValue).getOrNull())
            } })
        }

        @Test
        fun `check class of default value`() {
            assertIs<SnyggTextAlignValue>(encoder.defaultValue())
        }
    }

    @Nested
    inner class TextDecorationLineTest {
        private val encoder = SnyggTextDecorationLineValue

        @Test
        fun `deserialize text decoration line values`() {
            val pairs = listOf(
                // valid
                "none" to SnyggTextDecorationLineValue(TextDecoration.None),
                "underline" to SnyggTextDecorationLineValue(TextDecoration.Underline),
                "line-through" to SnyggTextDecorationLineValue(TextDecoration.LineThrough),
                // invalid
                "invalid" to null,
                "monospaced" to null,
                "level 3000" to null,
            )
            assertAll(pairs.map { (raw, expected) -> {
                assertEquals(expected, encoder.deserialize(raw).getOrNull())
            } })
        }

        @Test
        fun `serialize text decoration line values`() {
            val pairs = listOf(
                // valid
                SnyggTextDecorationLineValue(TextDecoration.None) to "none",
                SnyggTextDecorationLineValue(TextDecoration.Underline) to "underline",
                SnyggTextDecorationLineValue(TextDecoration.LineThrough) to "line-through",
                // invalid
                SnyggDefinedVarValue("shenanigans") to null,
                SnyggUndefinedValue to null,
            )
            assertAll(pairs.map { (snyggValue, expected) -> {
                assertEquals(expected, encoder.serialize(snyggValue).getOrNull())
            } })
        }

        @Test
        fun `check class of default value`() {
            assertIs<SnyggTextDecorationLineValue>(encoder.defaultValue())
        }
    }

    @Nested
    inner class TextOverflowTest {
        private val encoder = SnyggTextOverflowValue

        @Test
        fun `deserialize text overflow values`() {
            val pairs = listOf(
                // valid
                "clip" to SnyggTextOverflowValue(TextOverflow.Clip),
                "ellipsis" to SnyggTextOverflowValue(TextOverflow.Ellipsis),
                "visible" to SnyggTextOverflowValue(TextOverflow.Visible),
                // invalid
                "invalid" to null,
                "monospaced" to null,
                "level 3000" to null,
            )
            assertAll(pairs.map { (raw, expected) -> {
                assertEquals(expected, encoder.deserialize(raw).getOrNull())
            } })
        }

        @Test
        fun `serialize text overflow values`() {
            val pairs = listOf(
                // valid
                SnyggTextOverflowValue(TextOverflow.Clip) to "clip",
                SnyggTextOverflowValue(TextOverflow.Ellipsis) to "ellipsis",
                SnyggTextOverflowValue(TextOverflow.Visible) to "visible",
                // invalid
                SnyggDefinedVarValue("shenanigans") to null,
                SnyggUndefinedValue to null,
            )
            assertAll(pairs.map { (snyggValue, expected) -> {
                assertEquals(expected, encoder.serialize(snyggValue).getOrNull())
            } })
        }

        @Test
        fun `check class of default value`() {
            assertIs<SnyggTextOverflowValue>(encoder.defaultValue())
        }
    }
}
