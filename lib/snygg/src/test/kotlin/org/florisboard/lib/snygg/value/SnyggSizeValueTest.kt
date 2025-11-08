package org.florisboard.lib.snygg.value

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.junit.jupiter.api.assertAll
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
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
        SnyggDpSizeValue(5.dp) to listOf(" 5dp", " 5dp ", " 5dp", "5.dp", "5.dp"),
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
    fun `check class of default value`() {
        assertIs<SnyggPercentageSizeValue>(SnyggPercentageSizeValue.defaultValue())
        assertIs<SnyggSpSizeValue>(SnyggSpSizeValue.defaultValue())
        assertIs<SnyggDpSizeValue>(SnyggDpSizeValue.defaultValue())
    }

    @Test
    fun `check if value is type when deserializing`() {
        val sp = "-4.0sp"
        val dp = "-4.0dp"
        val percent = "-4.0%"
        assertEquals(null, SnyggSpSizeValue.deserialize(sp).getOrNull())
        assertEquals(null, SnyggDpSizeValue.deserialize(dp).getOrNull())
        assertEquals(null, SnyggPercentageSizeValue.deserialize(percent).getOrNull())
    }

    @Test
    fun `check if value is type when serializing`() {
        val sp = SnyggDefinedVarValue("shenanigans")
        val dp = SnyggDefinedVarValue("shenanigans")
        val percent = SnyggDefinedVarValue("shenanigans")
        assertEquals(null, SnyggSpSizeValue.serialize(sp).getOrNull())
        assertEquals(null, SnyggDpSizeValue.serialize(dp).getOrNull())
        assertEquals(null, SnyggPercentageSizeValue.serialize(percent).getOrNull())
    }

    @Test
    fun `test deserialize and serialize with valid input`() {
        assertAll(validPairs.map { (raw, obj) -> {
            val deserialized = obj.encoder().deserialize(raw)
            assertTrue("deserialize $raw") { deserialized.isSuccess }
            assertEquals(obj, deserialized.getOrNull())
            val serialized = obj.encoder().serialize(obj)
            assertTrue("serialize $raw") { serialized.isSuccess }
        } })
    }

    @Test
    fun `test deserialize and serialize with invalid input`() {
        assertAll(invalidPairs.map { (raw, obj) -> {
            val deserialized = obj.encoder().deserialize(raw)
            assertFalse("deserialize $raw should fail") { deserialized.isSuccess }
            val serialized = obj.encoder().serialize(obj)
            assertFalse("serialize $obj should fail") { serialized.isSuccess }
        } })
    }

    @Test
    fun `test deserialize with valid, ill-formatted input`() {
        assertAll(validIllFormattedInput.map { (obj, list) -> {
            assertAll(list.map { raw -> {
                val deserialized = obj.encoder().deserialize(raw)
                assertEquals(obj, deserialized.getOrNull(), "deserialize $raw")
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
