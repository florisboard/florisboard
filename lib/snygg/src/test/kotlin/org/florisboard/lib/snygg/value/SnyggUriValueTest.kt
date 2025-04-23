package org.florisboard.lib.snygg.value

import org.junit.jupiter.api.assertAll
import java.net.URI
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class SnyggUriValueTest {
    private val encoder = SnyggUriValue

    private fun helperMakeUri(uri: String): SnyggUriValue {
        return SnyggUriValue(URI.create(uri))
    }

    @Test
    fun `deserialize uri values`() {
        val pairs = listOf(
            // valid
            "uri(`flex:/my_image.png`)" to helperMakeUri("flex:/my_image.png"),
            "uri(`flex:/roboto.ttf`)" to helperMakeUri("flex:/roboto.ttf"),
            // invalid
            "some-color" to null,
        )
        assertAll(pairs.map { (raw, expected) -> {
            assertEquals(expected, encoder.deserialize(raw).getOrNull(), "deserialize $raw")
        } })
    }

    @Test
    fun `serialize uri values`() {
        val pairs = listOf(
            // valid
            helperMakeUri("flex:/my_image.png") to "uri(`flex:/my_image.png`)",
            helperMakeUri("flex:/roboto.ttf") to "uri(`flex:/roboto.ttf`)",
            // invalid
            SnyggDefinedVarValue("shenanigans") to null
        )
        assertAll(pairs.map { (snyggValue, expected) -> {
            assertEquals(expected, encoder.serialize(snyggValue).getOrNull(), "serialize $snyggValue")
        } })
    }

    @Test
    fun `check class of default value`() {
        assertIs<SnyggUriValue>(encoder.defaultValue())
    }
}
