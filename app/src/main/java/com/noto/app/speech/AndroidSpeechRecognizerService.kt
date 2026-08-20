package com.noto.app.speech

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

class AndroidSpeechRecognizerService(private val context: Context) : SpeechToTextService {

    @Volatile private var current: SpeechRecognizer? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    override fun isAvailable(): Boolean = SpeechRecognizer.isRecognitionAvailable(context)

    override fun finishListening() {
        val r = current ?: return
        mainHandler.post {
            try { r.stopListening() } catch (_: Throwable) { /* no-op */ }
        }
    }

    override fun listen(languageTag: String?): Flow<SpeechEvent> = callbackFlow {
        val recognizer: SpeechRecognizer = withContext(Dispatchers.Main) {
            SpeechRecognizer.createSpeechRecognizer(context)
        }
        current = recognizer

        val listener = object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) { trySend(SpeechEvent.ReadyForSpeech) }
            override fun onBeginningOfSpeech() { trySend(SpeechEvent.BeginningOfSpeech) }
            override fun onRmsChanged(rmsdB: Float) { trySend(SpeechEvent.Rms(rmsdB)) }
            override fun onBufferReceived(buffer: ByteArray?) { /* no-op */ }
            override fun onEndOfSpeech() { /* wait for results */ }
            override fun onError(error: Int) {
                trySend(SpeechEvent.Error(error, errorMessage(error)))
                close()
            }
            override fun onResults(results: Bundle?) {
                val list = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val text = list?.firstOrNull().orEmpty()
                if (text.isBlank()) trySend(SpeechEvent.Error(-1, "empty"))
                else trySend(SpeechEvent.Final(text))
                close()
            }
            override fun onPartialResults(partial: Bundle?) {
                val list = partial?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val text = list?.firstOrNull().orEmpty()
                if (text.isNotBlank()) trySend(SpeechEvent.Partial(text))
            }
            override fun onEvent(eventType: Int, params: Bundle?) { /* no-op */ }
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE,
                languageTag ?: Locale.getDefault().toLanguageTag()
            )
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, false)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 60_000L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 60_000L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 120_000L)
        }

        launch(Dispatchers.Main) {
            recognizer.setRecognitionListener(listener)
            recognizer.startListening(intent)
        }

        awaitClose {
            try { recognizer.stopListening() } catch (_: Throwable) { /* no-op */ }
            try { recognizer.destroy() } catch (_: Throwable) { /* no-op */ }
            if (current === recognizer) current = null
        }
    }.flowOn(Dispatchers.Main)

    private fun errorMessage(code: Int): String = when (code) {
        SpeechRecognizer.ERROR_AUDIO -> "audio"
        SpeechRecognizer.ERROR_CLIENT -> "client"
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "permission"
        SpeechRecognizer.ERROR_NETWORK -> "network"
        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "timeout"
        SpeechRecognizer.ERROR_NO_MATCH -> "no_match"
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "busy"
        SpeechRecognizer.ERROR_SERVER -> "server"
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "speech_timeout"
        else -> "unknown_$code"
    }
}
