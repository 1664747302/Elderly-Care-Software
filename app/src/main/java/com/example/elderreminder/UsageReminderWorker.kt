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
        val isCurfew = settings.isCurfewActive(now)

        // 根据是否为“夜间宵禁”确定提醒阈值和冷静期
        val thresholdMinutes = if (isCurfew) 2 else settings.reminderMinutes
        val thresholdMillis = thresholdMinutes * 60_000L
        
        val cooldownMillis = if (isCurfew) 2 * 60_000L else settings.reminderMinutes * 60_000L
        val cooldownPassed = now - settings.lastReminderAtMillis >= cooldownMillis

        if (cooldownPassed) {
            // 获取更长一段时间的统计，确保不会遗漏检测
            val events = readUsageEvents(now - thresholdMillis - FIFTEEN_MINUTES, now)
            val session = UsageSessionAnalyzer(applicationContext.packageName).currentSession(events, now)
            if (session.exceeds(thresholdMinutes)) {
                val reminderText = if (isCurfew) {
                    "爷爷，现在已到深夜宵禁时间，您已经连续看手机超过两分钟了。为了您的睡眠和身体，请立即闭眼休息。"
                } else {
                    settings.reminderText
                }

                ReminderNotifier(applicationContext).show(reminderText)
                if (settings.voiceEnabled) {
                    SpeechReminder.speak(applicationContext, reminderText, isCurfew)
                }
                settings.lastReminderAtMillis = now
                
                // 写入本地数据库，用于生成习惯周报
                try {
                    val db = ReminderHistoryDbHelper(applicationContext)
                    db.insertReminder(now, session.packageName ?: "未知", thresholdMinutes)
                } catch (e: java.lang.Exception) {
                    e.printStackTrace()
                }
            }
        }

        // 如果宵禁时间生效中，自动连环安排 2 分钟后的下一次单次高频检测
        if (settings.isCurfewActive(System.currentTimeMillis())) {
            enqueueNextCurfewCheck(applicationContext)
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
        private const val CURFEW_WORK_NAME = "elder_usage_reminder_curfew"

        fun enqueueNextCurfewCheck(context: Context) {
            val request = androidx.work.OneTimeWorkRequestBuilder<UsageReminderWorker>()
                .setInitialDelay(2, java.util.concurrent.TimeUnit.MINUTES)
                .build()
            androidx.work.WorkManager.getInstance(context).enqueueUniqueWork(
                CURFEW_WORK_NAME,
                androidx.work.ExistingWorkPolicy.REPLACE,
                request
            )
        }
    }
}
