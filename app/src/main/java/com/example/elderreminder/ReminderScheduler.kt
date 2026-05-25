package com.example.elderreminder

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object ReminderScheduler {
    private const val WORK_NAME = "elder_usage_reminder"

    fun schedule(context: Context) {
        val request = PeriodicWorkRequestBuilder<UsageReminderWorker>(15, TimeUnit.MINUTES).build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )

        // 如果当前正好在在宵禁时间域内，立即启动一次单次轮询链条
        val settings = AppSettings(context)
        if (settings.isCurfewActive(System.currentTimeMillis())) {
            val curfewRequest = androidx.work.OneTimeWorkRequestBuilder<UsageReminderWorker>().build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                "elder_usage_reminder_curfew",
                androidx.work.ExistingWorkPolicy.REPLACE,
                curfewRequest
            )
        }
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }
}
