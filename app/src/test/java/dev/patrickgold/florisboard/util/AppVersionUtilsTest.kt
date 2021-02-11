package dev.patrickgold.florisboard.util

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import dev.patrickgold.florisboard.ime.core.TestablePrefHelper
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.nullValue
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.doReturn
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner.Silent::class)
class AppVersionUtilsTest {
    @Mock private lateinit var context: Context
    @Mock private lateinit var packageManager: PackageManager
    private lateinit var prefs: TestablePrefHelper

    @Before
    fun initMocks() {
        prefs = TestablePrefHelper(context)

        `when`(context.packageManager).thenReturn(packageManager)
        `when`(context.packageName).thenReturn("com.example.test")
        `when`(packageManager.getPackageInfo(ArgumentMatchers.anyString(), ArgumentMatchers.anyInt())).thenReturn(
            PackageInfo().apply {
                versionName = "1.2.3"
            }
        )
    }

    @Test
    fun testGetRawVersionName() {
        assertThat(AppVersionUtils.getRawVersionName(context), `is`("1.2.3"))
    }

    @Test
    fun testShouldShowChangelog() {
        doReturn("1.2.2").`when`(prefs.internal).versionLastChangelog
        doReturn("0.0.0").`when`(prefs.internal).versionOnInstall
        assertThat(AppVersionUtils.shouldShowChangelog(context, prefs.instance), `is`(true))

        doReturn("1.2.3").`when`(prefs.internal).versionLastChangelog
        doReturn("0.0.0").`when`(prefs.internal).versionOnInstall
        assertThat(AppVersionUtils.shouldShowChangelog(context, prefs.instance), `is`(false))

        doReturn("0.0.0").`when`(prefs.internal).versionLastChangelog
        doReturn("1.2.3").`when`(prefs.internal).versionOnInstall
        assertThat(AppVersionUtils.shouldShowChangelog(context, prefs.instance), `is`(false))
    }

    @Test
    fun testVersionNameFromString() {
        // Test valid strings, must always return a VersionName object
        listOf(
            Pair("1.2.3", VersionName(1, 2, 3, null, null)),
            Pair("11.22.33", VersionName(11, 22, 33, null, null)),
            Pair("1.2.3.4", VersionName(1, 2, 3, null, 4)),
            Pair("11.22.33.44", VersionName(11, 22, 33, null, 44)),
            Pair("1.2.3.alpha", VersionName(1, 2, 3, "alpha", null)),
            Pair("11.22.33.alpha", VersionName(11, 22, 33, "alpha", null)),
            Pair("1.2.3.alpha4", VersionName(1, 2, 3, "alpha", 4)),
            Pair("11.22.33.alpha44", VersionName(11, 22, 33, "alpha", 44))
        ).forEach { (raw, expected) ->
            assertThat(VersionName.fromString(raw), `is`(expected))
        }

        // Test invalid strings, must always return null
        listOf("a.b.c", "abc", "1", "1.2", "1.2.3.4.5", "1..2.3").forEach {
            assertThat(VersionName.fromString(it), `is`(nullValue()))
        }
    }

    @Test
    fun testVersionNameToString() {
        listOf(
            Pair(VersionName(1, 2, 3, null, null), "1.2.3"),
            Pair(VersionName(11, 22, 33, null, null), "11.22.33"),
            Pair(VersionName(1, 2, 3, null, 4), "1.2.3.4"),
            Pair(VersionName(11, 22, 33, null, 44), "11.22.33.44"),
            Pair(VersionName(1, 2, 3, "alpha", null), "1.2.3.alpha"),
            Pair(VersionName(1, 2, 3, "alpha", 4), "1.2.3.alpha4"),
            Pair(VersionName(0, 0, 0, "c", 0), "0.0.0.c0")
        ).forEach { (raw, expected) ->
            assertThat(raw.toString(), `is`(expected))
        }
    }

    @Test
    fun testVersionNameCompareTo() {
        listOf(
            Triple(
                VersionName(1, 2, 3, null, null),
                VersionName(1, 2, 3, null, null),
                0
            ),
            Triple(
                VersionName(1, 2, 2, null, null),
                VersionName(1, 2, 3, null, null),
                -1
            ),
            Triple(
                VersionName(1, 2, 4, null, null),
                VersionName(1, 2, 3, null, null),
                1
            ),
            Triple(
                VersionName(1, 1, 3, null, null),
                VersionName(1, 2, 3, null, null),
                -1
            ),
            Triple(
                VersionName(1, 3, 3, null, null),
                VersionName(1, 2, 3, null, null),
                1
            ),
            Triple(
                VersionName(0, 2, 3, null, null),
                VersionName(1, 2, 3, null, null),
                -1
            ),
            Triple(
                VersionName(2, 2, 3, null, null),
                VersionName(1, 2, 3, null, null),
                1
            ),
            Triple(
                VersionName(1, 2, 3, null, 4),
                VersionName(1, 2, 3, null, 4),
                0
            ),
            Triple(
                VersionName(1, 2, 3, "alpha", 4),
                VersionName(1, 2, 3, "beta", 4),
                0
            )
        ).forEach { (vn, otherVn, expected) ->
            assertThat(vn.compareTo(otherVn), `is`(expected))
        }
    }
}
