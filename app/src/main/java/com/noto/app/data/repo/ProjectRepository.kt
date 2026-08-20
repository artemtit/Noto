package com.noto.app.data.repo

import com.noto.app.data.db.ProjectDao
import com.noto.app.data.entity.ProjectEntity
import com.noto.app.domain.model.Project
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ProjectRepository(private val dao: ProjectDao) {

    fun observeAll(): Flow<List<Project>> = dao.observeAll().map { list -> list.map { it.toModel() } }

    suspend fun getAll(): List<Project> = dao.getAll().map { it.toModel() }

    suspend fun getById(id: Long): Project? = dao.getById(id)?.toModel()

    suspend fun findByName(name: String): Project? = dao.findByName(name)?.toModel()

    suspend fun findOrCreateByName(name: String): Project {
        findByName(name)?.let { return it }
        val id = dao.insert(ProjectEntity(name = name.trim()))
        return if (id > 0) Project(id, name.trim())
        else findByName(name)!!
    }

    suspend fun create(name: String, colorHex: String? = null): Long =
        dao.insert(ProjectEntity(name = name.trim(), colorHex = colorHex))

    suspend fun delete(id: Long) = dao.deleteById(id)
}
