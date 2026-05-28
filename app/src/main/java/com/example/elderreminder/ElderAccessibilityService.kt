package com.example.elderreminder

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent

class ElderAccessibilityService : AccessibilityService() {

    private var currentApp: String? = null
    
    // We maintain a list of past usage transition events to pass to raw analyzer
    private val rawEvents = ArrayDeque<UsageSessionEvent>(60)
    
    private val handler = Handler(Looper.getMainLooper())
    private var checkRunnable: Runnable? = null

    private val launcherPackages = mutableSetOf<String>()

    private val screenOffReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_SCREEN_OFF) {
                stopTimer()
                currentApp = null
                rawEvents.clear()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        updateLauncherPackages()
        val filter = IntentFilter(Intent.ACTION_SCREEN_OFF)
        registerReceiver(screenOffReceiver, filter)
    }

    private fun updateLauncherPackages() {
        launcherPackages.clear()
        launcherPackages.add(packageName) // self
        launcherPackages.add("com.android.systemui") // system ui
        try {
            val intent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
            }
            val list = packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
            for (info in list) {
                info.activityInfo?.packageName?.let {
                    launcherPackages.add(it)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val pkgName = event.packageName?.toString() ?: return
            handleAppChange(pkgName)
        }
    }

    private fun handleAppChange(newPkg: String) {
        if (currentApp == newPkg) return

        val now = System.currentTimeMillis()
        val isLauncherOrSelf = launcherPackages.contains(newPkg)
        val wasLauncherOrSelf = launcherPackages.contains(currentApp ?: "")

        // Record the event
        if (currentApp != null) {
            rawEvents.add(UsageSessionEvent(now, currentApp!!, UsageSessionEvent.Type.ACTIVITY_PAUSED))
        }
        rawEvents.add(UsageSessionEvent(now, newPkg, UsageSessionEvent.Type.ACTIVITY_RESUMED))
        currentApp = newPkg

        // Clean up history to keep memory footprint bounded (keep only last 60 events)
        while (rawEvents.size > 60) {
            rawEvents.removeFirst()
        }

        if (isLauncherOrSelf) {
            stopTimer()
        } else {
            if (wasLauncherOrSelf) {
                startTimer()
            } else {
                // Check usage limit immediately when switching app (so we don't wait for timer tick)
                checkUsageLimit()
            }
        }
    }

    private fun startTimer() {
        stopTimer()
        val runnable = object : Runnable {
            override fun run() {
                checkUsageLimit()
                handler.postDelayed(this, 30_000L) // check every 30 seconds
            }
        }
        checkRunnable = runnable
        handler.post(runnable)
    }

    private fun stopTimer() {
        checkRunnable?.let { handler.removeCallbacks(it) }
        checkRunnable = null
    }

    private fun checkUsageLimit() {
        val now = System.currentTimeMillis()
        if (currentApp == null || launcherPackages.contains(currentApp!!)) return

        val settings = AppSettings(applicationContext)
        val isCurfew = settings.isCurfewActive(now)
        val thresholdMinutes = if (isCurfew) settings.curfewReminderIntervalMinutes else settings.reminderMinutes
        val restGraceMinutes = settings.restGracePeriodMinutes

        // Analyze using the updated robust analyzer that implements restGracePeriod minutes!
        val analyzer = UsageSessionAnalyzer(packageName)
        val session = analyzer.currentSession(rawEvents, now, ignoredPackages = launcherPackages, restGracePeriodMinutes = restGraceMinutes)

        if (session.packageName == null || session.durationMillis <= 0L) {
            return
        }

        if (session.exceeds(thresholdMinutes)) {
            val cooldownMillis = if (isCurfew) {
                settings.curfewReminderIntervalMinutes * 60_000L
            } else if (settings.lastReminderAtMillis >= session.startTimeMillis) {
                // already reminded in this session
                settings.repeatedReminderIntervalMinutes * 60_000L
            } else {
                settings.reminderMinutes * 60_000L
            }

            val cooldownPassed = now - settings.lastReminderAtMillis >= cooldownMillis

            if (cooldownPassed) {
                triggerReminder(now, thresholdMinutes, isCurfew)
            }
        }
    }

    private fun triggerReminder(now: Long, thresholdMinutes: Int, isCurfew: Boolean) {
        val settings = AppSettings(applicationContext)
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

        try {
            val db = ReminderHistoryDbHelper(applicationContext)
            db.insertReminder(now, currentApp ?: "未知", thresholdMinutes)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onInterrupt() {
        stopTimer()
    }

    override fun onDestroy() {
        stopTimer()
        try {
            unregisterReceiver(screenOffReceiver)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        super.onDestroy()
    }
}
