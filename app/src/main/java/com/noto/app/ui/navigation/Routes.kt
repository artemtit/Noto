package com.noto.app.ui.navigation

object Routes {
    const val TODAY = "today"
    const val CALENDAR = "calendar"
    const val SEARCH = "search"
    const val INBOX = "inbox"
    const val PROJECTS = "projects"
    const val PROJECT = "project/{projectId}"
    fun project(id: Long) = "project/$id"
    const val TASK_DETAILS = "task/{taskId}"
    fun taskDetails(id: Long) = "task/$id"
    const val VOICE = "voice"
    const val REVIEW = "review?transcript={transcript}"
    fun review(transcript: String): String {
        val encoded = java.net.URLEncoder.encode(transcript, "UTF-8")
        return "review?transcript=$encoded"
    }
    const val SETTINGS = "settings"
}
