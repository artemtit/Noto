package com.noto.app.core

import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

object DateTimeUtils {
    val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    val timeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    fun formatDateShort(date: LocalDate, today: LocalDate = LocalDate.now(), locale: Locale = Locale.getDefault()): String {
        return when (date) {
            today -> if (locale.language == "ru") "Сегодня" else "Today"
            today.plusDays(1) -> if (locale.language == "ru") "Завтра" else "Tomorrow"
            today.minusDays(1) -> if (locale.language == "ru") "Вчера" else "Yesterday"
            else -> DateTimeFormatter.ofPattern("d MMM", locale).format(date)
        }
    }

    fun formatTime(time: LocalTime): String = timeFormatter.format(time)

    fun nowIn(zone: ZoneId = ZoneId.systemDefault()): ZonedDateTime = ZonedDateTime.now(zone)

    fun triggerMillis(date: LocalDate, time: LocalTime, zone: ZoneId = ZoneId.systemDefault()): Long {
        return ZonedDateTime.of(date, time, zone).toInstant().toEpochMilli()
    }
}
