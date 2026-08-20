package com.noto.app.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.noto.app.data.prefs.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SettingsUiState(
    val apiKey: String = "",
    val baseUrl: String = "",
    val model: String = "",
    val theme: SettingsRepository.Theme = SettingsRepository.Theme.SYSTEM,
    val calendarSync: Boolean = false,
    val rhythm: SettingsRepository.RhythmProfile = SettingsRepository.RhythmProfile.DEFAULT,
)

class SettingsViewModel(private val repo: SettingsRepository) : ViewModel() {
    private val _state = MutableStateFlow(SettingsUiState())
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val ai = repo.currentAi()
            _state.value = _state.value.copy(apiKey = ai.apiKey, baseUrl = ai.baseUrl, model = ai.model)
        }
        viewModelScope.launch {
            repo.observeTheme().collect { theme -> _state.value = _state.value.copy(theme = theme) }
        }
        viewModelScope.launch {
            repo.observeCalendarSync().collect { on -> _state.value = _state.value.copy(calendarSync = on) }
        }
        viewModelScope.launch {
            repo.observeRhythm().collect { r -> _state.value = _state.value.copy(rhythm = r) }
        }
    }

    fun setWorkStart(hour: Int) {
        val r = _state.value.rhythm.copy(workStart = hour.coerceIn(0, 23))
        viewModelScope.launch { repo.setRhythm(r) }
    }
    fun setWorkEnd(hour: Int) {
        val r = _state.value.rhythm.copy(workEnd = hour.coerceIn(1, 24))
        viewModelScope.launch { repo.setRhythm(r) }
    }

    fun setApiKey(v: String) { _state.value = _state.value.copy(apiKey = v); viewModelScope.launch { repo.setApiKey(v) } }
    fun setBaseUrl(v: String) { _state.value = _state.value.copy(baseUrl = v); viewModelScope.launch { repo.setBaseUrl(v) } }
    fun setModel(v: String) { _state.value = _state.value.copy(model = v); viewModelScope.launch { repo.setModel(v) } }
    fun setTheme(t: SettingsRepository.Theme) { viewModelScope.launch { repo.setTheme(t) } }
    fun setCalendarSync(on: Boolean) { viewModelScope.launch { repo.setCalendarSync(on) } }
}
