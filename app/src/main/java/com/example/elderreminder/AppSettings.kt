package com.example.elderreminder

import android.content.Context

class AppSettings(context: Context) {
    private val preferences = context.getSharedPreferences("elder_reminder_settings", Context.MODE_PRIVATE)

    var pin: String
        get() = preferences.getString(KEY_PIN, DEFAULT_PIN) ?: DEFAULT_PIN
        set(value) = preferences.edit().putString(KEY_PIN, value).apply()

    var reminderMinutes: Int
        get() = preferences.getInt(KEY_REMINDER_MINUTES, DEFAULT_REMINDER_MINUTES)
        set(value) = preferences.edit().putInt(KEY_REMINDER_MINUTES, value.coerceIn(15, 180)).apply()

    var voiceEnabled: Boolean
        get() = preferences.getBoolean(KEY_VOICE_ENABLED, true)
        set(value) = preferences.edit().putBoolean(KEY_VOICE_ENABLED, value).apply()

    var reminderText: String
        get() = preferences.getString(KEY_REMINDER_TEXT, DEFAULT_REMINDER_TEXT) ?: DEFAULT_REMINDER_TEXT
        set(value) = preferences.edit().putString(KEY_REMINDER_TEXT, value.ifBlank { DEFAULT_REMINDER_TEXT }).apply()

    var lastReminderAtMillis: Long
        get() = preferences.getLong(KEY_LAST_REMINDER_AT, 0L)
        set(value) = preferences.edit().putLong(KEY_LAST_REMINDER_AT, value).apply()

    companion object {
        const val DEFAULT_PIN = "1234"
        const val DEFAULT_REMINDER_MINUTES = 30
        const val DEFAULT_REMINDER_TEXT = "爷爷，该休息一下了。请放下手机，看看远处，喝点水，活动活动身体。"

        private const val KEY_PIN = "pin"
        private const val KEY_REMINDER_MINUTES = "reminder_minutes"
        private const val KEY_VOICE_ENABLED = "voice_enabled"
        private const val KEY_REMINDER_TEXT = "reminder_text"
        private const val KEY_LAST_REMINDER_AT = "last_reminder_at"
    }
}
