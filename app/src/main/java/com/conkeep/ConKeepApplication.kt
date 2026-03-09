package com.conkeep

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.conkeep.notification.NotificationHelper
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class ConKeepApplication :
    Application(),
    Configuration.Provider {
    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var notificationHelper: NotificationHelper

    override val workManagerConfiguration: Configuration
        get() =
            Configuration
                .Builder()
                .setWorkerFactory(workerFactory) // 중요: Hilt가 Worker를 만들게 함
                .setMinimumLoggingLevel(android.util.Log.DEBUG)
                .build()

    override fun onCreate() {
        super.onCreate()
        notificationHelper.createAllChannels()
    }
}
