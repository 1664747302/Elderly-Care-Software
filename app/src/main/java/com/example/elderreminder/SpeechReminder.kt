package com.example.elderreminder

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

object SpeechReminder : TextToSpeech.OnInitListener {
    private var textToSpeech: TextToSpeech? = null
    private var pendingMessage: String? = null

    fun speak(context: Context, message: String) {
        pendingMessage = message
        val existing = textToSpeech
        if (existing == null) {
            textToSpeech = TextToSpeech(context.applicationContext, this)
        } else {
            existing.speak(message, TextToSpeech.QUEUE_FLUSH, null, "elder-reminder")
        }
    }

    override fun onInit(status: Int) {
        val tts = textToSpeech ?: return
        if (status == TextToSpeech.SUCCESS) {
            tts.language = Locale.CHINA
            pendingMessage?.let {
                tts.speak(it, TextToSpeech.QUEUE_FLUSH, null, "elder-reminder")
            }
        }
    }
}
