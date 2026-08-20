package com.noto.app.domain.model

import java.time.LocalDate
import java.time.LocalTime

/**
 * A mutation to apply to an existing task, returned by the AI when the user's utterance is a
 * command (complete / delete / reschedule) rather than a task-creation request.
 */
data class TaskAction(
    val kind: Kind,
    val taskId: Long,
    val newDate: LocalDate? = null,
    val newTime: LocalTime? = null,
) {
    enum class Kind { COMPLETE, DELETE, RESCHEDULE }
}

data class ExistingTaskRef(
    val id: Long,
    val title: String,
    val whenLabel: String? = null,
)
