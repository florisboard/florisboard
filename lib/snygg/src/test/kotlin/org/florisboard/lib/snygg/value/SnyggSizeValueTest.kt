package org.florisboard.lib.snygg.value

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.junit.jupiter.api.assertAll
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SnyggSizeValueTest {
    private val validPairs = listOf(
        "4.0dp"     to SnyggDpSizeValue(4.dp),
        "12.0dp"    to SnyggDpSizeValue(12.dp),
        "12.5dp"    to SnyggDpSizeValue(12.5.dp),
        "4.0sp"     to SnyggSpSizeValue(4.sp),
        "12.0sp"    to SnyggSpSizeValue(12.sp),
        "12.5sp"    to SnyggSpSizeValue(12.5.sp),
        "4.0%"      to SnyggPercentageSizeValue(0.04f),
        "12.0%"     to SnyggPercentageSizeValue(0.12f),
        "12.5%"     to SnyggPercentageSizeValue(0.125f),
        "100.0%"    to SnyggPercentageSizeValue(1.0f),
    )

    private val invalidPairs = listOf(
        "-4.0dp"    to SnyggDpSizeValue((-4).dp),
        "-12.0dp"   to SnyggDpSizeValue((-12).dp),
        "-12.5dp"   to SnyggDpSizeValue((-12.5).dp),
        "-4.0sp"    to SnyggSpSizeValue((-4).sp),
        "-12.0sp"   to SnyggSpSizeValue((-12).sp),
        "-12.5sp"   to SnyggSpSizeValue((-12.5).sp),
        "-4.0%"     to SnyggPercentageSizeValue(-4.0f),
        "-12.0%"    to SnyggPercentageSizeValue(-12.0f),
        "-12.5%"    to SnyggPercentageSizeValue(-12.5f),
        "-100.0%"   to SnyggPercentageSizeValue(-100.0f),
        "101.0%"    to SnyggPercentageSizeValue(1.01f),
    )

    private val validIllFormattedInput = listOf(
        SnyggDpSizeValue(5.dp) to listOf(" 5dp", " 5dp ", " 5 dp", "5.dp", "5. dp"),
        SnyggDpSizeValue(0.5.dp) to listOf(".5dp", " .5dp"),
    )

    private val invalidInput = listOf(
        SnyggDpSizeValue to listOf(
            "", " ", "dp", "-dp", "5,0dp", "5Dp", "5DP", "5dP", "5d p", "5sp", "5px", "5%", "5",
        ),
        SnyggSpSizeValue to listOf(
            "", " ", "sp", "-sp", "5,0sp", "5Sp", "5SP", "5sP", "5s p", "5dp", "5px", "5%", "5",
        ),
    )

    @Test
    fun `test deserialize and serialize with valid input`() {
        assertAll(validPairs.map { (raw, obj) -> {
            val deserialized = obj.encoder().deserialize(raw)
            assertTrue { deserialized.isSuccess }
            assertEquals(obj, deserialized.getOrNull())
            val serialized = obj.encoder().serialize(obj)
            assertTrue { serialized.isSuccess }
        } })
    }

    @Test
    fun `test deserialize and serialize with invalid input`() {
        assertAll(invalidPairs.map { (raw, obj) -> {
            val deserialized = obj.encoder().deserialize(raw)
            assertFalse { deserialized.isSuccess }
            val serialized = obj.encoder().serialize(obj)
            assertFalse { serialized.isSuccess }
        } })
    }

    @Test
    fun `test deserialize with valid, ill-formatted input`() {
        assertAll(validIllFormattedInput.map { (obj, list) -> {
            assertAll(list.map { raw -> {
                val deserialized = obj.encoder().deserialize(raw)
                assertEquals(obj, deserialized.getOrNull())
            } })
        } })
    }

    @Test
    fun `test deserialize with invalid input`() {
        assertAll(invalidInput.map { (encoder, list) -> {
            assertAll(list.map { raw -> {
                val deserialized = encoder.deserialize(raw)
                assertFalse { deserialized.isSuccess }
            } })
        } })
    }
}
