package com.noto.app.notifications

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.content.getSystemService
import com.noto.app.MainActivity
import com.noto.app.R

class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        NotificationChannels.ensure(context)
        val taskId = intent.getLongExtra(EXTRA_TASK_ID, -1L)
        val title = intent.getStringExtra(EXTRA_TITLE).orEmpty()
        val time = intent.getStringExtra(EXTRA_TIME).orEmpty()
        val notifId = intent.getIntExtra(EXTRA_NOTIF_ID, taskId.toInt().takeIf { it >= 0 } ?: title.hashCode())

        val contentPi = PendingIntent.getActivity(
            context,
            notifId,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra("openTaskId", taskId)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, NotificationChannels.REMINDERS_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(if (time.isNotBlank()) time else context.getString(R.string.app_name))
            .setContentText(title)
            .setStyle(NotificationCompat.BigTextStyle().bigText(title))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(contentPi)
            .build()

        context.getSystemService<NotificationManager>()?.notify(notifId, notification)
    }

    companion object {
        const val EXTRA_TASK_ID = "task_id"
        const val EXTRA_TITLE = "title"
        const val EXTRA_TIME = "time"
        const val EXTRA_NOTIF_ID = "notif_id"
    }
}
