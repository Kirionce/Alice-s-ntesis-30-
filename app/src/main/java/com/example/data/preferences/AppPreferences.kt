package com.example.data.preferences

import android.content.Context
import com.example.data.ai.GeminiModelOption

class AppPreferences(context: Context) {

    companion object {
        private const val PREFS_NAME = "alice_yui_app_preferences"
        private const val LEGACY_VAULT_PREFS = "alice_yui_secure_vault"

        private const val PREF_AFFECTIONATE_MODE = "affectionate_mode"
        private const val PREF_AUTO_VOICE = "auto_voice_reply"
        private const val PREF_GEMINI_MODEL = "selected_gemini_model"
    }

    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    init {
        // Purge any legacy API key vault preferences that may have existed previously
        appContext.getSharedPreferences(LEGACY_VAULT_PREFS, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .apply()
    }

    fun isAffectionateModeEnabled(): Boolean = prefs.getBoolean(PREF_AFFECTIONATE_MODE, true)

    fun setAffectionateModeEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(PREF_AFFECTIONATE_MODE, enabled).apply()
    }

    fun isAutoVoiceEnabled(): Boolean = prefs.getBoolean(PREF_AUTO_VOICE, true)

    fun setAutoVoiceEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(PREF_AUTO_VOICE, enabled).apply()
    }

    fun getSelectedModel(): GeminiModelOption =
        GeminiModelOption.fromModelId(prefs.getString(PREF_GEMINI_MODEL, GeminiModelOption.DEFAULT.modelId))

    fun setSelectedModel(model: GeminiModelOption) {
        prefs.edit().putString(PREF_GEMINI_MODEL, model.modelId).apply()
    }
}
