package com.noto.app.ui.screens.review

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.noto.app.ai.BusySlot
import com.noto.app.ai.SlotSuggester
import com.noto.app.ai.TaskParser
import com.noto.app.calendar.CalendarSyncService
import com.noto.app.core.AppError
import com.noto.app.core.AppResult
import com.noto.app.core.DateTimeUtils
import com.noto.app.data.prefs.SettingsRepository
import com.noto.app.data.repo.ChecklistRepository
import com.noto.app.data.repo.ProjectRepository
import com.noto.app.data.repo.TaskRepository
import com.noto.app.domain.model.ExistingTaskRef
import com.noto.app.domain.model.ParsedTask
import com.noto.app.domain.model.Priority
import com.noto.app.domain.model.Project
import com.noto.app.domain.model.Recurrence
import com.noto.app.domain.model.Task
import com.noto.app.domain.model.TaskAction
import com.noto.app.notifications.NotoNotificationScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
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
    /** Times already offered on previous rounds — excluded from the next regeneration. */
    val exhaustedSlots: Set<LocalTime> = emptySet(),
    val checklist: List<String> = emptyList(),
    val priority: Priority,
    val projectId: Long?,
    val reminder: Boolean,
    val recurrence: Recurrence = Recurrence.NONE,
)

data class ReviewAction(
    val id: String,
    val action: TaskAction,
    val task: Task,
    val description: String,
)

data class ReviewUiState(
    val loading: Boolean = false,
    val transcript: String = "",
    val items: List<ReviewItem> = emptyList(),
    val actions: List<ReviewAction> = emptyList(),
    val projects: List<Project> = emptyList(),
    val error: AppError? = null,
    val saved: Boolean = false,
)

class ReviewViewModel(
    private val parser: TaskParser,
    private val projects: ProjectRepository,
    private val tasks: TaskRepository,
    private val checklists: ChecklistRepository,
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

            val openTasks = tasks.openTasks(limit = 50)
            val existingRefs = openTasks.map { t ->
                ExistingTaskRef(id = t.id, title = t.title, whenLabel = whenLabel(t, today))
            }

            val result = parser.parse(
                transcript = transcript,
                now = now,
                locale = Locale.getDefault(),
                knownProjects = projs.map { it.name },
                busyToday = busy,
                rhythm = rhythm,
                existingTasks = existingRefs,
            )
            when (result) {
                is AppResult.Ok -> {
                    val wantsPool = SlotSuggester.wantsTimePool(transcript)
                    val items = result.value.tasks.mapIndexed { i, p ->
                        val base = p.toReviewItem(i, projs)
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
                    val byId = openTasks.associateBy { it.id }
                    val actions = result.value.actions.mapIndexedNotNull { i, a ->
                        val t = byId[a.taskId] ?: return@mapIndexedNotNull null
                        ReviewAction(
                            id = "a$i",
                            action = a,
                            task = t,
                            description = describeAction(a, t),
                        )
                    }
                    _state.value = _state.value.copy(
                        loading = false,
                        items = items,
                        actions = actions,
                        projects = projs,
                    )
                }
                is AppResult.Err -> _state.value = _state.value.copy(loading = false, error = result.error, projects = projs)
            }
        }
    }

    fun updateItem(id: String, transform: (ReviewItem) -> ReviewItem) {
        _state.update { s -> s.copy(items = s.items.map { if (it.id == id) transform(it) else it }) }
    }

    fun pickSlot(id: String, slot: LocalTime) = updateItem(id) {
        it.copy(dueTime = slot, suggestedSlots = emptyList())
    }

    /** #5: regenerate a fresh set of slot suggestions, avoiding times we've already offered. */
    fun regenerateSlots(id: String) {
        val current = _state.value.items.firstOrNull { it.id == id } ?: return
        viewModelScope.launch {
            val today = LocalDate.now()
            val date = current.dueDate ?: today
            val existing = tasks.tasksOnDate(date.toString())
            val rhythm = settings.currentRhythm()
            val exhausted = current.exhaustedSlots + current.suggestedSlots
            val fresh = SlotSuggester.suggest(
                date = date,
                now = LocalTime.now(),
                today = today,
                existing = existing,
                rhythm = rhythm,
                durationMinutes = current.estimatedMinutes ?: 30,
                excluding = exhausted,
            )
            updateItem(id) { it.copy(suggestedSlots = fresh, exhaustedSlots = exhausted) }
        }
    }

    fun addChecklistItem(id: String, text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        updateItem(id) { it.copy(checklist = it.checklist + trimmed) }
    }

    fun removeChecklistItem(id: String, index: Int) = updateItem(id) {
        if (index !in it.checklist.indices) it
        else it.copy(checklist = it.checklist.toMutableList().apply { removeAt(index) })
    }

    fun remove(id: String) {
        _state.update { it.copy(items = it.items.filterNot { it.id == id }) }
    }

    fun removeAction(id: String) {
        _state.update { it.copy(actions = it.actions.filterNot { it.id == id }) }
    }

    fun confirm() {
        val current = _state.value
        if (current.items.isEmpty() && current.actions.isEmpty()) return
        viewModelScope.launch {
            current.actions.forEach { ra -> applyAction(ra) }
            current.items.forEach { r ->
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
                r.checklist.forEach { text -> checklists.add(id, text) }
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

    private suspend fun applyAction(ra: ReviewAction) {
        val t = ra.task
        when (ra.action.kind) {
            TaskAction.Kind.COMPLETE -> {
                tasks.setCompleted(t.id, true)
                scheduler.cancel(t)
                tasks.spawnNextIfRecurring(t)?.let { scheduler.schedule(it) }
            }
            TaskAction.Kind.DELETE -> {
                scheduler.cancel(t)
                t.calendarEventId?.let { calendarSync.delete(it) }
                tasks.delete(t.id)
            }
            TaskAction.Kind.RESCHEDULE -> {
                val newDate = ra.action.newDate ?: t.dueDate
                val newTime = ra.action.newTime ?: t.dueTime
                val updated = t.copy(dueDate = newDate, dueTime = newTime)
                tasks.update(updated)
                scheduler.cancel(t)
                scheduler.schedule(updated)
                if (settings.isCalendarSyncEnabled() && calendarSync.hasPermission()) {
                    val eid = updated.calendarEventId
                    if (eid != null) calendarSync.update(eid, updated)
                    else if (updated.dueDate != null) calendarSync.insert(updated)?.let { newId ->
                        tasks.update(updated.copy(calendarEventId = newId))
                    }
                }
            }
        }
    }

    private fun describeAction(a: TaskAction, t: Task): String {
        val ru = Locale.getDefault().language == "ru"
        return when (a.kind) {
            TaskAction.Kind.COMPLETE ->
                if (ru) "Отметить «${t.title}» выполненной" else "Mark «${t.title}» done"
            TaskAction.Kind.DELETE ->
                if (ru) "Удалить «${t.title}»" else "Delete «${t.title}»"
            TaskAction.Kind.RESCHEDULE -> {
                val d = a.newDate?.toString() ?: "—"
                val time = a.newTime?.toString()?.take(5) ?: ""
                val whenStr = listOf(d, time).filter { it.isNotBlank() }.joinToString(" ")
                if (ru) "Перенести «${t.title}» → $whenStr" else "Move «${t.title}» → $whenStr"
            }
        }
    }

    private fun whenLabel(t: Task, today: LocalDate): String? {
        val d = t.dueDate ?: return null
        val date = DateTimeUtils.formatDateShort(d, today, Locale.getDefault())
        val time = t.dueTime?.let { DateTimeUtils.formatTime(it) }
        return if (time != null) "$date $time" else date
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
        checklist = checklist,
        priority = priority,
        projectId = projId,
        reminder = reminder,
    )
}

private fun <T, R : Any> List<T>.mapIndexedNotNull(transform: (Int, T) -> R?): List<R> {
    val out = ArrayList<R>(size)
    forEachIndexed { i, v -> transform(i, v)?.let { out.add(it) } }
    return out
}
