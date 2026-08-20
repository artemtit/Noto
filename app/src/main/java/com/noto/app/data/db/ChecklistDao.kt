package com.noto.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.noto.app.data.entity.ChecklistItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChecklistDao {

    @Query("SELECT * FROM checklist_items WHERE taskId = :taskId ORDER BY position ASC, id ASC")
    fun observeByTask(taskId: Long): Flow<List<ChecklistItemEntity>>

    @Query("SELECT * FROM checklist_items WHERE taskId = :taskId ORDER BY position ASC, id ASC")
    suspend fun byTask(taskId: Long): List<ChecklistItemEntity>

    @Query("SELECT COUNT(*) FROM checklist_items WHERE taskId = :taskId")
    suspend fun countByTask(taskId: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: ChecklistItemEntity): Long

    @Update
    suspend fun update(item: ChecklistItemEntity)

    @Query("DELETE FROM checklist_items WHERE id = :id")
    suspend fun deleteById(id: Long)
}
