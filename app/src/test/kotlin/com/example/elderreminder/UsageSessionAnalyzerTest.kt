package com.example.elderreminder

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class UsageSessionAnalyzerTest {
    private val analyzer = UsageSessionAnalyzer(ownPackageName = "com.example.elderreminder")

    @Test
    fun activeDurationIgnoresShortUse() {
        val now = 1_800_000L
        val events = listOf(
            UsageSessionEvent(600_000L, "com.social.video", UsageSessionEvent.Type.ACTIVITY_RESUMED),
            UsageSessionEvent(1_700_000L, "com.social.video", UsageSessionEvent.Type.ACTIVITY_PAUSED),
            UsageSessionEvent(1_720_000L, "com.chat", UsageSessionEvent.Type.ACTIVITY_RESUMED),
        )

        // Since com.social.video was used, paused, and after 20 seconds com.chat resumed,
        // the rest time is 20,000L (less than 3 minutes grace period).
        // Under the new logic:
        // At 600,000: session starts at 600,000, active = com.social.video
        // At 1,700,000: active = null, lastRestStartTime = 1,700,000
        // At 1,720,000: com.chat resumes. Rest of 20,000L is < 3 mins (180,000L).
        // Rest duration 20,000L is appended to session start time: sessionStartTime = 600,000 + 20,000 = 620,000.
        // Active = com.chat.
        // At 1,800,000: duration = 1,800,000 - 620,000 = 1,180,000L.
        val result = analyzer.currentSession(events, now)

        assertThat(result.packageName).isEqualTo("com.chat")
        assertThat(result.durationMillis).isEqualTo(1180000L)
    }

    @Test
    fun activeDurationCountsCurrentForegroundApp() {
        val now = 1_900_000L
        val events = listOf(
            UsageSessionEvent(50_000L, "com.browser", UsageSessionEvent.Type.ACTIVITY_RESUMED),
            UsageSessionEvent(90_000L, "com.browser", UsageSessionEvent.Type.ACTIVITY_PAUSED),
            // Rest from 90,000 to 100,000 is 10,000L (less than 3 mins)
            // Session starts at 50,000
            // At 100,000, rest is 10,000L < 3 mins, so session start becomes 50,000 + 10,000 = 60,000.
            UsageSessionEvent(100_000L, "com.shortvideo", UsageSessionEvent.Type.ACTIVITY_RESUMED),
        )

        val result = analyzer.currentSession(events, now)

        assertThat(result.packageName).isEqualTo("com.shortvideo")
        assertThat(result.durationMillis).isEqualTo(1_840_000L)
        assertThat(result.startTimeMillis).isEqualTo(60_000L)
    }

    @Test
    fun activeDurationIgnoresThisApp() {
        val now = 2_000_000L
        val events = listOf(
            UsageSessionEvent(100_000L, "com.example.elderreminder", UsageSessionEvent.Type.ACTIVITY_RESUMED),
        )

        val result = analyzer.currentSession(events, now)

        assertThat(result.packageName).isNull()
        assertThat(result.durationMillis).isEqualTo(0L)
        assertThat(result.exceeds(30)).isFalse()
    }

    @Test
    fun pausedForegroundAppEndsSession() {
        val now = 2_000_000L
        val events = listOf(
            UsageSessionEvent(100_000L, "com.news", UsageSessionEvent.Type.ACTIVITY_RESUMED),
            UsageSessionEvent(1_000_000L, "com.news", UsageSessionEvent.Type.ACTIVITY_PAUSED),
        )

        val result = analyzer.currentSession(events, now)

        assertThat(result.packageName).isNull()
        assertThat(result.durationMillis).isEqualTo(0L)
    }

    @Test
    fun restForMoreThanGracePeriodResetsSession() {
        val now = 2_000_000L
        val events = listOf(
            UsageSessionEvent(100_000L, "com.social", UsageSessionEvent.Type.ACTIVITY_RESUMED),
            UsageSessionEvent(500_000L, "com.social", UsageSessionEvent.Type.ACTIVITY_PAUSED),
            // Rested from 500,000 to 1,000,000 (500,000 ms = ~8.3 minutes, > 3 minutes grace period)
            UsageSessionEvent(1_000_000L, "com.game", UsageSessionEvent.Type.ACTIVITY_RESUMED),
        )

        val result = analyzer.currentSession(events, now, restGracePeriodMinutes = 3)

        assertThat(result.packageName).isEqualTo("com.game")
        // Since the rest was longer than 3 minutes, the session is reset and starts at 1,000,000.
        assertThat(result.startTimeMillis).isEqualTo(1_000_000L)
        assertThat(result.durationMillis).isEqualTo(1_000_000L)
    }
}
