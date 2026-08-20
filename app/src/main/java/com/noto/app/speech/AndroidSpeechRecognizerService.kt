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

/**
 * Long-form voice capture that keeps the microphone open until [finishListening] is called.
 *
 * Android's [SpeechRecognizer] insists on ending a session on silence (the timeout hints are only
 * respected sporadically). We work around that by keeping a rolling transcript in [accumulated]
 * and transparently restarting the recognizer on every terminal event (Final / NO_MATCH /
 * SPEECH_TIMEOUT) until the caller explicitly asks to stop.
 */
class AndroidSpeechRecognizerService(private val context: Context) : SpeechToTextService {

    @Volatile private var current: SpeechRecognizer? = null
    @Volatile private var stopRequested: Boolean = false
    private val mainHandler = Handler(Looper.getMainLooper())

    override fun isAvailable(): Boolean = SpeechRecognizer.isRecognitionAvailable(context)

    override fun finishListening() {
        stopRequested = true
        val r = current ?: return
        mainHandler.post {
            try { r.stopListening() } catch (_: Throwable) { /* no-op */ }
        }
    }

    override fun listen(languageTag: String?): Flow<SpeechEvent> = callbackFlow {
        stopRequested = false
        val lang = languageTag ?: Locale.getDefault().toLanguageTag()

        // Accumulated finalized fragments. Latest partial is added on top before emitting.
        val accumulated = StringBuilder()

        // Fatal errors that should surface to the user and close the flow.
        val fatalErrors = setOf(
            SpeechRecognizer.ERROR_AUDIO,
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS,
            SpeechRecognizer.ERROR_CLIENT,
        )

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, lang)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, false)
        }

        fun combined(partial: String = ""): String {
            val base = accumulated.toString().trim()
            val extra = partial.trim()
            return when {
                base.isEmpty() -> extra
                extra.isEmpty() -> base
                else -> "$base $extra"
            }
        }

        // Restart with a small delay — some devices report BUSY otherwise.
        fun scheduleRestart() {
            if (stopRequested) return
            mainHandler.postDelayed({
                if (stopRequested) return@postDelayed
                val r = current ?: return@postDelayed
                try { r.startListening(intent) } catch (_: Throwable) { /* no-op */ }
            }, 250)
        }

        val listener = object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) { trySend(SpeechEvent.ReadyForSpeech) }
            override fun onBeginningOfSpeech() { trySend(SpeechEvent.BeginningOfSpeech) }
            override fun onRmsChanged(rmsdB: Float) { trySend(SpeechEvent.Rms(rmsdB)) }
            override fun onBufferReceived(buffer: ByteArray?) { /* no-op */ }
            override fun onEndOfSpeech() { /* wait for onResults */ }

            override fun onError(error: Int) {
                if (error in fatalErrors) {
                    trySend(SpeechEvent.Error(error, errorMessage(error)))
                    close()
                    return
                }
                // NO_MATCH / SPEECH_TIMEOUT / NETWORK / BUSY / SERVER — treat as silence, restart.
                if (stopRequested) {
                    trySend(SpeechEvent.Final(combined()))
                    close()
                } else {
                    scheduleRestart()
                }
            }

            override fun onResults(results: Bundle?) {
                val text = results
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                    .orEmpty()
                    .trim()
                if (text.isNotEmpty()) {
                    if (accumulated.isNotEmpty()) accumulated.append(' ')
                    accumulated.append(text)
                    trySend(SpeechEvent.Partial(combined()))
                }
                if (stopRequested) {
                    trySend(SpeechEvent.Final(combined()))
                    close()
                } else {
                    scheduleRestart()
                }
            }

            override fun onPartialResults(partial: Bundle?) {
                val text = partial
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                    .orEmpty()
                if (text.isNotBlank()) trySend(SpeechEvent.Partial(combined(text)))
            }

            override fun onEvent(eventType: Int, params: Bundle?) { /* no-op */ }
        }

        val recognizer: SpeechRecognizer = withContext(Dispatchers.Main) {
            SpeechRecognizer.createSpeechRecognizer(context)
        }
        current = recognizer

        launch(Dispatchers.Main) {
            recognizer.setRecognitionListener(listener)
            recognizer.startListening(intent)
        }

        awaitClose {
            stopRequested = true
            mainHandler.removeCallbacksAndMessages(null)
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
