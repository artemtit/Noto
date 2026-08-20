package com.noto.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.noto.app.domain.model.Project

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val colorHex: String? = null,
    val isDefault: Boolean = false,
) {
    fun toModel() = Project(id, name, colorHex, isDefault)

    companion object {
        fun fromModel(p: Project) = ProjectEntity(p.id, p.name, p.colorHex, p.isDefault)
    }
}
