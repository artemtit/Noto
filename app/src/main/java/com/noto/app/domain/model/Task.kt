package com.noto.app.domain.model

import java.time.LocalDate
import java.time.LocalTime

data class Task(
    val id: Long = 0L,
    val title: String,
    val description: String? = null,
    val startDate: LocalDate? = null,
    val dueDate: LocalDate? = null,
    val dueTime: LocalTime? = null,
    val estimatedMinutes: Int? = null,
    val priority: Priority = Priority.MEDIUM,
    val projectId: Long? = null,
    val completed: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val reminderEnabled: Boolean = true,
    val reminderId: Int? = null,
    val calendarEventId: Long? = null,
    val recurrence: Recurrence = Recurrence.NONE,
) {
    val isRange: Boolean get() = startDate != null && dueDate != null && startDate < dueDate
    val effectiveDurationMinutes: Int get() = estimatedMinutes ?: 30
}

data class ParsedTask(
    val title: String,
    val description: String? = null,
    val startDate: LocalDate? = null,
    val dueDate: LocalDate? = null,
    val dueTime: LocalTime? = null,
    val estimatedMinutes: Int? = null,
    val suggestedSlots: List<LocalTime> = emptyList(),
    val checklist: List<String> = emptyList(),
    val priority: Priority = Priority.MEDIUM,
    val projectName: String? = null,
    val reminder: Boolean = true,
)
