package com.noto.app.ui.screens.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.noto.app.data.repo.ProjectRepository
import com.noto.app.data.repo.TaskRepository
import com.noto.app.domain.model.Project
import com.noto.app.domain.model.Task
import com.noto.app.notifications.NotoNotificationScheduler
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SearchUiState(
    val query: String = "",
    val results: List<Task> = emptyList(),
    val projectsById: Map<Long, Project> = emptyMap(),
)

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class SearchViewModel(
    private val tasks: TaskRepository,
    private val projects: ProjectRepository,
    private val scheduler: NotoNotificationScheduler,
) : ViewModel() {

    private val q = MutableStateFlow("")

    val state: StateFlow<SearchUiState> = combine(
        q,
        q.debounce(200).flatMapLatest { s ->
            if (s.isBlank()) flowOf(emptyList()) else tasks.search(s)
        },
        projects.observeAll(),
    ) { query, list, projs ->
        SearchUiState(query = query, results = list, projectsById = projs.associateBy { it.id })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SearchUiState())

    fun setQuery(s: String) { q.value = s }

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
