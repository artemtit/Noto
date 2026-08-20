package com.noto.app.ui.screens.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.noto.app.data.repo.ChecklistRepository
import com.noto.app.data.repo.ProjectRepository
import com.noto.app.data.repo.TaskRepository
import com.noto.app.domain.model.ChecklistItem
import com.noto.app.domain.model.ChecklistProgress
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
    val progressById: Map<Long, ChecklistProgress> = emptyMap(),
    val itemsByTask: Map<Long, List<ChecklistItem>> = emptyMap(),
)

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class SearchViewModel(
    private val tasks: TaskRepository,
    private val projects: ProjectRepository,
    private val checklists: ChecklistRepository,
    private val scheduler: NotoNotificationScheduler,
) : ViewModel() {

    private val q = MutableStateFlow("")

    val state: StateFlow<SearchUiState> = combine(
        q,
        q.debounce(200).flatMapLatest { s ->
            if (s.isBlank()) flowOf(emptyList()) else tasks.search(s)
        },
        projects.observeAll(),
        checklists.observeAllProgress(),
        checklists.observeAllItems(),
    ) { values ->
        @Suppress("UNCHECKED_CAST") val query = values[0] as String
        @Suppress("UNCHECKED_CAST") val list = values[1] as List<Task>
        @Suppress("UNCHECKED_CAST") val projs = values[2] as List<Project>
        @Suppress("UNCHECKED_CAST") val progress = values[3] as Map<Long, ChecklistProgress>
        @Suppress("UNCHECKED_CAST") val items = values[4] as Map<Long, List<ChecklistItem>>
        SearchUiState(
            query = query,
            results = list,
            projectsById = projs.associateBy { it.id },
            progressById = progress,
            itemsByTask = items,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SearchUiState())

    fun setQuery(s: String) { q.value = s }

    fun toggleChecklistItem(item: ChecklistItem) {
        viewModelScope.launch { checklists.toggle(item) }
    }

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
