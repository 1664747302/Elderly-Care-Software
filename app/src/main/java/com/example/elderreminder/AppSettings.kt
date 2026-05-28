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
        set(value) = preferences.edit().putInt(KEY_CURFEW_START_HOUR, (if (value == 24) 0 else value).coerceIn(0, 23)).apply()

    var curfewEndHour: Int
        get() = preferences.getInt(KEY_CURFEW_END_HOUR, 6)
        set(value) = preferences.edit().putInt(KEY_CURFEW_END_HOUR, (if (value == 24) 0 else value).coerceIn(0, 23)).apply()

    // 新增：家人设置的老人宵禁时间的提醒间隔（单位：分钟），限制在 1 到 15 分钟
    var curfewReminderIntervalMinutes: Int
        get() = preferences.getInt(KEY_CURFEW_REMINDER_INTERVAL_MINUTES, DEFAULT_CURFEW_REMINDER_INTERVAL_MINUTES)
        set(value) = preferences.edit().putInt(KEY_CURFEW_REMINDER_INTERVAL_MINUTES, value.coerceIn(1, 15)).apply()

    // 新增：如果老人连续使用提醒后依旧没有停止使用手机，家人设置的重复弹窗和语音提醒间隔（单位：分钟）
    var repeatedReminderIntervalMinutes: Int
        get() = preferences.getInt(KEY_REPEATED_REMINDER_INTERVAL_MINUTES, DEFAULT_REPEATED_REMINDER_INTERVAL_MINUTES)
        set(value) = preferences.edit().putInt(KEY_REPEATED_REMINDER_INTERVAL_MINUTES, value.coerceAtLeast(1)).apply()

    // 新增：防卡bug的休息判定间隔时间阈值（单位：分钟），限制在 1 到 30 分钟
    var restGracePeriodMinutes: Int
        get() = preferences.getInt(KEY_REST_GRACE_PERIOD_MINUTES, DEFAULT_REST_GRACE_PERIOD_MINUTES)
        set(value) = preferences.edit().putInt(KEY_REST_GRACE_PERIOD_MINUTES, value.coerceIn(1, 30)).apply()

    // 新增：DeepSeek API 评价相关设置
    var deepseekApiKey: String
        get() = preferences.getString(KEY_DEEPSEEK_API_KEY, "") ?: ""
        set(value) = preferences.edit().putString(KEY_DEEPSEEK_API_KEY, value.trim()).apply()

    var deepseekApiUrl: String
        get() = preferences.getString(KEY_DEEPSEEK_API_URL, "https://api.deepseek.com/v1") ?: "https://api.deepseek.com/v1"
        set(value) = preferences.edit().putString(KEY_DEEPSEEK_API_URL, value.trim()).apply()

    var deepseekModel: String
        get() = preferences.getString(KEY_DEEPSEEK_MODEL, "deepseek-v4-flash") ?: "deepseek-v4-flash"
        set(value) = preferences.edit().putString(KEY_DEEPSEEK_MODEL, value.trim()).apply()

    var lastAiEvaluationTimeMillis: Long
        get() = preferences.getLong(KEY_LAST_AI_EVALUATION_TIME, 0L)
        set(value) = preferences.edit().putLong(KEY_LAST_AI_EVALUATION_TIME, value).apply()

    var lastAiEvaluationResult: String
        get() = preferences.getString(KEY_LAST_AI_EVALUATION_RESULT, "") ?: ""
        set(value) = preferences.edit().putString(KEY_LAST_AI_EVALUATION_RESULT, value).apply()

    var userAge: String
        get() = preferences.getString(KEY_USER_AGE, "") ?: ""
        set(value) = preferences.edit().putString(KEY_USER_AGE, value.trim()).apply()

    var userGender: String
        get() = preferences.getString(KEY_USER_GENDER, "") ?: ""
        set(value) = preferences.edit().putString(KEY_USER_GENDER, value.trim()).apply()

    var userChronicDiseases: String
        get() = preferences.getString(KEY_USER_CHRONIC_DISEASES, "") ?: ""
        set(value) = preferences.edit().putString(KEY_USER_CHRONIC_DISEASES, value.trim()).apply()

    var bpSystolicMin: Int
        get() = preferences.getInt(KEY_BP_SYSTOLIC_MIN, 90)
        set(value) = preferences.edit().putInt(KEY_BP_SYSTOLIC_MIN, value.coerceIn(40, 260)).apply()

    var bpSystolicMax: Int
        get() = preferences.getInt(KEY_BP_SYSTOLIC_MAX, 139)
        set(value) = preferences.edit().putInt(KEY_BP_SYSTOLIC_MAX, value.coerceIn(40, 260)).apply()

    var bpDiastolicMin: Int
        get() = preferences.getInt(KEY_BP_DIASTOLIC_MIN, 60)
        set(value) = preferences.edit().putInt(KEY_BP_DIASTOLIC_MIN, value.coerceIn(30, 180)).apply()

    var bpDiastolicMax: Int
        get() = preferences.getInt(KEY_BP_DIASTOLIC_MAX, 89)
        set(value) = preferences.edit().putInt(KEY_BP_DIASTOLIC_MAX, value.coerceIn(30, 180)).apply()

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
        const val DEFAULT_CURFEW_REMINDER_INTERVAL_MINUTES = 2
        const val DEFAULT_REST_GRACE_PERIOD_MINUTES = 3

        private const val KEY_PIN = "pin"
        private const val KEY_REMINDER_MINUTES = "reminder_minutes"
        private const val KEY_VOICE_ENABLED = "voice_enabled"
        private const val KEY_REMINDER_TEXT = "reminder_text"
        private const val KEY_LAST_REMINDER_AT = "last_reminder_at"
        private const val KEY_CUSTOM_AUDIO_ENABLED = "custom_audio_enabled"
        private const val KEY_CURFEW_ENABLED = "curfew_enabled"
        private const val KEY_CURFEW_START_HOUR = "curfew_start_hour"
        private const val KEY_CURFEW_END_HOUR = "curfew_end_hour"
        private const val KEY_CURFEW_REMINDER_INTERVAL_MINUTES = "curfew_reminder_interval_minutes"
        private const val KEY_REPEATED_REMINDER_INTERVAL_MINUTES = "repeated_reminder_interval_minutes"
        private const val KEY_REST_GRACE_PERIOD_MINUTES = "rest_grace_period_minutes"
        private const val KEY_DEEPSEEK_API_KEY = "deepseek_api_key"
        private const val KEY_DEEPSEEK_API_URL = "deepseek_api_url"
        private const val KEY_DEEPSEEK_MODEL = "deepseek_model"
        private const val KEY_LAST_AI_EVALUATION_TIME = "last_ai_evaluation_time"
        private const val KEY_LAST_AI_EVALUATION_RESULT = "last_ai_evaluation_result"
        private const val KEY_USER_AGE = "user_age"
        private const val KEY_USER_GENDER = "user_gender"
        private const val KEY_USER_CHRONIC_DISEASES = "user_chronic_diseases"
        private const val KEY_BP_SYSTOLIC_MIN = "bp_systolic_min"
        private const val KEY_BP_SYSTOLIC_MAX = "bp_systolic_max"
        private const val KEY_BP_DIASTOLIC_MIN = "bp_diastolic_min"
        private const val KEY_BP_DIASTOLIC_MAX = "bp_diastolic_max"
    }
}
