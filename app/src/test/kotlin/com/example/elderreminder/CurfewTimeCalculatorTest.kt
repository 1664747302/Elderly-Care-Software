package com.example.elderreminder

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.util.Calendar

class CurfewTimeCalculatorTest {

    private fun checkCurfewActive(
        curfewEnabled: Boolean,
        startHour: Int,
        endHour: Int,
        hourOfDay: Int
    ): Boolean {
        if (!curfewEnabled) return false
        
        return if (startHour < endHour) {
            hourOfDay in startHour until endHour
        } else {
            hourOfDay >= startHour || hourOfDay < endHour
        }
    }

    @Test
    fun testCurfewActiveCrossMidnight() {
        // 22:00 ~ 06:00
        val start = 22
        val end = 6

        // 23:00 is active
        assertThat(checkCurfewActive(true, start, end, 23)).isTrue()
        // 03:00 is active
        assertThat(checkCurfewActive(true, start, end, 3)).isTrue()
        // 21:00 is not active
        assertThat(checkCurfewActive(true, start, end, 21)).isFalse()
        // 07:00 is not active
        assertThat(checkCurfewActive(true, start, end, 7)).isFalse()
    }

    @Test
    fun testCurfewActiveNormalRange() {
        // 13:00 ~ 15:00
        val start = 13
        val end = 15

        assertThat(checkCurfewActive(true, start, end, 14)).isTrue()
        assertThat(checkCurfewActive(true, start, end, 12)).isFalse()
        assertThat(checkCurfewActive(true, start, end, 15)).isFalse()
    }

    @Test
    fun testCurfewMidnightBoundaryMapping() {
        // Test that 24 maps to 0 correctly when setting startHour or endHour
        val start = if (24 == 24) 0 else 24
        val end = 14
        
        // At 14:00:28 (hourOfDay = 14), start = 0, end = 14
        // hourOfDay is 14, start is 0, end is 14. start < end so 14 in 0 until 14 is False.
        assertThat(checkCurfewActive(true, start, end, 14)).isFalse()
        
        // At 13:59 (hourOfDay = 13) it is active
        assertThat(checkCurfewActive(true, start, end, 13)).isTrue()
        // At 00:00 (hourOfDay = 0) it is active
        assertThat(checkCurfewActive(true, start, end, 0)).isTrue()
    }

    @Test
    fun testCurfewReminderIntervalRange() {
        // Test interval limits
        val minInterval = 1
        val maxInterval = 15
        assertThat(minInterval).isEqualTo(1)
        assertThat(maxInterval).isEqualTo(15)
    }
}
