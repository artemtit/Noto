package com.noto.app.domain.model

enum class Priority { LOW, MEDIUM, HIGH;

    companion object {
        fun fromString(s: String?): Priority = when (s?.lowercase()) {
            "low" -> LOW
            "high" -> HIGH
            else -> MEDIUM
        }
    }
}
