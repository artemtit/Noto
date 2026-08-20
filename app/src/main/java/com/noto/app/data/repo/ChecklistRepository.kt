package com.noto.app.data.repo

import com.noto.app.data.db.ChecklistDao
import com.noto.app.data.entity.ChecklistItemEntity
import com.noto.app.domain.model.ChecklistItem
import com.noto.app.domain.model.ChecklistProgress
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ChecklistRepository(private val dao: ChecklistDao) {

    fun observe(taskId: Long): Flow<List<ChecklistItem>> =
        dao.observeByTask(taskId).map { list -> list.map { it.toModel() } }

    fun observeAllProgress(): Flow<Map<Long, ChecklistProgress>> =
        dao.observeAllProgress().map { list -> list.associateBy { it.taskId } }

    fun observeAllItems(): Flow<Map<Long, List<ChecklistItem>>> =
        dao.observeAll().map { list -> list.map { it.toModel() }.groupBy { it.taskId } }

    suspend fun add(taskId: Long, text: String): Long {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return 0L
        val existing = dao.byTask(taskId)
        return dao.insert(
            ChecklistItemEntity(
                taskId = taskId,
                position = existing.size,
                text = trimmed,
            )
        )
    }

    suspend fun toggle(item: ChecklistItem) =
        dao.update(ChecklistItemEntity.fromModel(item.copy(done = !item.done)))

    suspend fun updateText(item: ChecklistItem, text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) {
            dao.deleteById(item.id)
        } else {
            dao.update(ChecklistItemEntity.fromModel(item.copy(text = trimmed)))
        }
    }

    suspend fun delete(id: Long) = dao.deleteById(id)
}
