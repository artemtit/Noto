package com.noto.app.ui.screens.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.noto.app.data.repo.ProjectRepository
import com.noto.app.data.repo.TaskRepository
import com.noto.app.domain.model.Project
import com.noto.app.domain.model.Task
import com.noto.app.notifications.NotoNotificationScheduler
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

data class CalendarUiState(
    val month: YearMonth = YearMonth.now(),
    val selectedDate: LocalDate = LocalDate.now(),
    val tasksByDate: Map<LocalDate, List<Task>> = emptyMap(),
    val projectsById: Map<Long, Project> = emptyMap(),
) {
    val selectedTasks: List<Task> get() = tasksByDate[selectedDate].orEmpty()
}

@OptIn(ExperimentalCoroutinesApi::class)
class CalendarViewModel(
    private val tasks: TaskRepository,
    private val projects: ProjectRepository,
    private val scheduler: NotoNotificationScheduler,
) : ViewModel() {

    private val month = MutableStateFlow(YearMonth.now())
    private val selected = MutableStateFlow(LocalDate.now())

    val state: StateFlow<CalendarUiState> = combine(
        month,
        selected,
        month.flatMapLatest { ym ->
            tasks.observeByDateRange(ym.atDay(1).toString(), ym.atEndOfMonth().toString())
        },
        projects.observeAll(),
    ) { ym, sel, list, projs ->
        val map = HashMap<LocalDate, MutableList<Task>>()
        list.forEach { t ->
            val start: LocalDate? = t.startDate ?: t.dueDate
            val end: LocalDate? = t.dueDate ?: t.startDate
            if (start != null && end != null) {
                var d: LocalDate = start
                while (!d.isAfter(end)) {
                    map.getOrPut(d) { mutableListOf() }.add(t)
                    d = d.plusDays(1)
                }
            }
        }
        CalendarUiState(
            month = ym,
            selectedDate = sel,
            tasksByDate = map,
            projectsById = projs.associateBy { it.id },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CalendarUiState())

    fun setMonth(ym: YearMonth) { month.value = ym }
    fun nextMonth() { month.value = month.value.plusMonths(1) }
    fun prevMonth() { month.value = month.value.minusMonths(1) }
    fun goToToday() {
        val today = LocalDate.now()
        month.value = YearMonth.from(today)
        selected.value = today
    }
    fun select(day: LocalDate) { selected.value = day }

    fun toggle(task: Task) {
        viewModelScope.launch {
            val newValue = !task.completed
            tasks.setCompleted(task.id, newValue)
            if (newValue) {
                scheduler.cancel(task)
                tasks.spawnNextIfRecurring(task)?.let { scheduler.schedule(it) }
            } else {
                scheduler.schedule(task.copy(completed = false))
            }
        }
    }

    fun delete(task: Task) {
        viewModelScope.launch {
            scheduler.cancel(task)
            tasks.delete(task.id)
        }
    }
}
