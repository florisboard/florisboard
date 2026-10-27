package org.florisboard.lib.snygg.value

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.unit.dp
import org.junit.jupiter.api.assertAll
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertSame

class SnyggPaddingValueTest {
    private val encoder = SnyggPaddingValue

    @Test
    fun `deserialize padding values`() {
        val pairs = listOf(
            // valid
            "10dp" to SnyggPaddingValue(PaddingValues(all = 10.dp)),
            "10dp 12dp" to SnyggPaddingValue(PaddingValues(horizontal = 10.dp, vertical = 12.dp)),
            "10dp 11dp 12dp 13dp" to SnyggPaddingValue(PaddingValues(start = 10.dp, top = 11.dp, end = 12.dp, bottom = 13.dp)),
            "3.5dp" to SnyggPaddingValue(PaddingValues(all = 3.5.dp)),
            // invalid
            "10" to null,
            "10%" to null,
        )
        assertAll(pairs.map { (raw, expected) -> {
            assertEquals(expected, encoder.deserialize(raw).getOrNull())
        } })
    }

    @Test
    fun `serialize padding values`() {
        val pairs = listOf(
            // valid
            SnyggPaddingValue(PaddingValues(all = 10.dp)) to "10dp 10dp 10dp 10dp",
            // invalid
            SnyggDefinedVarValue("shenanigans") to null
        )
        assertAll(pairs.map { (snyggValue, expected) -> {
            assertEquals(expected, encoder.serialize(snyggValue).getOrNull())
        } })
    }

    @Test
    fun `check class of default value`() {
        assertIs<SnyggPaddingValue>(encoder.defaultValue())
        assertSame(encoder, SnyggPaddingValue(PaddingValues(all = 10.dp)).encoder())
    }
}
