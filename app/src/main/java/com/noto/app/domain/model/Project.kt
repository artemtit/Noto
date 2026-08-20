package com.noto.app.domain.model

data class Project(
    val id: Long,
    val name: String,
    val colorHex: String? = null,
    val isDefault: Boolean = false,
)
