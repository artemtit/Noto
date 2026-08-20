package com.noto.app.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.getSystemService
import com.noto.app.core.DateTimeUtils
import com.noto.app.domain.model.Task
import java.time.ZoneId

class NotoNotificationScheduler(private val context: Context) {

    private val alarmManager: AlarmManager?
        get() = context.getSystemService()

    /**
     * Schedules or reschedules a reminder for the given task. Returns the notification id
     * that was used (which callers should persist on the task so the alarm can be cancelled later).
     */
    fun schedule(task: Task): Int? {
        val date = task.dueDate ?: return null
        val time = task.dueTime ?: return null
        if (!task.reminderEnabled || task.completed) return null

        val triggerAt = DateTimeUtils.triggerMillis(date, time, ZoneId.systemDefault())
        if (triggerAt <= System.currentTimeMillis()) return null

        val notifId = task.reminderId ?: stableId(task)
        val pi = pendingIntentOrCreate(task, notifId)
        val am = alarmManager ?: return null

        val canExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || am.canScheduleExactAlarms()
        try {
            if (canExact) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
            } else {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
            }
        } catch (_: SecurityException) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
        }
        return notifId
    }

    fun cancel(task: Task) {
        val notifId = task.reminderId ?: stableId(task)
        val pi = pendingIntentIfExists(task, notifId) ?: return
        alarmManager?.cancel(pi)
        pi.cancel()
    }

    private fun stableId(task: Task): Int = if (task.id != 0L) task.id.toInt() else task.title.hashCode()

    private fun buildIntent(task: Task, notifId: Int) =
        Intent(context, ReminderReceiver::class.java).apply {
            action = "com.noto.app.ACTION_REMINDER_$notifId"
            putExtra(ReminderReceiver.EXTRA_TASK_ID, task.id)
            putExtra(ReminderReceiver.EXTRA_TITLE, task.title)
            putExtra(ReminderReceiver.EXTRA_TIME, task.dueTime?.toString()?.substring(0, 5).orEmpty())
            putExtra(ReminderReceiver.EXTRA_NOTIF_ID, notifId)
        }

    private fun pendingIntentOrCreate(task: Task, notifId: Int): PendingIntent =
        PendingIntent.getBroadcast(
            context, notifId, buildIntent(task, notifId),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    private fun pendingIntentIfExists(task: Task, notifId: Int): PendingIntent? =
        PendingIntent.getBroadcast(
            context, notifId, buildIntent(task, notifId),
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
        )
}
