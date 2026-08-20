package com.noto.app.ui.screens.projects

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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ProjectsViewModel(private val projects: ProjectRepository) : ViewModel() {
    val state: StateFlow<List<Project>> = projects.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun create(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch { projects.create(name.trim()) }
    }
}

data class ProjectDetailUiState(
    val project: Project? = null,
    val tasks: List<Task> = emptyList(),
    val progressById: Map<Long, ChecklistProgress> = emptyMap(),
    val itemsByTask: Map<Long, List<ChecklistItem>> = emptyMap(),
)

class ProjectDetailViewModel(
    private val tasks: TaskRepository,
    private val projects: ProjectRepository,
    private val checklists: ChecklistRepository,
    private val scheduler: NotoNotificationScheduler,
    private val projectId: Long,
) : ViewModel() {

    private val projectState = MutableStateFlow<Project?>(null)

    val state: StateFlow<ProjectDetailUiState> = run {
        viewModelScope.launch { projectState.value = projects.getById(projectId) }
        combine(
            tasks.observeByProject(projectId),
            projectState,
            checklists.observeAllProgress(),
            checklists.observeAllItems(),
        ) { list, project, progress, items ->
            ProjectDetailUiState(project, list, progress, items)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProjectDetailUiState())
    }

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
