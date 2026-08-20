package com.noto.app.domain.model

data class ChecklistItem(
    val id: Long = 0L,
    val taskId: Long,
    val position: Int,
    val text: String,
    val done: Boolean = false,
)

data class ChecklistProgress(
    val taskId: Long,
    val total: Int,
    val done: Int,
)
