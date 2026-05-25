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
}
