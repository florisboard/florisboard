package com.speekez.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.speekez.core.ApiMode
import com.speekez.core.ModelTier

class EncryptedPreferencesManager(private val sharedPreferences: SharedPreferences) {

    constructor(context: Context) : this(
        EncryptedSharedPreferences.create(
            context,
            FILE_NAME,
            MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build(),
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    )

    companion object {
        private const val FILE_NAME = "speekez_secure_prefs"

        private const val KEY_OPENROUTER_API_KEY = "openrouter_api_key"
        private const val KEY_OPENAI_API_KEY = "openai_api_key"
        private const val KEY_ANTHROPIC_API_KEY = "anthropic_api_key"
        private const val KEY_API_MODE = "api_mode"
        private const val KEY_MODEL_TIER = "model_tier"
        private const val KEY_CUSTOM_STT_MODEL = "custom_stt_model"
        private const val KEY_CUSTOM_REFINEMENT_MODEL = "custom_refinement_model"
        private const val KEY_VOICE_COPY_TO_CLIPBOARD = "voice_copy_to_clipboard"

        private const val PREFIX_OPENROUTER = "sk-or-"
        private const val PREFIX_OPENAI = "sk-"
        private const val PREFIX_ANTHROPIC = "sk-ant-"
    }

    // OpenRouter Key
    fun saveOpenRouterKey(key: String): Boolean {
        if (!key.startsWith(PREFIX_OPENROUTER)) return false
        sharedPreferences.edit().putString(KEY_OPENROUTER_API_KEY, key).apply()
        return true
    }

    fun getOpenRouterKey(): String? = sharedPreferences.getString(KEY_OPENROUTER_API_KEY, null)

    fun hasOpenRouterKey(): Boolean = sharedPreferences.contains(KEY_OPENROUTER_API_KEY)

    fun clearOpenRouterKey() {
        sharedPreferences.edit().remove(KEY_OPENROUTER_API_KEY).apply()
    }

    // OpenAI Key
    fun saveOpenAiKey(key: String): Boolean {
        if (!key.startsWith(PREFIX_OPENAI)) return false
        // Ensure it doesn't accidentally match other prefixes if that's a concern, 
        // but sk- is the general OpenAI prefix.
        sharedPreferences.edit().putString(KEY_OPENAI_API_KEY, key).apply()
        return true
    }

    fun getOpenAiKey(): String? = sharedPreferences.getString(KEY_OPENAI_API_KEY, null)

    fun hasOpenAiKey(): Boolean = sharedPreferences.contains(KEY_OPENAI_API_KEY)

    fun clearOpenAiKey() {
        sharedPreferences.edit().remove(KEY_OPENAI_API_KEY).apply()
    }

    // Anthropic Key
    fun saveAnthropicKey(key: String): Boolean {
        if (!key.startsWith(PREFIX_ANTHROPIC)) return false
        sharedPreferences.edit().putString(KEY_ANTHROPIC_API_KEY, key).apply()
        return true
    }

    fun getAnthropicKey(): String? = sharedPreferences.getString(KEY_ANTHROPIC_API_KEY, null)

    fun hasAnthropicKey(): Boolean = sharedPreferences.contains(KEY_ANTHROPIC_API_KEY)

    fun clearAnthropicKey() {
        sharedPreferences.edit().remove(KEY_ANTHROPIC_API_KEY).apply()
    }

    // API Mode
    fun saveApiMode(mode: ApiMode) {
        sharedPreferences.edit().putString(KEY_API_MODE, mode.name).apply()
    }

    fun getApiMode(): ApiMode {
        val modeName = sharedPreferences.getString(KEY_API_MODE, null)
        return try {
            if (modeName != null) ApiMode.valueOf(modeName) else ApiMode.NO_KEYS
        } catch (e: IllegalArgumentException) {
            ApiMode.NO_KEYS
        }
    }

    // Model Tier
    fun saveModelTier(tier: ModelTier) {
        sharedPreferences.edit().putString(KEY_MODEL_TIER, tier.name).apply()
    }

    fun getModelTier(): ModelTier {
        val tierName = sharedPreferences.getString(KEY_MODEL_TIER, null)
        return try {
            if (tierName != null) ModelTier.valueOf(tierName) else ModelTier.CHEAP
        } catch (e: IllegalArgumentException) {
            ModelTier.CHEAP
        }
    }

    // Custom Models
    fun saveCustomSttModel(model: String) {
        sharedPreferences.edit().putString(KEY_CUSTOM_STT_MODEL, model).apply()
    }

    fun getCustomSttModel(): String? = sharedPreferences.getString(KEY_CUSTOM_STT_MODEL, null)

    fun saveCustomRefinementModel(model: String) {
        sharedPreferences.edit().putString(KEY_CUSTOM_REFINEMENT_MODEL, model).apply()
    }

    fun getCustomRefinementModel(): String? = sharedPreferences.getString(KEY_CUSTOM_REFINEMENT_MODEL, null)

    // Voice Settings
    fun setCopyToClipboard(enabled: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_VOICE_COPY_TO_CLIPBOARD, enabled).apply()
    }

    fun isCopyToClipboardEnabled(): Boolean = sharedPreferences.getBoolean(KEY_VOICE_COPY_TO_CLIPBOARD, false)
}
