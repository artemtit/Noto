package com.noto.app.ui.screens.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.noto.app.data.repo.ProjectRepository
import com.noto.app.data.repo.TaskRepository
import com.noto.app.domain.model.Project
import com.noto.app.domain.model.Task
import com.noto.app.notifications.NotoNotificationScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class TodayUiState(
    val date: LocalDate = LocalDate.now(),
    val pending: List<Task> = emptyList(),
    val completed: List<Task> = emptyList(),
    val projectsById: Map<Long, Project> = emptyMap(),
) {
    val total: Int get() = pending.size + completed.size
}

class TodayViewModel(
    private val tasks: TaskRepository,
    private val projects: ProjectRepository,
    private val scheduler: NotoNotificationScheduler,
) : ViewModel() {

    private val today = MutableStateFlow(LocalDate.now())

    val state: StateFlow<TodayUiState> =
        combine(tasks.observeByDate(today.value.toString()), projects.observeAll()) { list, projs ->
            val map = projs.associateBy { it.id }
            TodayUiState(
                date = today.value,
                pending = list.filter { !it.completed },
                completed = list.filter { it.completed },
                projectsById = map,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TodayUiState())

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

    fun quickAdd(title: String) {
        if (title.isBlank()) return
        viewModelScope.launch {
            tasks.insert(Task(title = title.trim(), dueDate = today.value))
        }
    }

    fun delete(task: Task) {
        viewModelScope.launch {
            scheduler.cancel(task)
            tasks.delete(task.id)
        }
    }
}
