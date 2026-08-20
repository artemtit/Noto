package com.noto.app.ui.screens.inbox

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
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class InboxUiState(
    val tasks: List<Task> = emptyList(),
    val projectsById: Map<Long, Project> = emptyMap(),
    val progressById: Map<Long, ChecklistProgress> = emptyMap(),
    val itemsByTask: Map<Long, List<ChecklistItem>> = emptyMap(),
)

class InboxViewModel(
    private val tasks: TaskRepository,
    private val projects: ProjectRepository,
    private val checklists: ChecklistRepository,
    private val scheduler: NotoNotificationScheduler,
) : ViewModel() {

    val state: StateFlow<InboxUiState> =
        combine(
            tasks.observeInbox(),
            projects.observeAll(),
            checklists.observeAllProgress(),
            checklists.observeAllItems(),
        ) { list, projs, progress, items ->
            InboxUiState(list, projs.associateBy { it.id }, progress, items)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), InboxUiState())

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
