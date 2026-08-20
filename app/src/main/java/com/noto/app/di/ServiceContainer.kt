package com.noto.app.di

import android.content.Context
import com.noto.app.ai.OpenAiTaskParser
import com.noto.app.ai.TaskParser
import com.noto.app.calendar.CalendarSyncService
import com.noto.app.data.db.NotoDatabase
import com.noto.app.data.prefs.SettingsRepository
import com.noto.app.data.repo.ProjectRepository
import com.noto.app.data.repo.TaskRepository
import com.noto.app.notifications.NotoNotificationScheduler
import com.noto.app.speech.AndroidSpeechRecognizerService
import com.noto.app.speech.SpeechToTextService

class ServiceContainer(context: Context) {
    private val appContext = context.applicationContext

    private val database by lazy { NotoDatabase.getInstance(appContext) }

    val settingsRepository by lazy { SettingsRepository(appContext) }
    val taskRepository by lazy { TaskRepository(database.taskDao()) }
    val projectRepository by lazy { ProjectRepository(database.projectDao()) }

    val notificationScheduler by lazy { NotoNotificationScheduler(appContext) }
    val calendarSyncService by lazy { CalendarSyncService(appContext) }
    val speechService: SpeechToTextService by lazy { AndroidSpeechRecognizerService(appContext) }
    val taskParser: TaskParser by lazy { OpenAiTaskParser(settingsRepository) }
}
