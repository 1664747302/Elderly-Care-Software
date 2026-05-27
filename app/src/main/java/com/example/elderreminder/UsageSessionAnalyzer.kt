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
    fun currentSession(events: List<UsageSessionEvent>, nowMillis: Long): UsageSession {
        var activePackage: String? = null
        var activeSince: Long? = null

        events.sortedBy { it.timestampMillis }.forEach { event ->
            when (event.type) {
                UsageSessionEvent.Type.ACTIVITY_RESUMED -> {
                    activePackage = event.packageName
                    activeSince = event.timestampMillis
                }

                UsageSessionEvent.Type.ACTIVITY_PAUSED,
                UsageSessionEvent.Type.ACTIVITY_STOPPED -> {
                    if (activePackage == event.packageName) {
                        activePackage = null
                        activeSince = null
                    }
                }
            }
        }

        val packageName = activePackage
        val start = activeSince
        if (packageName == null || start == null || packageName == ownPackageName) {
            return UsageSession(packageName = null, durationMillis = 0L)
        }

        return UsageSession(
            packageName = packageName,
            durationMillis = (nowMillis - start).coerceAtLeast(0L),
            startTimeMillis = start,
        )
    }
}
