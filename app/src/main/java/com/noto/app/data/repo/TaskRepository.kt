package com.noto.app.data.repo

import com.noto.app.data.db.TaskDao
import com.noto.app.data.entity.TaskEntity
import com.noto.app.domain.model.Task
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class TaskRepository(private val dao: TaskDao) {

    fun observeAll(): Flow<List<Task>> = dao.observeAll().map { list -> list.map { it.toModel() } }

    fun observeByDate(iso: String): Flow<List<Task>> =
        dao.observeByDate(iso).map { list -> list.map { it.toModel() } }

    fun observeByDateRange(fromIso: String, toIso: String): Flow<List<Task>> =
        dao.observeByDateRange(fromIso, toIso).map { list -> list.map { it.toModel() } }

    fun search(q: String): Flow<List<Task>> =
        dao.observeSearch("%${q.trim()}%").map { list -> list.map { it.toModel() } }

    fun observeInbox(): Flow<List<Task>> =
        dao.observeInbox().map { list -> list.map { it.toModel() } }

    fun observeByProject(projectId: Long): Flow<List<Task>> =
        dao.observeByProject(projectId).map { list -> list.map { it.toModel() } }

    suspend fun getById(id: Long): Task? = dao.getById(id)?.toModel()

    suspend fun tasksWithReminders(): List<Task> = dao.tasksWithReminders().map { it.toModel() }

    suspend fun tasksOnDate(iso: String): List<Task> = dao.tasksOnDate(iso).map { it.toModel() }

    suspend fun tasksOnDateExcept(iso: String, excludeId: Long): List<Task> =
        dao.tasksOnDateExcept(iso, excludeId).map { it.toModel() }

    suspend fun insert(task: Task): Long = dao.insert(TaskEntity.fromModel(task))

    suspend fun update(task: Task) {
        dao.update(TaskEntity.fromModel(task.copy(updatedAt = System.currentTimeMillis())))
    }

    suspend fun delete(id: Long) = dao.deleteById(id)

    suspend fun setCompleted(id: Long, completed: Boolean) =
        dao.setCompleted(id, completed, System.currentTimeMillis())

    suspend fun setCalendarEventId(id: Long, eventId: Long?) =
        dao.setCalendarEventId(id, eventId)

    suspend fun spawnNextIfRecurring(task: Task): Task? {
        val date = task.dueDate ?: return null
        val nextDate = task.recurrence.next(date) ?: return null
        val next = task.copy(
            id = 0L,
            dueDate = nextDate,
            completed = false,
            reminderId = null,
            calendarEventId = null,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
        )
        val id = insert(next)
        return next.copy(id = id)
    }
}
