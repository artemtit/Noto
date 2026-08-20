package com.noto.app.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.content.getSystemService
import com.noto.app.R

object NotificationChannels {
    const val REMINDERS_ID = "reminders"

    fun ensure(context: Context) {
        val nm = context.getSystemService<NotificationManager>() ?: return
        if (nm.getNotificationChannel(REMINDERS_ID) == null) {
            val channel = NotificationChannel(
                REMINDERS_ID,
                context.getString(R.string.notif_channel_reminders),
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = context.getString(R.string.notif_channel_reminders_desc)
                enableLights(true)
                enableVibration(true)
            }
            nm.createNotificationChannel(channel)
        }
    }
}
