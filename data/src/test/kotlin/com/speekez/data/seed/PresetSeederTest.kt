package com.speekez.data.seed

import com.speekez.data.dao.PresetDao
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class PresetSeederTest {
    private lateinit var presetDao: PresetDao

    @BeforeEach
    fun setUp() {
        presetDao = mockk(relaxed = true)
    }

    @Test
    fun `seedDefaultPresetsIfEmpty inserts 3 presets when database is empty`() = runBlocking {
        // Given
        coEvery { presetDao.count() } returns 0

        // When
        PresetSeeder.seedDefaultPresetsIfEmpty(presetDao)

        // Then
        coVerify(exactly = 3) { presetDao.insertPreset(any()) }
    }

    @Test
    fun `seedDefaultPresetsIfEmpty does not insert when database is not empty`() = runBlocking {
        // Given
        coEvery { presetDao.count() } returns 5

        // When
        PresetSeeder.seedDefaultPresetsIfEmpty(presetDao)

        // Then
        coVerify(exactly = 0) { presetDao.insertPreset(any()) }
    }
}
