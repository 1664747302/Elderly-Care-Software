package com.example.elderreminder

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class UsageReminderWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        if (!UsagePermission.hasUsageAccess(applicationContext)) {
            return Result.success()
        }

        val settings = AppSettings(applicationContext)
        val now = System.currentTimeMillis()
        val reminderMillis = settings.reminderMinutes * 60_000L
        val cooldownPassed = now - settings.lastReminderAtMillis >= reminderMillis
        if (!cooldownPassed) {
            return Result.success()
        }

        val events = readUsageEvents(now - reminderMillis - FIFTEEN_MINUTES, now)
        val session = UsageSessionAnalyzer(applicationContext.packageName).currentSession(events, now)
        if (session.exceeds(settings.reminderMinutes)) {
            ReminderNotifier(applicationContext).show(settings.reminderText)
            if (settings.voiceEnabled) {
                SpeechReminder.speak(applicationContext, settings.reminderText)
            }
            settings.lastReminderAtMillis = now
            
            // 写入本地数据库，用于生成习惯周报
            try {
                val db = ReminderHistoryDbHelper(applicationContext)
                db.insertReminder(now, session.packageName ?: "未知", settings.reminderMinutes)
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
            }
        }

        return Result.success()
    }

    private fun readUsageEvents(startMillis: Long, endMillis: Long): List<UsageSessionEvent> {
        val manager = applicationContext.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val usageEvents = manager.queryEvents(startMillis, endMillis)
        val event = UsageEvents.Event()
        val result = mutableListOf<UsageSessionEvent>()

        while (usageEvents.hasNextEvent()) {
            usageEvents.getNextEvent(event)
            val type = when (event.eventType) {
                UsageEvents.Event.ACTIVITY_RESUMED -> UsageSessionEvent.Type.ACTIVITY_RESUMED
                UsageEvents.Event.ACTIVITY_PAUSED -> UsageSessionEvent.Type.ACTIVITY_PAUSED
                UsageEvents.Event.ACTIVITY_STOPPED -> UsageSessionEvent.Type.ACTIVITY_STOPPED
                else -> null
            }
            if (type != null && event.packageName != null) {
                result += UsageSessionEvent(event.timeStamp, event.packageName, type)
            }
        }

        return result
    }

    companion object {
        private const val FIFTEEN_MINUTES = 15 * 60_000L
    }
}
