package dev.patrickgold.florisboard.ime.theme

import android.graphics.Color
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Config.OLDEST_SDK])
class ThemeValueTest {
    @Test
    fun testFromString() {
        assertThat(ThemeValue.fromString("@abc/def"), `is`(ThemeValue.Reference("abc", "def")))
        assertThat(ThemeValue.fromString("@/def"), `is`(ThemeValue.Other("@/def")))
        assertThat(ThemeValue.fromString("@abc/"), `is`(ThemeValue.Other("@abc/")))
        assertThat(ThemeValue.fromString("#123456"), `is`(ThemeValue.SolidColor(Color.rgb(0x12, 0x34, 0x56))))
        assertThat(ThemeValue.fromString("#A2D4E5"), `is`(ThemeValue.SolidColor(Color.rgb(0xA2, 0xD4, 0xE5))))
        assertThat(ThemeValue.fromString("#AB123456"), `is`(ThemeValue.SolidColor(Color.argb(0xAB, 0x12, 0x34, 0x56))))
        assertThat(ThemeValue.fromString("#AB12345"), `is`(ThemeValue.Other("#AB12345")))
    }

    @Test
    fun testToString() {
        assertThat(ThemeValue.Reference("abc", "def").toString(), `is`("@abc/def"))
        assertThat(ThemeValue.SolidColor(Color.rgb(0x34, 0x56, 0xAB)).toString(), `is`("#FF3456AB"))
    }

    @Test
    fun testToSolidColor() {
        assertThat(ThemeValue.Reference("abc", "def").toSolidColor(), `is`(ThemeValue.SolidColor(0)))
        assertThat(ThemeValue.SolidColor(Color.rgb(0x34, 0x56, 0xAB)).toSolidColor(), `is`(ThemeValue.SolidColor(Color.rgb(0x34, 0x56, 0xAB))))
    }
}