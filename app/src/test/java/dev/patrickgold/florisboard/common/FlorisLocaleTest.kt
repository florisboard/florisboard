package dev.patrickgold.florisboard.common

import org.junit.Test
import java.util.*
import kotlin.test.assertEquals

/**
 * Unit test for [FlorisLocale].
 */
class FlorisLocaleTest {
    @Test
    fun `Test fromTag with dashes`() {
        assertEquals(
            expected = Locale("ja", "", ""),
            actual = FlorisLocale.fromTag("ja").base,
            message = "Incorrect tag parsing"
        )
        assertEquals(
            expected = Locale("ja", "JP", ""),
            actual = FlorisLocale.fromTag("ja-JP").base,
            message = "Incorrect tag parsing"
        )
        assertEquals(
            expected = Locale("ja", "JP", "jis"),
            actual = FlorisLocale.fromTag("ja-JP-jis").base,
            message = "Incorrect tag parsing"
        )
    }

    @Test
    fun `Test fromTag with underscore`() {
        assertEquals(
            expected = Locale("ja", "", ""),
            actual = FlorisLocale.fromTag("ja").base,
            message = "Incorrect tag parsing"
        )
        assertEquals(
            expected = Locale("ja", "JP", ""),
            actual = FlorisLocale.fromTag("ja_JP").base,
            message = "Incorrect tag parsing"
        )
        assertEquals(
            expected = Locale("ja", "JP", "jis"),
            actual = FlorisLocale.fromTag("ja_JP_jis").base,
            message = "Incorrect tag parsing"
        )
    }

    @Test
    fun `Test languageTag`() {
        assertEquals(
            expected = "ja",
            actual = FlorisLocale.from("ja", "", "").languageTag(),
            message = "Incorrect language tag constructing"
        )
        assertEquals(
            expected = "ja-JP",
            actual = FlorisLocale.from("ja", "JP", "").languageTag(),
            message = "Incorrect language tag constructing"
        )
        assertEquals(
            expected = "ja-JP-jis",
            actual = FlorisLocale.from("ja", "JP", "jis").languageTag(),
            message = "Incorrect language tag constructing"
        )
    }

    @Test
    fun `Test localeTag`() {
        assertEquals(
            expected = "ja",
            actual = FlorisLocale.from("ja", "", "").localeTag(),
            message = "Incorrect locale tag constructing"
        )
        assertEquals(
            expected = "ja_JP",
            actual = FlorisLocale.from("ja", "JP", "").localeTag(),
            message = "Incorrect locale tag constructing"
        )
        assertEquals(
            expected = "ja_JP_jis",
            actual = FlorisLocale.from("ja", "JP", "jis").localeTag(),
            message = "Incorrect locale tag constructing"
        )
    }
}
