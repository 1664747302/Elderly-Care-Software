package com.example.elderreminder

data class UsageSessionEvent(
    val timestampMillis: Long,
    val packageName: String,
    val type: Type,
) {
    enum class Type {
        ACTIVITY_RESUMED,
        ACTIVITY_PAUSED,
        ACTIVITY_STOPPED,
    }
}

data class UsageSession(
    val packageName: String?,
    val durationMillis: Long,
    val startTimeMillis: Long = 0L,
) {
    fun exceeds(minutes: Int): Boolean = durationMillis >= minutes * 60_000L
}

class UsageSessionAnalyzer(
    private val ownPackageName: String,
) {
    fun currentSession(
        events: List<UsageSessionEvent>,
        nowMillis: Long,
        ignoredPackages: Set<String> = emptySet(),
        restGracePeriodMinutes: Int = 3
    ): UsageSession {
        var sessionStartTime: Long? = null
        var lastRestStartTime: Long? = null
        var activePackage: String? = null

        val sortedEvents = events.sortedBy { it.timestampMillis }
        
        for (event in sortedEvents) {
            val isIgnored = event.packageName == ownPackageName || ignoredPackages.contains(event.packageName)
            when (event.type) {
                UsageSessionEvent.Type.ACTIVITY_RESUMED -> {
                    if (isIgnored) {
                        if (activePackage != null) {
                            lastRestStartTime = event.timestampMillis
                            activePackage = null
                        }
                    } else {
                        if (lastRestStartTime != null) {
                            val restDuration = event.timestampMillis - lastRestStartTime
                            if (restDuration >= restGracePeriodMinutes * 60_000L) {
                                sessionStartTime = event.timestampMillis
                            } else {
                                if (sessionStartTime != null) {
                                    sessionStartTime += restDuration
                                } else {
                                    sessionStartTime = event.timestampMillis
                                }
                            }
                            lastRestStartTime = null
                        } else {
                            if (sessionStartTime == null) {
                                sessionStartTime = event.timestampMillis
                            }
                        }
                        activePackage = event.packageName
                    }
                }
                UsageSessionEvent.Type.ACTIVITY_PAUSED,
                UsageSessionEvent.Type.ACTIVITY_STOPPED -> {
                    if (activePackage == event.packageName) {
                        lastRestStartTime = event.timestampMillis
                        activePackage = null
                    }
                }
            }
        }

        // If the user is currently resting, we also check if they have successfully rested for the grace period.
        // Wait, if they are currently resting (activePackage is null), but the rest duration *until now* is less than grace period,
        // does that mean the session is technically still active?
        // Note: from the perspective of "currentSessionpackageName", if they are currently resting, the active app is indeed null.
        // But if they resume a normal app again *after* nowMillis, the next analysis will correctly shift the start time.
        // What if they are currently resting, and WorkManager runs?
        // If WorkManager runs and activePackage is null, there is no need to show any reminder right now anyway,
        // because the user is on Launcher or screen is off.
        // So returning null activePackage is totally correct!
        val currentActive = activePackage
        val start = sessionStartTime
        if (currentActive == null || start == null) {
            return UsageSession(packageName = null, durationMillis = 0L, startTimeMillis = 0L)
        }

        return UsageSession(
            packageName = currentActive,
            durationMillis = (nowMillis - start).coerceAtLeast(0L),
            startTimeMillis = start
        )
    }
}
