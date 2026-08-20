package com.noto.app.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.noto.app.data.prefs.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
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
            _state.update { it.copy(apiKey = ai.apiKey, baseUrl = ai.baseUrl, model = ai.model) }
        }
        viewModelScope.launch {
            repo.observeTheme().collect { theme -> _state.update { it.copy(theme = theme) } }
        }
        viewModelScope.launch {
            repo.observeCalendarSync().collect { on -> _state.update { it.copy(calendarSync = on) } }
        }
        viewModelScope.launch {
            repo.observeRhythm().collect { r -> _state.update { it.copy(rhythm = r) } }
        }
    }

    fun setApiKey(v: String) {
        _state.update { it.copy(apiKey = v) }
        viewModelScope.launch { repo.setApiKey(v) }
    }
    fun setBaseUrl(v: String) {
        _state.update { it.copy(baseUrl = v) }
        viewModelScope.launch { repo.setBaseUrl(v) }
    }
    fun setModel(v: String) {
        _state.update { it.copy(model = v) }
        viewModelScope.launch { repo.setModel(v) }
    }
    fun setTheme(t: SettingsRepository.Theme) { viewModelScope.launch { repo.setTheme(t) } }
    fun setCalendarSync(on: Boolean) { viewModelScope.launch { repo.setCalendarSync(on) } }

    fun setWorkStart(hour: Int) {
        val current = _state.value.rhythm
        val newStart = hour.coerceIn(0, current.workEnd - 1)
        viewModelScope.launch { repo.setRhythm(current.copy(workStart = newStart)) }
    }
    fun setWorkEnd(hour: Int) {
        val current = _state.value.rhythm
        val newEnd = hour.coerceIn(current.workStart + 1, 24)
        viewModelScope.launch { repo.setRhythm(current.copy(workEnd = newEnd)) }
    }
}
