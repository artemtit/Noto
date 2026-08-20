package com.noto.app.domain.model

import java.time.LocalDate

enum class Recurrence {
    NONE, DAILY, WEEKLY, MONTHLY;

    fun next(date: LocalDate): LocalDate? = when (this) {
        NONE -> null
        DAILY -> date.plusDays(1)
        WEEKLY -> date.plusWeeks(1)
        MONTHLY -> date.plusMonths(1)
    }

    companion object {
        fun fromString(v: String?): Recurrence = when (v?.lowercase()) {
            "daily" -> DAILY
            "weekly" -> WEEKLY
            "monthly" -> MONTHLY
            else -> NONE
        }
    }
}
