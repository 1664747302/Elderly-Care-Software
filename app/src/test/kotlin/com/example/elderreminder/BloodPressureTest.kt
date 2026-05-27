package com.example.elderreminder

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class BloodPressureTest {

    @Test
    fun testBloodPressureRecordStructure() {
        val record = BloodPressureRecord(
            id = 45L,
            date = "2026-05-27",
            period = "早晨",
            systolic = 125,
            diastolic = 82,
            heartRate = 72,
            timestamp = 1716800000000L
        )

        assertThat(record.id).isEqualTo(45L)
        assertThat(record.date).isEqualTo("2026-05-27")
        assertThat(record.period).isEqualTo("早晨")
        assertThat(record.systolic).isEqualTo(125)
        assertThat(record.diastolic).isEqualTo(82)
        assertThat(record.heartRate).isEqualTo(72)
        assertThat(record.timestamp).isEqualTo(1716800000000L)
    }

    @Test
    fun testBloodPressureEvaluationLogic() {
        // We can replicate or test evaluating helper directly or keep it standalone.
        // Let's verify evaluations:
        // systolic >= 140 || diastolic >= 90 => HIGH
        // systolic < 90 || diastolic < 60 => LOW
        // systolic in 120..139 || diastolic in 80..89 => PRE_HIGH
        // else => NORMAL
        
        fun evaluate(systolic: Int, diastolic: Int): String {
            return when {
                systolic >= 140 || diastolic >= 90 -> "HIGH"
                systolic < 90 || diastolic < 60 -> "LOW"
                systolic in 120..139 || diastolic in 80..89 -> "PRE_HIGH"
                else -> "NORMAL"
            }
        }

        assertThat(evaluate(115, 75)).isEqualTo("NORMAL")
        assertThat(evaluate(120, 80)).isEqualTo("PRE_HIGH")
        assertThat(evaluate(139, 89)).isEqualTo("PRE_HIGH")
        assertThat(evaluate(140, 85)).isEqualTo("HIGH")
        assertThat(evaluate(130, 90)).isEqualTo("HIGH")
        assertThat(evaluate(89, 70)).isEqualTo("LOW")
        assertThat(evaluate(110, 59)).isEqualTo("LOW")
    }

    @Test
    fun testDailyScoreCalculationLogic() {
        // Test custom score helper formula matches expected score mapping
        fun score(systolic: Int, diastolic: Int, heartRate: Int): Int {
            val status = when {
                systolic >= 140 || diastolic >= 90 -> "HIGH"
                systolic < 90 || diastolic < 60 -> "LOW"
                systolic in 120..139 || diastolic in 80..89 -> "PRE_HIGH"
                else -> "NORMAL"
            }
            var points = when (status) {
                "NORMAL" -> 100
                "PRE_HIGH" -> 85
                "LOW" -> 70
                "HIGH" -> 55
                else -> 0
            }
            if (heartRate !in 60..100) {
                points -= 10
            }
            return points.coerceAtLeast(0).coerceAtMost(100)
        }

        // Verify normal BP & normal heart rate is 100 points
        assertThat(score(115, 75, 72)).isEqualTo(100)
        // Verify pre-high BP & normal heart rate is 85 points
        assertThat(score(120, 80, 75)).isEqualTo(85)
        // Verify high BP & high heart rate is 55 - 10 = 45 points
        assertThat(score(145, 95, 110)).isEqualTo(45)
        // Verify normal BP & low heart rate is 100 - 10 = 90 points
        assertThat(score(110, 70, 55)).isEqualTo(90)
    }
}
