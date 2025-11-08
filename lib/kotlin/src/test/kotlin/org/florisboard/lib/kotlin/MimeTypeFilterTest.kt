/*
 * Copyright (C) 2025 The FlorisBoard Contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.florisboard.lib.kotlin

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class MimeTypeFilterTest {
    @Nested
    inner class BasicMatching {
        @ParameterizedTest
        @ValueSource(strings = [
            "font/woff2",
            "image/png",
            "application/x-font-otf",
        ])
        fun `valid matches`(mimeType: String) {
            val filter = mimeTypeFilterOf(mimeType)
            assertTrue { filter.matches(mimeType) }
        }

        @Test
        fun `null does not match`() {
            val filter = mimeTypeFilterOf("image/png")
            assertFalse { filter.matches(null) }
        }

        @Test
        fun `empty string does not match`() {
            val filter = mimeTypeFilterOf("image/png")
            assertFalse { filter.matches("") }
        }

        @Test
        fun `blank string does not match`() {
            val filter = mimeTypeFilterOf("image/png")
            assertFalse { filter.matches("   ") }
        }
    }

    @Nested
    inner class WildcardMatching {
        @ParameterizedTest
        @ValueSource(strings = [
            "image/png",
            "image/jpeg",
            "font/woff2",
            "application/x-font-otf",
        ])
        fun `should match type=any subtype=any`(mimeType: String) {
            val filter = mimeTypeFilterOf("*/*")
            assertTrue { filter.matches(mimeType) }
        }

        @ParameterizedTest
        @ValueSource(strings = [
            "",
            "   ",
            "/",
            "   /",
            "/    ",
            "image/",
            "/jpeg",
            "image/   ",
            "   /jpeg",
            "image/png/jpeg",
            "image-jpeg",
        ])
        fun `should not match type=any subtype=any`(mimeType: String) {
            val filter = mimeTypeFilterOf("*/*")
            assertFalse { filter.matches(mimeType) }
        }

        @ParameterizedTest
        @ValueSource(strings = [
            "image/png",
            "image/jpeg",
        ])
        fun `should match type=image subtype=any`(mimeType: String) {
            val filter = mimeTypeFilterOf("image/*")
            assertTrue { filter.matches(mimeType) }
        }

        @Test
        fun `legacy otf file should work with wildcard filters`() {
            // https://github.com/florisboard/florisboard/issues/2957
            val filter = mimeTypeFilterOf(
                "font/*",
                "application/font-*",
                "application/x-font-*",
                "application/vnd.ms-fontobject",
            )
            assertTrue { filter.matches("application/x-font-otf") }
        }

        @Test
        fun `should match type=any subtype=font-any`() {
            val filter = mimeTypeFilterOf(
                "*/x-font-*",
            )
            assertTrue { filter.matches("application/x-font-otf") }
        }

        @Test
        fun `should match type=application subtype=any-font-any`() {
            val filter = mimeTypeFilterOf(
                "application/*-font-*",
            )
            assertTrue { filter.matches("application/x-font-otf") }
        }

        @Test
        fun `should match type=any-application-any subtype=any-font-any`() {
            val filter = mimeTypeFilterOf(
                "*-application-*/*-font-*",
            )
            assertTrue { filter.matches("x-application-custom/x-font-otf") }
        }
    }
}
