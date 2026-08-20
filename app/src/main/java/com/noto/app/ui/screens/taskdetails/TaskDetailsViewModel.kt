package com.noto.app.ui.screens.taskdetails

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.noto.app.ai.SlotSuggester
import com.noto.app.calendar.CalendarSyncService
import com.noto.app.data.prefs.SettingsRepository
import com.noto.app.data.repo.ProjectRepository
import com.noto.app.data.repo.TaskRepository
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

data class ConflictPrompt(
    val other: Task,
    val alternatives: List<LocalTime>,
)

data class TaskDetailsState(
    val loading: Boolean = true,
    val task: Task? = null,
    val projects: List<Project> = emptyList(),
    val finished: Boolean = false,
    val conflict: ConflictPrompt? = null,
    val rescheduleOptions: List<LocalTime> = emptyList(),
    val showReschedule: Boolean = false,
)

class TaskDetailsViewModel(
    private val tasks: TaskRepository,
    private val projects: ProjectRepository,
    private val scheduler: NotoNotificationScheduler,
    private val calendarSync: CalendarSyncService,
    private val settings: SettingsRepository,
    private val taskId: Long,
) : ViewModel() {

    private val _state = MutableStateFlow(TaskDetailsState())
    val state: StateFlow<TaskDetailsState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val task = if (taskId > 0) tasks.getById(taskId) else Task(title = "")
            val projs = projects.getAll()
            _state.value = TaskDetailsState(loading = false, task = task, projects = projs)
        }
    }

    fun onTitle(v: String) = update { it.copy(title = v) }
    fun onDescription(v: String) = update { it.copy(description = v) }
    fun onDate(v: LocalDate?) = update { it.copy(dueDate = v) }
    fun onStartDate(v: LocalDate?) = update { it.copy(startDate = v) }
    fun onTime(v: LocalTime?) = update { it.copy(dueTime = v) }
    fun onDuration(v: Int?) = update { it.copy(estimatedMinutes = v) }
    fun onPriority(p: Priority) = update { it.copy(priority = p) }
    fun onProject(id: Long?) = update { it.copy(projectId = id) }
    fun onReminder(enabled: Boolean) = update { it.copy(reminderEnabled = enabled) }
    fun onRecurrence(r: Recurrence) = update { it.copy(recurrence = r) }

    private fun update(block: (Task) -> Task) {
        val current = _state.value.task ?: return
        _state.value = _state.value.copy(task = block(current))
    }

    /** Called by Save button. If task has date+time, check overlaps; else save straight. */
    fun save() {
        val t = _state.value.task ?: return
        if (t.title.isBlank()) return
        viewModelScope.launch {
            val date = t.dueDate
            val time = t.dueTime
            if (date != null && time != null) {
                val others = tasks.tasksOnDateExcept(date.toString(), t.id)
                val conflict = SlotSuggester.findConflict(time, t.effectiveDurationMinutes, others)
                if (conflict != null) {
                    val alts = shiftAlternatives(time, t.effectiveDurationMinutes, others)
                    _state.value = _state.value.copy(conflict = ConflictPrompt(conflict, alts))
                    return@launch
                }
            }
            persist(t)
        }
    }

    fun dismissConflict() { _state.value = _state.value.copy(conflict = null) }

    /** Ignore conflict, save as-is. */
    fun saveDespiteConflict() {
        val t = _state.value.task ?: return
        _state.value = _state.value.copy(conflict = null)
        viewModelScope.launch { persist(t) }
    }

    /** Apply an alternative time from conflict dialog. */
    fun applyAlternative(time: LocalTime) {
        val t = _state.value.task ?: return
        val shifted = t.copy(dueTime = time)
        _state.value = _state.value.copy(task = shifted, conflict = null)
        viewModelScope.launch { persist(shifted) }
    }

    /** Open reschedule pool for the task (used for overdue). */
    fun openReschedule() {
        val t = _state.value.task ?: return
        viewModelScope.launch {
            val today = LocalDate.now()
            val date = t.dueDate ?: today
            val existing = tasks.tasksOnDateExcept(date.toString(), t.id)
            val rhythm = settings.currentRhythm()
            val slots = SlotSuggester.suggest(
                date = if (date.isBefore(today)) today else date,
                now = LocalTime.now(),
                today = today,
                existing = if (date.isBefore(today)) tasks.tasksOnDateExcept(today.toString(), t.id) else existing,
                rhythm = rhythm,
                durationMinutes = t.effectiveDurationMinutes,
            )
            _state.value = _state.value.copy(rescheduleOptions = slots, showReschedule = true)
        }
    }

    fun dismissReschedule() { _state.value = _state.value.copy(showReschedule = false) }

    fun applyReschedule(time: LocalTime) {
        val t = _state.value.task ?: return
        val today = LocalDate.now()
        val newDate = if ((t.dueDate ?: today).isBefore(today)) today else (t.dueDate ?: today)
        val shifted = t.copy(dueDate = newDate, dueTime = time)
        _state.value = _state.value.copy(task = shifted, showReschedule = false)
    }

    private suspend fun persist(t: Task) {
        val saved = if (t.id == 0L) {
            val id = tasks.insert(t)
            t.copy(id = id)
        } else {
            tasks.update(t); t
        }
        val notifId = scheduler.schedule(saved)
        val syncEnabled = settings.isCalendarSyncEnabled() && calendarSync.hasPermission()
        val newEventId = when {
            !syncEnabled && saved.calendarEventId != null -> {
                calendarSync.delete(saved.calendarEventId); null
            }
            syncEnabled && saved.calendarEventId == null && saved.dueDate != null -> {
                calendarSync.insert(saved)
            }
            syncEnabled && saved.calendarEventId != null -> {
                if (saved.dueDate == null) { calendarSync.delete(saved.calendarEventId); null }
                else { calendarSync.update(saved.calendarEventId, saved); saved.calendarEventId }
            }
            else -> saved.calendarEventId
        }
        val finalReminderId = notifId
        if (finalReminderId != saved.reminderId || newEventId != saved.calendarEventId) {
            tasks.update(saved.copy(reminderId = finalReminderId, calendarEventId = newEventId))
        }
        _state.value = _state.value.copy(finished = true)
    }

    fun delete() {
        val t = _state.value.task ?: return
        viewModelScope.launch {
            if (t.id != 0L) {
                scheduler.cancel(t)
                t.calendarEventId?.let { calendarSync.delete(it) }
                tasks.delete(t.id)
            }
            _state.value = _state.value.copy(finished = true)
        }
    }

    /** Suggest three shifts: +15, +30, next free hour. Skip ones that still conflict. */
    private fun shiftAlternatives(original: LocalTime, duration: Int, others: List<Task>): List<LocalTime> {
        val candidates = listOf(15L, 30L, 60L, 90L, 120L).map { original.plusMinutes(it) }
        return candidates.filter { c ->
            SlotSuggester.findConflict(c, duration, others) == null
        }.take(3)
    }
}
