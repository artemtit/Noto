package com.noto.app.speech

import kotlinx.coroutines.flow.Flow

sealed interface SpeechEvent {
    data object ReadyForSpeech : SpeechEvent
    data object BeginningOfSpeech : SpeechEvent
    data class Partial(val text: String) : SpeechEvent
    data class Rms(val db: Float) : SpeechEvent
    data class Final(val text: String) : SpeechEvent
    data class Error(val code: Int, val message: String) : SpeechEvent
}

interface SpeechToTextService {
    /** Returns true if a recognizer is available on the device. */
    fun isAvailable(): Boolean

    /**
     * Starts listening and emits [SpeechEvent]s. The stream completes after a Final
     * or Error event, or when the consumer cancels collection (which stops the recognizer).
     */
    fun listen(languageTag: String? = null): Flow<SpeechEvent>

    /** Explicitly finalize current recognition. Recognizer will emit its Final result. */
    fun finishListening()
}
