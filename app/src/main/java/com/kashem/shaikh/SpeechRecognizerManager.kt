package com.kashem.shaikh

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import kotlinx.coroutines.*
import java.util.Locale

class SpeechRecognizerManager(
    private val context: Context,
    private val onCommandRecognized: (String) -> Unit
) {
    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false
    private val mainScope = CoroutineScope(Dispatchers.Main + Job())

    fun initialize() {
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onError(error: Int) {
                restartListening()
            }
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val command = matches?.firstOrNull() ?: return
                // সরাসরি lowercase() ব্যবহার করুন (locale ছাড়া)
                onCommandRecognized(command.lowercase())
                restartListening()
            }
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
    }

    fun startListening() {
        if (isListening) return
        if (speechRecognizer == null) initialize()
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }
        speechRecognizer?.startListening(intent)
        isListening = true
    }

    fun stopListening() {
        speechRecognizer?.stopListening()
        isListening = false
    }

    private fun restartListening() {
        stopListening()
        mainScope.launch {
            delay(500)
            startListening()
        }
    }

    fun destroy() {
        stopListening()
        speechRecognizer?.destroy()
        speechRecognizer = null
        mainScope.cancel()
    }
}