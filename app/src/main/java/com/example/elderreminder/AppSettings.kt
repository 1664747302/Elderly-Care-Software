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

    var customAudioEnabled: Boolean
        get() = preferences.getBoolean(KEY_CUSTOM_AUDIO_ENABLED, false)
        set(value) = preferences.edit().putBoolean(KEY_CUSTOM_AUDIO_ENABLED, value).apply()

    var curfewEnabled: Boolean
        get() = preferences.getBoolean(KEY_CURFEW_ENABLED, false)
        set(value) = preferences.edit().putBoolean(KEY_CURFEW_ENABLED, value).apply()

    var curfewStartHour: Int
        get() = preferences.getInt(KEY_CURFEW_START_HOUR, 22)
        set(value) = preferences.edit().putInt(KEY_CURFEW_START_HOUR, value.coerceIn(0, 23)).apply()

    var curfewEndHour: Int
        get() = preferences.getInt(KEY_CURFEW_END_HOUR, 6)
        set(value) = preferences.edit().putInt(KEY_CURFEW_END_HOUR, value.coerceIn(0, 23)).apply()

    // 新增：如果老人连续使用提醒后依旧没有停止使用手机，家人设置的重复弹窗和语音提醒间隔（单位：分钟）
    var repeatedReminderIntervalMinutes: Int
        get() = preferences.getInt(KEY_REPEATED_REMINDER_INTERVAL_MINUTES, DEFAULT_REPEATED_REMINDER_INTERVAL_MINUTES)
        set(value) = preferences.edit().putInt(KEY_REPEATED_REMINDER_INTERVAL_MINUTES, value.coerceAtLeast(1)).apply()

    fun isCurfewActive(nowMillis: Long): Boolean {
        if (!curfewEnabled) return false
        val cal = java.util.Calendar.getInstance().apply { timeInMillis = nowMillis }
        val hour = cal.get(java.util.Calendar.HOUR_OF_DAY)
        
        val start = curfewStartHour
        val end = curfewEndHour
        
        return if (start < end) {
            hour in start until end
        } else {
            hour >= start || hour < end
        }
    }

    companion object {
        const val DEFAULT_PIN = "1234"
        const val DEFAULT_REMINDER_MINUTES = 30
        const val DEFAULT_REMINDER_TEXT = "家人，该休息一下了。请放下手机，看看远处，喝点水，活动活动身体。"
        const val DEFAULT_REPEATED_REMINDER_INTERVAL_MINUTES = 5

        private const val KEY_PIN = "pin"
        private const val KEY_REMINDER_MINUTES = "reminder_minutes"
        private const val KEY_VOICE_ENABLED = "voice_enabled"
        private const val KEY_REMINDER_TEXT = "reminder_text"
        private const val KEY_LAST_REMINDER_AT = "last_reminder_at"
        private const val KEY_CUSTOM_AUDIO_ENABLED = "custom_audio_enabled"
        private const val KEY_CURFEW_ENABLED = "curfew_enabled"
        private const val KEY_CURFEW_START_HOUR = "curfew_start_hour"
        private const val KEY_CURFEW_END_HOUR = "curfew_end_hour"
        private const val KEY_REPEATED_REMINDER_INTERVAL_MINUTES = "repeated_reminder_interval_minutes"
    }
}
