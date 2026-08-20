package com.noto.app

import android.app.Application
import com.noto.app.di.ServiceContainer
import com.noto.app.notifications.NotificationChannels

class NotoApplication : Application() {

    lateinit var container: ServiceContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = ServiceContainer(this)
        NotificationChannels.ensure(this)
    }
}
