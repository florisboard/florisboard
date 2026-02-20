package com.speekez.security

import android.content.SharedPreferences
import com.speekez.core.ApiMode
import com.speekez.core.ModelTier
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class EncryptedPreferencesManagerTest {

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor
    private lateinit var manager: EncryptedPreferencesManager

    @BeforeEach
    fun setup() {
        sharedPreferences = mockk(relaxed = true)
        editor = mockk(relaxed = true)
        every { sharedPreferences.edit() } returns editor
        manager = EncryptedPreferencesManager(sharedPreferences)
    }

    @Test
    fun `saveOpenRouterKey with valid prefix returns true and saves key`() {
        val key = "sk-or-testkey"
        val result = manager.saveOpenRouterKey(key)
        assertTrue(result)
        verify { editor.putString("openrouter_api_key", key) }
    }

    @Test
    fun `saveOpenRouterKey with invalid prefix returns false and does not save`() {
        val key = "invalid-testkey"
        val result = manager.saveOpenRouterKey(key)
        assertFalse(result)
        verify(exactly = 0) { editor.putString("openrouter_api_key", any()) }
    }

    @Test
    fun `saveOpenAiKey with valid prefix returns true and saves key`() {
        val key = "sk-testkey"
        val result = manager.saveOpenAiKey(key)
        assertTrue(result)
        verify { editor.putString("openai_api_key", key) }
    }

    @Test
    fun `saveAnthropicKey with valid prefix returns true and saves key`() {
        val key = "sk-ant-testkey"
        val result = manager.saveAnthropicKey(key)
        assertTrue(result)
        verify { editor.putString("anthropic_api_key", key) }
    }

    @Test
    fun `getOpenRouterKey returns saved key`() {
        every { sharedPreferences.getString("openrouter_api_key", null) } returns "sk-or-saved"
        assertEquals("sk-or-saved", manager.getOpenRouterKey())
    }

    @Test
    fun `clearOpenRouterKey removes key`() {
        manager.clearOpenRouterKey()
        verify { editor.remove("openrouter_api_key") }
    }

    @Test
    fun `saveApiMode saves mode name`() {
        manager.saveApiMode(ApiMode.SEPARATE)
        verify { editor.putString("api_mode", "SEPARATE") }
    }

    @Test
    fun `getApiMode returns saved mode`() {
        every { sharedPreferences.getString("api_mode", null) } returns "OPENROUTER"
        assertEquals(ApiMode.OPENROUTER, manager.getApiMode())
    }

    @Test
    fun `getApiMode returns NO_KEYS if nothing saved`() {
        every { sharedPreferences.getString("api_mode", null) } returns null
        assertEquals(ApiMode.NO_KEYS, manager.getApiMode())
    }

    @Test
    fun `saveModelTier saves tier name`() {
        manager.saveModelTier(ModelTier.BEST)
        verify { editor.putString("model_tier", "BEST") }
    }

    @Test
    fun `getModelTier returns saved tier`() {
        every { sharedPreferences.getString("model_tier", null) } returns "CUSTOM"
        assertEquals(ModelTier.CUSTOM, manager.getModelTier())
    }

    @Test
    fun `saveCustomSttModel saves model name`() {
        manager.saveCustomSttModel("whisper-1")
        verify { editor.putString("custom_stt_model", "whisper-1") }
    }

    @Test
    fun `getCustomSttModel returns saved model`() {
        every { sharedPreferences.getString("custom_stt_model", null) } returns "whisper-1"
        assertEquals("whisper-1", manager.getCustomSttModel())
    }

    @Test
    fun `saveCustomRefinementModel saves model name`() {
        manager.saveCustomRefinementModel("claude-3")
        verify { editor.putString("custom_refinement_model", "claude-3") }
    }

    @Test
    fun `getCustomRefinementModel returns saved model`() {
        every { sharedPreferences.getString("custom_refinement_model", null) } returns "claude-3"
        assertEquals("claude-3", manager.getCustomRefinementModel())
    }

    @Test
    fun `hasOpenRouterKey returns true if key exists`() {
        every { sharedPreferences.contains("openrouter_api_key") } returns true
        assertTrue(manager.hasOpenRouterKey())
    }

    @Test
    fun `hasOpenAiKey returns true if key exists`() {
        every { sharedPreferences.contains("openai_api_key") } returns true
        assertTrue(manager.hasOpenAiKey())
    }

    @Test
    fun `hasAnthropicKey returns true if key exists`() {
        every { sharedPreferences.contains("anthropic_api_key") } returns true
        assertTrue(manager.hasAnthropicKey())
    }

    @Test
    fun `clearOpenAiKey removes key`() {
        manager.clearOpenAiKey()
        verify { editor.remove("openai_api_key") }
    }

    @Test
    fun `clearAnthropicKey removes key`() {
        manager.clearAnthropicKey()
        verify { editor.remove("anthropic_api_key") }
    }
}
