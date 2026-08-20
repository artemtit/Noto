package com.noto.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.noto.app.data.entity.TaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {

    @Query("SELECT * FROM tasks ORDER BY completed ASC, dueDate ASC, dueTime ASC, createdAt DESC")
    fun observeAll(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE dueDate = :isoDate ORDER BY completed ASC, dueTime ASC, createdAt DESC")
    fun observeByDate(isoDate: String): Flow<List<TaskEntity>>

    @Query("""SELECT * FROM tasks WHERE
        (dueDate IS NOT NULL AND dueDate >= :fromIso AND dueDate <= :toIso)
        OR (startDate IS NOT NULL AND startDate <= :toIso AND dueDate >= :fromIso)
        ORDER BY dueDate ASC, dueTime ASC, createdAt DESC""")
    fun observeByDateRange(fromIso: String, toIso: String): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE (title LIKE :q OR description LIKE :q) ORDER BY completed ASC, dueDate ASC, dueTime ASC, createdAt DESC")
    fun observeSearch(q: String): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE (dueDate IS NULL OR projectId IS NULL) AND completed = 0 ORDER BY createdAt DESC")
    fun observeInbox(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE projectId = :projectId ORDER BY completed ASC, dueDate ASC, dueTime ASC, createdAt DESC")
    fun observeByProject(projectId: Long): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getById(id: Long): TaskEntity?

    @Query("SELECT * FROM tasks WHERE dueDate = :isoDate AND completed = 0")
    suspend fun tasksOnDate(isoDate: String): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE dueDate = :isoDate AND id != :excludeId AND completed = 0")
    suspend fun tasksOnDateExcept(isoDate: String, excludeId: Long): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE reminderEnabled = 1 AND completed = 0 AND dueDate IS NOT NULL AND dueTime IS NOT NULL")
    suspend fun tasksWithReminders(): List<TaskEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(task: TaskEntity): Long

    @Update
    suspend fun update(task: TaskEntity)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE tasks SET completed = :completed, updatedAt = :updatedAt WHERE id = :id")
    suspend fun setCompleted(id: Long, completed: Boolean, updatedAt: Long)

    @Query("UPDATE tasks SET calendarEventId = :eventId WHERE id = :id")
    suspend fun setCalendarEventId(id: Long, eventId: Long?)
}
