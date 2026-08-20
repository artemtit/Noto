package com.noto.app.calendar

import android.Manifest
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import android.util.Log
import androidx.core.content.ContextCompat
import com.noto.app.domain.model.Task
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Writes tasks to the phone's calendar (Google/Samsung/etc.) via CalendarContract.
 * Requires the WRITE_CALENDAR permission granted at runtime.
 */
class CalendarSyncService(private val context: Context) {

    fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_CALENDAR) ==
            PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) ==
                PackageManager.PERMISSION_GRANTED

    /**
     * Inserts an event for the given task. Returns eventId, or null if not possible
     * (no permission, no calendar, no due date). Defaults to a 30-minute event.
     * If dueTime is null, uses 09:00 as start.
     */
    fun insert(task: Task): Long? {
        if (!hasPermission()) return null
        val date = task.dueDate ?: return null
        val calendarId = defaultCalendarId() ?: return null
        val time = task.dueTime ?: java.time.LocalTime.of(9, 0)
        val zone = ZoneId.systemDefault()
        val start = ZonedDateTime.of(date, time, zone).toInstant().toEpochMilli()
        val end = start + task.effectiveDurationMinutes.toLong() * 60_000

        val values = ContentValues().apply {
            put(CalendarContract.Events.CALENDAR_ID, calendarId)
            put(CalendarContract.Events.TITLE, task.title)
            task.description?.let { put(CalendarContract.Events.DESCRIPTION, it) }
            put(CalendarContract.Events.DTSTART, start)
            put(CalendarContract.Events.DTEND, end)
            put(CalendarContract.Events.EVENT_TIMEZONE, zone.id)
        }
        return try {
            val uri = context.contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)
            uri?.let { ContentUris.parseId(it) }
        } catch (e: SecurityException) {
            Log.w(TAG, "insert denied: ${e.message}")
            null
        } catch (e: Exception) {
            Log.w(TAG, "insert failed: ${e.message}")
            null
        }
    }

    fun delete(eventId: Long): Boolean {
        if (!hasPermission()) return false
        return try {
            val uri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventId)
            context.contentResolver.delete(uri, null, null) > 0
        } catch (e: SecurityException) {
            Log.w(TAG, "delete denied: ${e.message}")
            false
        } catch (e: Exception) {
            Log.w(TAG, "delete failed: ${e.message}")
            false
        }
    }

    fun update(eventId: Long, task: Task): Boolean {
        if (!hasPermission()) return false
        val date = task.dueDate ?: return delete(eventId)
        val time = task.dueTime ?: java.time.LocalTime.of(9, 0)
        val zone = ZoneId.systemDefault()
        val start = ZonedDateTime.of(date, time, zone).toInstant().toEpochMilli()
        val end = start + task.effectiveDurationMinutes.toLong() * 60_000
        val values = ContentValues().apply {
            put(CalendarContract.Events.TITLE, task.title)
            task.description?.let { put(CalendarContract.Events.DESCRIPTION, it) }
            put(CalendarContract.Events.DTSTART, start)
            put(CalendarContract.Events.DTEND, end)
            put(CalendarContract.Events.EVENT_TIMEZONE, zone.id)
        }
        return try {
            val uri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventId)
            context.contentResolver.update(uri, values, null, null) > 0
        } catch (e: SecurityException) {
            Log.w(TAG, "update denied: ${e.message}")
            false
        } catch (e: Exception) {
            Log.w(TAG, "update failed: ${e.message}")
            false
        }
    }

    private fun defaultCalendarId(): Long? {
        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL,
            CalendarContract.Calendars.IS_PRIMARY,
        )
        return try {
            context.contentResolver.query(
                CalendarContract.Calendars.CONTENT_URI,
                projection,
                "${CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL} >= ?",
                arrayOf(CalendarContract.Calendars.CAL_ACCESS_CONTRIBUTOR.toString()),
                null,
            )?.use { c ->
                var primary: Long? = null
                var fallback: Long? = null
                val idIdx = c.getColumnIndexOrThrow(CalendarContract.Calendars._ID)
                val isPrimaryIdx = c.getColumnIndex(CalendarContract.Calendars.IS_PRIMARY)
                while (c.moveToNext()) {
                    val id = c.getLong(idIdx)
                    if (fallback == null) fallback = id
                    if (isPrimaryIdx >= 0 && c.getInt(isPrimaryIdx) == 1) {
                        primary = id
                        break
                    }
                }
                primary ?: fallback
            }
        } catch (e: SecurityException) {
            Log.w(TAG, "query denied: ${e.message}")
            null
        }
    }

    companion object {
        private const val TAG = "NotoCal"
    }
}
