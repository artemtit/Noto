package com.noto.app.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.noto.app.domain.model.ChecklistItem

@Entity(
    tableName = "checklist_items",
    foreignKeys = [
        ForeignKey(
            entity = TaskEntity::class,
            parentColumns = ["id"],
            childColumns = ["taskId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index("taskId")],
)
data class ChecklistItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val taskId: Long,
    val position: Int,
    val text: String,
    val done: Boolean = false,
) {
    fun toModel() = ChecklistItem(
        id = id,
        taskId = taskId,
        position = position,
        text = text,
        done = done,
    )

    companion object {
        fun fromModel(m: ChecklistItem) = ChecklistItemEntity(
            id = m.id,
            taskId = m.taskId,
            position = m.position,
            text = m.text,
            done = m.done,
        )
    }
}
