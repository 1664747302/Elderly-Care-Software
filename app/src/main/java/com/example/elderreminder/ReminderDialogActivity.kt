package com.example.elderreminder

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.media.AudioManager
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class ReminderDialogActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Ensure the screen turns on and shows over lock screen
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
        
        val reminderText = intent.getStringExtra("reminder_text") ?: "该休息一下了"
        val isCurfew = intent.getBooleanExtra("is_curfew", false)

        // Force volume to max
        try {
            val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, maxVolume, 0)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Trigger voice alert
        val settings = AppSettings(this)
        if (settings.voiceEnabled) {
            SpeechReminder.speak(this, reminderText, isCurfew)
        }

        setContentView(buildContentView(reminderText, isCurfew))
    }

    private fun buildContentView(reminderText: String, isCurfew: Boolean): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(0xFFFAFAF7.toInt()) // Warm background
            setPadding(dp(24), dp(24), dp(24), dp(24))
        }

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            val bgDrawable = ContextCompat.getDrawable(this@ReminderDialogActivity, R.drawable.status_panel)
            background = bgDrawable
            setPadding(dp(24), dp(32), dp(24), dp(32))
        }

        val titleView = TextView(this).apply {
            text = if (isCurfew) "🌙 深夜护眼守护提醒" else "👀 连续用眼休息提醒"
            textSize = 26f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(ContextCompat.getColor(this@ReminderDialogActivity, R.color.brand_green_dark))
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, dp(16))
        }
        card.addView(titleView)

        val messageView = TextView(this).apply {
            text = reminderText
            textSize = 21f
            setTextColor(Color.DKGRAY)
            gravity = Gravity.CENTER
            setLineSpacing(0f, 1.3f)
            setPadding(0, 0, 0, dp(24))
        }
        card.addView(messageView)

        val btn = Button(this).apply {
            text = "我知道了，闭眼休息"
            textSize = 20f
            setTextColor(Color.WHITE)
            background = ContextCompat.getDrawable(this@ReminderDialogActivity, R.drawable.button_primary)
            setOnClickListener {
                finish()
            }
        }
        
        val btnParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        card.addView(btn, btnParams)

        val cardParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        root.addView(card, cardParams)

        return root
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
