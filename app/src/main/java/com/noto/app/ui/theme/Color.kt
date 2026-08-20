package com.noto.app.ui.theme

import androidx.compose.ui.graphics.Color

// Noto accent — indigo/violet
val NotoAccent = Color(0xFF6366F1)
val NotoAccentDark = Color(0xFF818CF8)
val NotoAccentSoft = Color(0xFFEEF0FF)
val NotoAccentSoftDark = Color(0xFF23253A)

// Light palette
val LightBg = Color(0xFFFAFAFB)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceElev = Color(0xFFF4F4F7)
val LightOnBg = Color(0xFF0F0F14)
val LightOnSurfaceMuted = Color(0xFF6B7280)
val LightOutline = Color(0xFFE6E6EA)

// Dark palette
val DarkBg = Color(0xFF0A0A0F)
val DarkSurface = Color(0xFF14141C)
val DarkSurfaceElev = Color(0xFF1C1C26)
val DarkOnBg = Color(0xFFF5F5F7)
val DarkOnSurfaceMuted = Color(0xFF9CA3AF)
val DarkOutline = Color(0xFF23232E)

val PriorityHigh = Color(0xFFEF4444)
val PriorityMedium = Color(0xFFF59E0B)
val PriorityLow = Color(0xFF10B981)

// Project palette (stable seed by id)
val ProjectPalette = listOf(
    Color(0xFF6366F1), // indigo
    Color(0xFFEC4899), // pink
    Color(0xFF10B981), // emerald
    Color(0xFFF59E0B), // amber
    Color(0xFF06B6D4), // cyan
    Color(0xFF8B5CF6), // violet
    Color(0xFFEF4444), // red
    Color(0xFF14B8A6), // teal
)

fun projectColor(id: Long): Color =
    ProjectPalette[((id.hashCode() ushr 1) % ProjectPalette.size).let { if (it < 0) it + ProjectPalette.size else it }]
