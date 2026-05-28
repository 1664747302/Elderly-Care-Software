package com.example.elderreminder

import android.content.Context
import android.media.MediaPlayer
import android.speech.tts.TextToSpeech
import java.io.File
import java.util.Locale

object SpeechReminder : TextToSpeech.OnInitListener {
    private var textToSpeech: TextToSpeech? = null
    private var pendingMessage: String? = null
    private var mediaPlayer: MediaPlayer? = null

    fun speak(context: Context, message: String, isCurfew: Boolean) {
        val settings = AppSettings(context)
        if (settings.customAudioEnabled) {
            val audioFile = getCustomAudioFile(context, isCurfew)
            if (audioFile.exists()) {
                playAudioFile(audioFile.absolutePath)
                return
            }
        }

        // 降级使用原本的 TTS
        pendingMessage = message
        val existing = textToSpeech
        if (existing == null) {
            textToSpeech = TextToSpeech(context.applicationContext, this)
        } else {
            existing.speak(message, TextToSpeech.QUEUE_FLUSH, null, "elder-reminder")
        }
    }

    fun getCustomAudioFile(context: Context, isCurfew: Boolean): File {
        return getCustomAudioFile(context.filesDir, isCurfew)
    }

    fun getCustomAudioFile(filesDir: File, isCurfew: Boolean): File {
        if (isCurfew) {
            return File(filesDir, "custom_reminder_curfew.3gp")
        } else {
            val regular = File(filesDir, "custom_reminder_regular.3gp")
            if (!regular.exists()) {
                val old = File(filesDir, "custom_reminder.3gp")
                if (old.exists()) {
                    return old
                }
            }
            return regular
        }
    }

    private fun playAudioFile(path: String) {
        try {
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer().apply {
                setDataSource(path)
                prepare()
                setOnCompletionListener { mp ->
                    mp.release()
                    mediaPlayer = null
                }
                start()
            }
        } catch (e: Exception) {
            e.printStackTrace()
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
