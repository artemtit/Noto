package com.noto.app.ui.screens.voice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.noto.app.speech.SpeechEvent
import com.noto.app.speech.SpeechToTextService
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class VoiceUiState(
    val status: Status = Status.IDLE,
    val partial: String = "",
    val rms: Float = 0f,
    val error: String? = null,
    val finalText: String? = null,
) {
    enum class Status { IDLE, LISTENING, DONE, ERROR }
}

class VoiceCaptureViewModel(private val speech: SpeechToTextService) : ViewModel() {

    private val _state = MutableStateFlow(VoiceUiState())
    val state: StateFlow<VoiceUiState> = _state.asStateFlow()

    private var job: Job? = null

    fun isAvailable(): Boolean = speech.isAvailable()

    fun start(languageTag: String?) {
        stop()
        _state.value = VoiceUiState(status = VoiceUiState.Status.LISTENING)
        job = viewModelScope.launch {
            speech.listen(languageTag).collect { ev ->
                when (ev) {
                    SpeechEvent.ReadyForSpeech -> Unit
                    SpeechEvent.BeginningOfSpeech -> Unit
                    is SpeechEvent.Rms -> _state.value = _state.value.copy(rms = ev.db)
                    is SpeechEvent.Partial -> _state.value = _state.value.copy(partial = ev.text)
                    is SpeechEvent.Final -> _state.value = _state.value.copy(
                        status = VoiceUiState.Status.DONE,
                        finalText = ev.text,
                        partial = ev.text,
                    )
                    is SpeechEvent.Error -> _state.value = _state.value.copy(
                        status = VoiceUiState.Status.ERROR,
                        error = ev.message,
                    )
                }
            }
        }
    }

    /** Ask the recognizer to finalize what was spoken (does NOT cancel — waits for onResults). */
    fun finish() {
        speech.finishListening()
    }

    fun stop() {
        job?.cancel(); job = null
    }

    override fun onCleared() {
        stop(); super.onCleared()
    }
}
