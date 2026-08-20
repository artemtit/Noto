package com.noto.app.ai

import com.noto.app.core.AppResult
import com.noto.app.data.prefs.SettingsRepository
import com.noto.app.domain.model.ExistingTaskRef
import com.noto.app.domain.model.ParsedTask
import com.noto.app.domain.model.TaskAction
import java.time.LocalTime
import java.time.ZonedDateTime
import java.util.Locale

data class BusySlot(val start: LocalTime, val durationMinutes: Int)

data class ParseResult(
    val tasks: List<ParsedTask> = emptyList(),
    val actions: List<TaskAction> = emptyList(),
)

interface TaskParser {
    suspend fun parse(
        transcript: String,
        now: ZonedDateTime,
        locale: Locale,
        knownProjects: List<String>,
        busyToday: List<BusySlot> = emptyList(),
        rhythm: SettingsRepository.RhythmProfile = SettingsRepository.RhythmProfile.DEFAULT,
        existingTasks: List<ExistingTaskRef> = emptyList(),
    ): AppResult<ParseResult>
}
