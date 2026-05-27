package com.example.elderreminder

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
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

        // 根据是否为“夜间宵禁”确定提醒阈值和最长查询区间
        val thresholdMinutes = if (isCurfew) settings.curfewReminderIntervalMinutes else settings.reminderMinutes
        val thresholdMillis = thresholdMinutes * 60_000L

        // 获取比检测区间更长一段时间的统计，确保能追溯到当前前台运行会话的起始点
        val maxQueryRangeMinutes = maxOf(settings.reminderMinutes, 180)
        val events = readUsageEvents(now - maxQueryRangeMinutes * 60_000L - FIFTEEN_MINUTES, now)
        val session = UsageSessionAnalyzer(applicationContext.packageName).currentSession(events, now)

        if (session.packageName == null || session.durationMillis <= 0L) {
            // 如果宵禁时间生效中，自动连环安排 2 分钟后的下一次单次高频检测
            if (settings.isCurfewActive(System.currentTimeMillis())) {
                enqueueNextCurfewCheck(applicationContext)
            }
            return Result.success()
        }

        if (session.exceeds(thresholdMinutes)) {
            // 判定是否已经在当前的连续使用会话中提醒过
            val alreadyRemindedInSession = settings.lastReminderAtMillis >= session.startTimeMillis

            val cooldownMillis = if (isCurfew) {
                settings.curfewReminderIntervalMinutes * 60_000L
            } else if (alreadyRemindedInSession) {
                // 如果已在该会话提醒过且未停止使用手机，则按照家人设置的重复提醒间隔计算冷却时间
                settings.repeatedReminderIntervalMinutes * 60_000L
            } else {
                settings.reminderMinutes * 60_000L
            }

            val cooldownPassed = now - settings.lastReminderAtMillis >= cooldownMillis

            if (cooldownPassed) {
                val reminderText = if (isCurfew) {
                    "家人，现在已到深夜宵禁时间，您已经连续看手机超过${thresholdMinutes}分钟了。为了您的睡眠和身体，请立即闭眼休息。"
                } else {
                    settings.reminderText
                }

                // Show Notification
                ReminderNotifier(applicationContext).show(reminderText)
                
                // Show Full Screen Alert Dialog Activity
                try {
                    val intent = Intent(applicationContext, ReminderDialogActivity::class.java).apply {
                        putExtra("reminder_text", reminderText)
                        putExtra("is_curfew", isCurfew)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    }
                    applicationContext.startActivity(intent)
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                // SpeechReminder speak will also be triggered inside ReminderDialogActivity, 
                // fallback in case activity launch fails or doesn't play
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
            val settings = AppSettings(context)
            val intervalMinutes = settings.curfewReminderIntervalMinutes
            val request = androidx.work.OneTimeWorkRequestBuilder<UsageReminderWorker>()
                .setInitialDelay(intervalMinutes.toLong(), java.util.concurrent.TimeUnit.MINUTES)
                .build()
            androidx.work.WorkManager.getInstance(context).enqueueUniqueWork(
                CURFEW_WORK_NAME,
                androidx.work.ExistingWorkPolicy.REPLACE,
                request
            )
        }
    }
}
