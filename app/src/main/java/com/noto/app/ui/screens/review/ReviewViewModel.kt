package com.noto.app.ui.screens.review

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.noto.app.ai.BusySlot
import com.noto.app.ai.SlotSuggester
import com.noto.app.ai.TaskParser
import com.noto.app.calendar.CalendarSyncService
import com.noto.app.core.AppError
import com.noto.app.core.AppResult
import com.noto.app.data.prefs.SettingsRepository
import com.noto.app.data.repo.ProjectRepository
import com.noto.app.data.repo.TaskRepository
import com.noto.app.domain.model.ParsedTask
import com.noto.app.domain.model.Priority
import com.noto.app.domain.model.Project
import com.noto.app.domain.model.Recurrence
import com.noto.app.domain.model.Task
import com.noto.app.notifications.NotoNotificationScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Locale

data class ReviewItem(
    val id: String,
    val title: String,
    val description: String?,
    val startDate: LocalDate?,
    val dueDate: LocalDate?,
    val dueTime: LocalTime?,
    val estimatedMinutes: Int?,
    val suggestedSlots: List<LocalTime> = emptyList(),
    val priority: Priority,
    val projectId: Long?,
    val reminder: Boolean,
    val recurrence: Recurrence = Recurrence.NONE,
)

data class ReviewUiState(
    val loading: Boolean = false,
    val transcript: String = "",
    val items: List<ReviewItem> = emptyList(),
    val projects: List<Project> = emptyList(),
    val error: AppError? = null,
    val saved: Boolean = false,
)

class ReviewViewModel(
    private val parser: TaskParser,
    private val projects: ProjectRepository,
    private val tasks: TaskRepository,
    private val scheduler: NotoNotificationScheduler,
    private val calendarSync: CalendarSyncService,
    private val settings: SettingsRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ReviewUiState())
    val state: StateFlow<ReviewUiState> = _state.asStateFlow()

    fun start(transcript: String) {
        if (_state.value.transcript == transcript && (_state.value.items.isNotEmpty() || _state.value.error != null)) return
        _state.value = ReviewUiState(loading = true, transcript = transcript)
        viewModelScope.launch {
            val projs = projects.getAll()
            val now = ZonedDateTime.now(ZoneId.systemDefault())
            val today = now.toLocalDate()
            val todayTasks = tasks.tasksOnDate(today.toString())
            val busy = todayTasks.mapNotNull { t ->
                t.dueTime?.let { BusySlot(it, t.effectiveDurationMinutes) }
            }
            val rhythm = settings.currentRhythm()
            val result = parser.parse(
                transcript = transcript,
                now = now,
                locale = Locale.getDefault(),
                knownProjects = projs.map { it.name },
                busyToday = busy,
                rhythm = rhythm,
            )
            when (result) {
                is AppResult.Ok -> {
                    val wantsPool = SlotSuggester.wantsTimePool(transcript)
                    val items = result.value.mapIndexed { i, p ->
                        val base = p.toReviewItem(i, projs)
                        // Local fallback: if user asked for time pool but AI didn't return any,
                        // and dueTime is empty, synthesize suggestions.
                        if (base.suggestedSlots.isEmpty() && base.dueTime == null && wantsPool) {
                            val date = base.dueDate ?: today
                            val existing = if (date == today) todayTasks
                            else tasks.tasksOnDate(date.toString())
                            val slots = SlotSuggester.suggest(
                                date = date,
                                now = now.toLocalTime(),
                                today = today,
                                existing = existing,
                                rhythm = rhythm,
                                durationMinutes = base.estimatedMinutes ?: 30,
                            )
                            base.copy(dueDate = date, suggestedSlots = slots)
                        } else base
                    }
                    _state.value = _state.value.copy(loading = false, items = items, projects = projs)
                }
                is AppResult.Err -> _state.value = _state.value.copy(loading = false, error = result.error, projects = projs)
            }
        }
    }

    fun updateItem(id: String, transform: (ReviewItem) -> ReviewItem) {
        _state.value = _state.value.copy(items = _state.value.items.map { if (it.id == id) transform(it) else it })
    }

    fun pickSlot(id: String, slot: LocalTime) = updateItem(id) {
        it.copy(dueTime = slot, suggestedSlots = emptyList())
    }

    fun remove(id: String) {
        _state.value = _state.value.copy(items = _state.value.items.filterNot { it.id == id })
    }

    fun confirm() {
        val items = _state.value.items
        if (items.isEmpty()) return
        viewModelScope.launch {
            items.forEach { r ->
                val task = Task(
                    title = r.title,
                    description = r.description,
                    startDate = r.startDate,
                    dueDate = r.dueDate,
                    dueTime = r.dueTime,
                    estimatedMinutes = r.estimatedMinutes,
                    priority = r.priority,
                    projectId = r.projectId,
                    reminderEnabled = r.reminder,
                    recurrence = r.recurrence,
                )
                val id = tasks.insert(task)
                val saved = task.copy(id = id)
                val notifId = scheduler.schedule(saved)
                val syncEnabled = settings.isCalendarSyncEnabled()
                val eventId = if (syncEnabled && calendarSync.hasPermission()) calendarSync.insert(saved) else null
                if (notifId != null || eventId != null) {
                    tasks.update(saved.copy(reminderId = notifId, calendarEventId = eventId))
                }
            }
            _state.value = _state.value.copy(saved = true)
        }
    }
}

private fun ParsedTask.toReviewItem(index: Int, projects: List<Project>): ReviewItem {
    val projId = projectName?.let { name ->
        projects.firstOrNull { it.name.equals(name, ignoreCase = true) }?.id
    }
    return ReviewItem(
        id = "$index",
        title = title,
        description = description,
        startDate = startDate,
        dueDate = dueDate,
        dueTime = dueTime,
        estimatedMinutes = estimatedMinutes,
        suggestedSlots = suggestedSlots,
        priority = priority,
        projectId = projId,
        reminder = reminder,
    )
}
