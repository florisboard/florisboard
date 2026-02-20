/*
 * Copyright (C) 2025 SpeekEZ
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

package com.speekez.voice

import android.Manifest
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class PermissionUtilsTest {

    private lateinit var context: Context
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor

    @BeforeEach
    fun setUp() {
        context = mockk()
        sharedPreferences = mockk()
        editor = mockk()

        every { context.getSharedPreferences("speekez_permissions", Context.MODE_PRIVATE) } returns sharedPreferences
        every { sharedPreferences.edit() } returns editor
        every { editor.putBoolean(any(), any()) } returns editor
        every { editor.apply() } returns Unit
    }

    @Test
    fun `isMicPermissionPreviouslyDenied returns true when preference is false`() {
        every { sharedPreferences.getBoolean("speekez_mic_permission_granted", false) } returns false

        val result = PermissionUtils.isMicPermissionPreviouslyDenied(context)

        assertEquals(true, result)
    }

    @Test
    fun `isMicPermissionPreviouslyDenied returns false when preference is true`() {
        every { sharedPreferences.getBoolean("speekez_mic_permission_granted", false) } returns true

        val result = PermissionUtils.isMicPermissionPreviouslyDenied(context)

        assertEquals(false, result)
    }

    @Test
    fun `saveMicPermissionResult saves true to preferences`() {
        PermissionUtils.saveMicPermissionResult(context, true)

        verify { editor.putBoolean("speekez_mic_permission_granted", true) }
        verify { editor.apply() }
    }

    @Test
    fun `saveMicPermissionResult saves false to preferences`() {
        PermissionUtils.saveMicPermissionResult(context, false)

        verify { editor.putBoolean("speekez_mic_permission_granted", false) }
        verify { editor.apply() }
    }

    @Test
    fun `hasMicPermission returns true when permission is granted`() {
        mockkStatic(ContextCompat::class)
        every { ContextCompat.checkSelfPermission(any(), Manifest.permission.RECORD_AUDIO) } returns PackageManager.PERMISSION_GRANTED

        val result = PermissionUtils.hasMicPermission(context)

        assertEquals(true, result)
        unmockkStatic(ContextCompat::class)
    }

    @Test
    fun `hasMicPermission returns false when permission is denied`() {
        mockkStatic(ContextCompat::class)
        every { ContextCompat.checkSelfPermission(any(), Manifest.permission.RECORD_AUDIO) } returns PackageManager.PERMISSION_DENIED

        val result = PermissionUtils.hasMicPermission(context)

        assertEquals(false, result)
        unmockkStatic(ContextCompat::class)
    }
}
