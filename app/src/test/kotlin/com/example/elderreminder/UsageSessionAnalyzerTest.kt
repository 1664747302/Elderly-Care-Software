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

        val result = analyzer.currentSession(events, now)

        assertThat(result.packageName).isEqualTo("com.chat")
        assertThat(result.durationMillis).isEqualTo(80_000L)
        assertThat(result.exceeds(30)).isFalse()
    }

    @Test
    fun activeDurationCountsCurrentForegroundApp() {
        val now = 1_900_000L
        val events = listOf(
            UsageSessionEvent(50_000L, "com.browser", UsageSessionEvent.Type.ACTIVITY_RESUMED),
            UsageSessionEvent(90_000L, "com.browser", UsageSessionEvent.Type.ACTIVITY_PAUSED),
            UsageSessionEvent(100_000L, "com.shortvideo", UsageSessionEvent.Type.ACTIVITY_RESUMED),
        )

        val result = analyzer.currentSession(events, now)

        assertThat(result.packageName).isEqualTo("com.shortvideo")
        assertThat(result.durationMillis).isEqualTo(1_800_000L)
        assertThat(result.startTimeMillis).isEqualTo(100_000L)
        assertThat(result.exceeds(30)).isTrue()
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
}
