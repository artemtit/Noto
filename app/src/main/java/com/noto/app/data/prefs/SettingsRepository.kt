package com.noto.app.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.noto.app.core.AppConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "noto_settings")

class SettingsRepository(private val context: Context) {

    private val keyApiKey = stringPreferencesKey("ai_api_key")
    private val keyBaseUrl = stringPreferencesKey("ai_base_url")
    private val keyModel = stringPreferencesKey("ai_model")
    private val keyTheme = stringPreferencesKey("theme")
    private val keyCalSync = booleanPreferencesKey("calendar_sync")
    private val keyWorkStart = intPreferencesKey("rhythm_work_start")
    private val keyWorkEnd = intPreferencesKey("rhythm_work_end")

    data class AiConfig(val apiKey: String, val baseUrl: String, val model: String)

    data class RhythmProfile(val workStart: Int, val workEnd: Int) {
        companion object { val DEFAULT = RhythmProfile(9, 21) }
    }

    fun observeAi(): Flow<AiConfig> = context.dataStore.data.map { prefs -> readAi(prefs) }

    suspend fun currentAi(): AiConfig = readAi(context.dataStore.data.first())

    private fun readAi(prefs: Preferences): AiConfig = AiConfig(
        apiKey = prefs[keyApiKey].orEmpty(),
        baseUrl = prefs[keyBaseUrl] ?: AppConfig.DEFAULT_AI_BASE_URL,
        model = prefs[keyModel] ?: AppConfig.DEFAULT_AI_MODEL,
    )

    suspend fun setApiKey(value: String) { context.dataStore.edit { it[keyApiKey] = value } }
    suspend fun setBaseUrl(value: String) { context.dataStore.edit { it[keyBaseUrl] = value } }
    suspend fun setModel(value: String) { context.dataStore.edit { it[keyModel] = value } }

    fun observeTheme(): Flow<Theme> = context.dataStore.data.map { Theme.fromKey(it[keyTheme]) }

    suspend fun setTheme(theme: Theme) { context.dataStore.edit { it[keyTheme] = theme.key } }

    fun observeCalendarSync(): Flow<Boolean> = context.dataStore.data.map { it[keyCalSync] ?: false }
    suspend fun isCalendarSyncEnabled(): Boolean = context.dataStore.data.first()[keyCalSync] ?: false
    suspend fun setCalendarSync(enabled: Boolean) { context.dataStore.edit { it[keyCalSync] = enabled } }

    fun observeRhythm(): Flow<RhythmProfile> = context.dataStore.data.map { readRhythm(it) }
    suspend fun currentRhythm(): RhythmProfile = readRhythm(context.dataStore.data.first())
    suspend fun setRhythm(profile: RhythmProfile) {
        context.dataStore.edit {
            it[keyWorkStart] = profile.workStart.coerceIn(0, 23)
            it[keyWorkEnd] = profile.workEnd.coerceIn(0, 24)
        }
    }
    private fun readRhythm(prefs: Preferences): RhythmProfile = RhythmProfile(
        workStart = prefs[keyWorkStart] ?: RhythmProfile.DEFAULT.workStart,
        workEnd = prefs[keyWorkEnd] ?: RhythmProfile.DEFAULT.workEnd,
    )

    enum class Theme(val key: String) {
        SYSTEM("system"), LIGHT("light"), DARK("dark");

        companion object {
            fun fromKey(k: String?): Theme = entries.firstOrNull { it.key == k } ?: SYSTEM
        }
    }
}
