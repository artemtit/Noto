package com.noto.app

import com.noto.app.core.DateTimeUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.util.Locale

class DateTimeUtilsTest {

    @Test fun `today, tomorrow, yesterday formatting`() {
        val today = LocalDate.of(2026, 8, 20)
        assertEquals("Today", DateTimeUtils.formatDateShort(today, today, Locale.ENGLISH))
        assertEquals("Tomorrow", DateTimeUtils.formatDateShort(today.plusDays(1), today, Locale.ENGLISH))
        assertEquals("Yesterday", DateTimeUtils.formatDateShort(today.minusDays(1), today, Locale.ENGLISH))
    }

    @Test fun `triggerMillis is in the future for future date`() {
        val date = LocalDate.now().plusDays(1)
        val time = LocalTime.of(9, 0)
        val ms = DateTimeUtils.triggerMillis(date, time, ZoneId.systemDefault())
        assertTrue(ms > System.currentTimeMillis())
    }
}
