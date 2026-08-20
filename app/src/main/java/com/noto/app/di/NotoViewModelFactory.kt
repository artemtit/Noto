package com.noto.app.di

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.noto.app.ui.screens.calendar.CalendarViewModel
import com.noto.app.ui.screens.inbox.InboxViewModel
import com.noto.app.ui.screens.projects.ProjectDetailViewModel
import com.noto.app.ui.screens.search.SearchViewModel
import com.noto.app.ui.screens.projects.ProjectsViewModel
import com.noto.app.ui.screens.review.ReviewViewModel
import com.noto.app.ui.screens.settings.SettingsViewModel
import com.noto.app.ui.screens.taskdetails.TaskDetailsViewModel
import com.noto.app.ui.screens.today.TodayViewModel
import com.noto.app.ui.screens.voice.VoiceCaptureViewModel

class NotoViewModelFactory(
    private val container: ServiceContainer,
    private val extras: Map<String, Any?> = emptyMap(),
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(TodayViewModel::class.java) ->
                TodayViewModel(
                    container.taskRepository,
                    container.projectRepository,
                    container.checklistRepository,
                    container.notificationScheduler,
                ) as T
            modelClass.isAssignableFrom(InboxViewModel::class.java) ->
                InboxViewModel(
                    container.taskRepository,
                    container.projectRepository,
                    container.checklistRepository,
                    container.notificationScheduler,
                ) as T
            modelClass.isAssignableFrom(ProjectsViewModel::class.java) ->
                ProjectsViewModel(container.projectRepository) as T
            modelClass.isAssignableFrom(ProjectDetailViewModel::class.java) ->
                ProjectDetailViewModel(
                    container.taskRepository,
                    container.projectRepository,
                    container.checklistRepository,
                    container.notificationScheduler,
                    (extras["projectId"] as? Long) ?: 0L,
                ) as T
            modelClass.isAssignableFrom(TaskDetailsViewModel::class.java) ->
                TaskDetailsViewModel(
                    container.taskRepository,
                    container.projectRepository,
                    container.checklistRepository,
                    container.notificationScheduler,
                    container.calendarSyncService,
                    container.settingsRepository,
                    (extras["taskId"] as? Long) ?: 0L,
                ) as T
            modelClass.isAssignableFrom(VoiceCaptureViewModel::class.java) ->
                VoiceCaptureViewModel(container.speechService) as T
            modelClass.isAssignableFrom(ReviewViewModel::class.java) ->
                ReviewViewModel(
                    container.taskParser,
                    container.projectRepository,
                    container.taskRepository,
                    container.checklistRepository,
                    container.notificationScheduler,
                    container.calendarSyncService,
                    container.settingsRepository,
                ) as T
            modelClass.isAssignableFrom(SettingsViewModel::class.java) ->
                SettingsViewModel(container.settingsRepository) as T
            modelClass.isAssignableFrom(CalendarViewModel::class.java) ->
                CalendarViewModel(
                    container.taskRepository,
                    container.projectRepository,
                    container.checklistRepository,
                    container.notificationScheduler,
                ) as T
            modelClass.isAssignableFrom(SearchViewModel::class.java) ->
                SearchViewModel(
                    container.taskRepository,
                    container.projectRepository,
                    container.checklistRepository,
                    container.notificationScheduler,
                ) as T
            else -> error("Unknown VM: ${modelClass.name}")
        }
    }
}
