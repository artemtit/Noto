package com.noto.app.ai

import com.noto.app.core.AppResult
import com.noto.app.data.prefs.SettingsRepository
import com.noto.app.domain.model.ParsedTask
import java.time.LocalTime
import java.time.ZonedDateTime
import java.util.Locale

data class BusySlot(val start: LocalTime, val durationMinutes: Int)

interface TaskParser {
    suspend fun parse(
        transcript: String,
        now: ZonedDateTime,
        locale: Locale,
        knownProjects: List<String>,
        busyToday: List<BusySlot> = emptyList(),
        rhythm: SettingsRepository.RhythmProfile = SettingsRepository.RhythmProfile.DEFAULT,
    ): AppResult<List<ParsedTask>>
}
