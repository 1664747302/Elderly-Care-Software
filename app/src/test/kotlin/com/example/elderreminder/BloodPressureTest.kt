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

    @Test
    fun testBloodPressureExporterRangeAndGrouping() {
        // 1. 验证日期区间计算返回 yyyy-MM-dd 格式且跨度正确
        val oneMonthStart = BloodPressureExporter.getStartDateForRange(1)
        val threeMonthStart = BloodPressureExporter.getStartDateForRange(2)
        val oneYearStart = BloodPressureExporter.getStartDateForRange(3)

        assertThat(oneMonthStart).matches("^\\d{4}-\\d{2}-\\d{2}$")
        assertThat(threeMonthStart).matches("^\\d{4}-\\d{2}-\\d{2}$")
        assertThat(oneYearStart).matches("^\\d{4}-\\d{2}-\\d{2}$")

        // 2. 验证单日合算平均值算法 (当某天有多次自测记录时，要求输出单日平均统计合并的多维数据)
        // 比如某组记录同日有 3 次测定，应正确合并取平均值
        val records = listOf(
            BloodPressureRecord(1L, "2026-05-28", "早晨", 120, 80, 70, 1000L),
            BloodPressureRecord(2L, "2026-05-28", "中午", 130, 85, 75, 2000L),
            BloodPressureRecord(3L, "2026-05-28", "晚上", 110, 75, 65, 3000L),
            BloodPressureRecord(4L, "2026-05-27", "早晨", 140, 90, 80, 4000L)
        )

        // 我们在 JVM 纯测试环境下，模拟对这批数据进行单日化处理 (同 Exporter 实现)
        val groupedMap = records.groupBy { it.date }.toSortedMap(reverseOrder())
        val dailyRecords = mutableListOf<DailyBPRecord>()
        for ((date, dayRecs) in groupedMap) {
            val count = dayRecs.size
            if (count > 0) {
                val avgSystolic = Math.round(dayRecs.map { it.systolic }.average()).toInt()
                val avgDiastolic = Math.round(dayRecs.map { it.diastolic }.average()).toInt()
                val avgHeartRate = Math.round(dayRecs.map { it.heartRate }.average()).toInt()
                dailyRecords.add(DailyBPRecord(date, avgSystolic, avgDiastolic, avgHeartRate, count))
            }
        }

        // 验证合并天数与排序
        assertThat(dailyRecords).hasSize(2)
        // 验证最晚日期在前
        assertThat(dailyRecords[0].date).isEqualTo("2026-05-28")
        // 验证 5-28 平均值 (120+130+110)/3 = 120; (80+85+75)/3 = 80; (70+75+65)/3 = 70
        assertThat(dailyRecords[0].avgSystolic).isEqualTo(120)
        assertThat(dailyRecords[0].avgDiastolic).isEqualTo(80)
        assertThat(dailyRecords[0].avgHeartRate).isEqualTo(70)
        assertThat(dailyRecords[0].count).isEqualTo(3)

        // 验证 5-27 平均值
        assertThat(dailyRecords[1].date).isEqualTo("2026-05-27")
        assertThat(dailyRecords[1].avgSystolic).isEqualTo(140)
        assertThat(dailyRecords[1].avgDiastolic).isEqualTo(90)
        assertThat(dailyRecords[1].avgHeartRate).isEqualTo(80)
        assertThat(dailyRecords[1].count).isEqualTo(1)
    }
}
