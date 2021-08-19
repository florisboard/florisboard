package dev.patrickgold.florisboard.ime.core

import android.app.Application
import android.inputmethodservice.InputMethodService
import android.view.inputmethod.ExtractedText
import dev.patrickgold.florisboard.ime.keyboard.KeyboardState
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals

/**
 * Unit test for [EditorInstance].
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class EditorInstanceTest {
    private val editorInstance = EditorInstance(InputMethodService(), KeyboardState.new())

    @Test
    fun `Test normalizeBounds`() {
        assertEquals(
            expected = EditorInstance.Bounds(3, 5),
            actual = editorInstance.normalizeBounds(3, 5),
            message = "Bounds order input is already normalized and must match output, but is different."
        )
        assertEquals(
            expected = EditorInstance.Bounds(3, 5),
            actual = editorInstance.normalizeBounds(5, 3),
            message = "Bounds order input is not normalized and start/end must be swapped, but they aren't."
        )
        assertEquals(
            expected = EditorInstance.Bounds(5, 5),
            actual = editorInstance.normalizeBounds(5, 5),
            message = "Bounds input start and end is the same number, output should match too but doesn't."
        )
    }

    @Test
    fun `Test ExtractedText$isPartialChange`() {
        assertEquals(
            expected = true,
            actual = editorInstance.run {
                ExtractedText().apply { partialStartOffset = 3; partialEndOffset = 5 }.isPartialChange()
            },
            message = "Is partial change fails for 3/5"
        )
        assertEquals(
            expected = false,
            actual = editorInstance.run {
                ExtractedText().apply { partialStartOffset = -1; partialEndOffset = -1 }.isPartialChange()
            },
            message = "Is partial change fails for -1/-1"
        )
        assertEquals(
            expected = false,
            actual = editorInstance.run {
                ExtractedText().apply { partialStartOffset = -1; partialEndOffset = 5 }.isPartialChange()
            },
            message = "Is partial change fails for -1/5"
        )
        assertEquals(
            expected = false,
            actual = editorInstance.run {
                ExtractedText().apply { partialStartOffset = 3; partialEndOffset = -1 }.isPartialChange()
            },
            message = "Is partial change fails for 3/-1"
        )
    }

    @Test
    fun `Test Bounds$equals`() {
        assertEquals(
            expected = true,
            actual = EditorInstance.Bounds(3, 5) == EditorInstance.Bounds(3, 5),
            message = "Equals for Bounds fails."
        )
        assertEquals(
            expected = false,
            actual = EditorInstance.Bounds(3, 5) == EditorInstance.Bounds(3, 4),
            message = "Equals for Bounds fails."
        )
        assertEquals(
            expected = false,
            actual = EditorInstance.Bounds(4, 5) == EditorInstance.Bounds(3, 5),
            message = "Equals for Bounds fails."
        )
        assertEquals(
            expected = false,
            actual = EditorInstance.Bounds(1, 2) == EditorInstance.Bounds(3, 4),
            message = "Equals for Bounds fails."
        )
    }

    @Test
    fun `Test Bounds$component1`() {
        assertEquals(
            expected = 3,
            actual = EditorInstance.Bounds(3, 5).component1(),
            message = "component1 for Bounds fails."
        )
    }

    @Test
    fun `Test Bounds$component2`() {
        assertEquals(
            expected = 5,
            actual = EditorInstance.Bounds(3, 5).component2(),
            message = "component2 for Bounds fails."
        )
    }
}
