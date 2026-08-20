package com.noto.app.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.noto.app.domain.model.Priority
import com.noto.app.domain.model.Recurrence
import com.noto.app.domain.model.Task
import java.time.LocalDate
import java.time.LocalTime

@Entity(
    tableName = "tasks",
    indices = [Index("dueDate"), Index("projectId"), Index("completed")]
)
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val title: String,
    val description: String? = null,
    val startDate: String? = null, // ISO yyyy-MM-dd
    val dueDate: String? = null,   // ISO yyyy-MM-dd
    val dueTime: String? = null,   // HH:mm
    val estimatedMinutes: Int? = null,
    val priority: String = "medium",
    val projectId: Long? = null,
    val completed: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val reminderEnabled: Boolean = true,
    val reminderId: Int? = null,
    val calendarEventId: Long? = null,
    val recurrence: String = "none",
) {
    fun toModel() = Task(
        id = id,
        title = title,
        description = description,
        startDate = startDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() },
        dueDate = dueDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() },
        dueTime = dueTime?.let { runCatching { LocalTime.parse(it) }.getOrNull() },
        estimatedMinutes = estimatedMinutes,
        priority = Priority.fromString(priority),
        projectId = projectId,
        completed = completed,
        createdAt = createdAt,
        updatedAt = updatedAt,
        reminderEnabled = reminderEnabled,
        reminderId = reminderId,
        calendarEventId = calendarEventId,
        recurrence = Recurrence.fromString(recurrence),
    )

    companion object {
        fun fromModel(t: Task) = TaskEntity(
            id = t.id,
            title = t.title,
            description = t.description,
            startDate = t.startDate?.toString(),
            dueDate = t.dueDate?.toString(),
            dueTime = t.dueTime?.toString()?.substring(0, 5),
            estimatedMinutes = t.estimatedMinutes,
            priority = t.priority.name.lowercase(),
            projectId = t.projectId,
            completed = t.completed,
            createdAt = t.createdAt,
            updatedAt = t.updatedAt,
            reminderEnabled = t.reminderEnabled,
            reminderId = t.reminderId,
            calendarEventId = t.calendarEventId,
            recurrence = t.recurrence.name.lowercase(),
        )
    }
}
