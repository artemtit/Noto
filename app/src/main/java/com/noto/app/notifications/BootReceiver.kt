package com.noto.app.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.noto.app.NotoApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != Intent.ACTION_MY_PACKAGE_REPLACED
        ) return
        val app = context.applicationContext as? NotoApplication ?: return
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val tasks = app.container.taskRepository.tasksWithReminders()
                tasks.forEach { app.container.notificationScheduler.schedule(it) }
            } finally {
                pending.finish()
            }
        }
    }
}
