package dev.patrickgold.florisboard.res

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class FlorisRefTest {
    companion object {
        private const val TEST_PATH = "ime/test"

        private const val MSG_INCORRECT_RESULT_FOR_ASSETS = "Incorrect result for ref with assets target"
        private const val MSG_INCORRECT_RESULT_FOR_CACHE = "Incorrect result for ref with cache target"
        private const val MSG_INCORRECT_RESULT_FOR_INTERNAL = "Incorrect result for ref with internal target"
        private const val MSG_INCORRECT_RESULT_FOR_EXTERNAL = "Incorrect result for ref with external target"
    }

    private val assetsRef = FlorisRef.assets(TEST_PATH)
    private val cacheRef = FlorisRef.cache(TEST_PATH)
    private val internalRef = FlorisRef.internal(TEST_PATH)
    private val externalRef = FlorisRef.from("content://authority/$TEST_PATH")

    @Test
    fun `Test isAssets`() {
        assertEquals(
            expected = true,
            actual = assetsRef.isAssets,
            message = MSG_INCORRECT_RESULT_FOR_ASSETS
        )
        assertEquals(
            expected = false,
            actual = cacheRef.isAssets,
            message = MSG_INCORRECT_RESULT_FOR_CACHE
        )
        assertEquals(
            expected = false,
            actual = internalRef.isAssets,
            message = MSG_INCORRECT_RESULT_FOR_INTERNAL
        )
        assertEquals(
            expected = false,
            actual = externalRef.isAssets,
            message = MSG_INCORRECT_RESULT_FOR_EXTERNAL
        )
    }

    @Test
    fun `Test isCache`() {
        assertEquals(
            expected = false,
            actual = assetsRef.isCache,
            message = MSG_INCORRECT_RESULT_FOR_ASSETS
        )
        assertEquals(
            expected = true,
            actual = cacheRef.isCache,
            message = MSG_INCORRECT_RESULT_FOR_CACHE
        )
        assertEquals(
            expected = false,
            actual = internalRef.isCache,
            message = MSG_INCORRECT_RESULT_FOR_INTERNAL
        )
        assertEquals(
            expected = false,
            actual = externalRef.isCache,
            message = MSG_INCORRECT_RESULT_FOR_EXTERNAL
        )
    }

    @Test
    fun `Test isInternal`() {
        assertEquals(
            expected = false,
            actual = assetsRef.isInternal,
            message = MSG_INCORRECT_RESULT_FOR_ASSETS
        )
        assertEquals(
            expected = false,
            actual = cacheRef.isInternal,
            message = MSG_INCORRECT_RESULT_FOR_CACHE
        )
        assertEquals(
            expected = true,
            actual = internalRef.isInternal,
            message = MSG_INCORRECT_RESULT_FOR_INTERNAL
        )
        assertEquals(
            expected = false,
            actual = externalRef.isInternal,
            message = MSG_INCORRECT_RESULT_FOR_EXTERNAL
        )
    }

    @Test
    fun `Test isExternal`() {
        assertEquals(
            expected = false,
            actual = assetsRef.isExternal,
            message = MSG_INCORRECT_RESULT_FOR_ASSETS
        )
        assertEquals(
            expected = false,
            actual = cacheRef.isExternal,
            message = MSG_INCORRECT_RESULT_FOR_CACHE
        )
        assertEquals(
            expected = false,
            actual = internalRef.isExternal,
            message = MSG_INCORRECT_RESULT_FOR_INTERNAL
        )
        assertEquals(
            expected = true,
            actual = externalRef.isExternal,
            message = MSG_INCORRECT_RESULT_FOR_EXTERNAL
        )
    }

    @Test
    fun `Test scheme`() {
        assertEquals(
            expected = FlorisRef.SCHEME_FLORIS,
            actual = assetsRef.scheme,
            message = MSG_INCORRECT_RESULT_FOR_ASSETS
        )
        assertEquals(
            expected = FlorisRef.SCHEME_FLORIS,
            actual = cacheRef.scheme,
            message = MSG_INCORRECT_RESULT_FOR_CACHE
        )
        assertEquals(
            expected = FlorisRef.SCHEME_FLORIS,
            actual = internalRef.scheme,
            message = MSG_INCORRECT_RESULT_FOR_INTERNAL
        )
        assertEquals(
            expected = "content",
            actual = externalRef.scheme,
            message = MSG_INCORRECT_RESULT_FOR_EXTERNAL
        )
    }

    @Test
    fun `Test authority`() {
        assertEquals(
            expected = FlorisRef.AUTHORITY_ASSETS,
            actual = assetsRef.authority,
            message = MSG_INCORRECT_RESULT_FOR_ASSETS
        )
        assertEquals(
            expected = FlorisRef.AUTHORITY_CACHE,
            actual = cacheRef.authority,
            message = MSG_INCORRECT_RESULT_FOR_CACHE
        )
        assertEquals(
            expected = FlorisRef.AUTHORITY_INTERNAL,
            actual = internalRef.authority,
            message = MSG_INCORRECT_RESULT_FOR_INTERNAL
        )
        assertEquals(
            expected = "authority",
            actual = externalRef.authority,
            message = MSG_INCORRECT_RESULT_FOR_EXTERNAL
        )
    }

    @Test
    fun `Test relativePath`() {
        assertEquals(
            expected = TEST_PATH,
            actual = assetsRef.relativePath,
            message = MSG_INCORRECT_RESULT_FOR_ASSETS
        )
        assertEquals(
            expected = TEST_PATH,
            actual = cacheRef.relativePath,
            message = MSG_INCORRECT_RESULT_FOR_CACHE
        )
        assertEquals(
            expected = TEST_PATH,
            actual = internalRef.relativePath,
            message = MSG_INCORRECT_RESULT_FOR_INTERNAL
        )
        assertEquals(
            expected = TEST_PATH,
            actual = externalRef.relativePath,
            message = MSG_INCORRECT_RESULT_FOR_EXTERNAL
        )
    }

    @Test
    fun `Test absolutePath(context)`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertEquals(
            expected = TEST_PATH,
            actual = assetsRef.absolutePath(context),
            message = MSG_INCORRECT_RESULT_FOR_ASSETS
        )
        assertEquals(
            expected = "${context.cacheDir}/$TEST_PATH",
            actual = cacheRef.absolutePath(context),
            message = MSG_INCORRECT_RESULT_FOR_CACHE
        )
        assertEquals(
            expected = "${context.filesDir}/$TEST_PATH",
            actual = internalRef.absolutePath(context),
            message = MSG_INCORRECT_RESULT_FOR_INTERNAL
        )
        assertEquals(
            expected = "/$TEST_PATH",
            actual = externalRef.absolutePath(context),
            message = MSG_INCORRECT_RESULT_FOR_EXTERNAL
        )
    }

    @Test
    fun `Test subRef(name)`() {
        val name = "hello"
        val newPath = "$TEST_PATH/$name"
        assertEquals(
            expected = FlorisRef.assets(newPath),
            actual = assetsRef.subRef(name),
            message = MSG_INCORRECT_RESULT_FOR_ASSETS
        )
        assertEquals(
            expected = FlorisRef.cache(newPath),
            actual = cacheRef.subRef(name),
            message = MSG_INCORRECT_RESULT_FOR_CACHE
        )
        assertEquals(
            expected = FlorisRef.internal(newPath),
            actual = internalRef.subRef(name),
            message = MSG_INCORRECT_RESULT_FOR_INTERNAL
        )
        assertEquals(
            expected = FlorisRef.from("content://authority/$newPath"),
            actual = externalRef.subRef(name),
            message = MSG_INCORRECT_RESULT_FOR_EXTERNAL
        )
    }
}
