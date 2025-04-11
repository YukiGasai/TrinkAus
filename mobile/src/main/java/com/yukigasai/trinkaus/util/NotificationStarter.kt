package com.yukigasai.trinkaus.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.yukigasai.trinkaus.util.NotificationWorker
import com.yukigasai.trinkaus.shared.Constants
import java.util.concurrent.TimeUnit

class NotificationStarter : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            scheduleHydrationReminders(context)
        }
    }
}

fun scheduleHydrationReminders(context: Context) {
    val repeatingRequest = PeriodicWorkRequestBuilder<NotificationWorker>(1, TimeUnit.HOURS)
        .build()

    WorkManager.getInstance(context).enqueueUniquePeriodicWork(
        Constants.Notification.WORKER_TAG,
        ExistingPeriodicWorkPolicy.KEEP,
        repeatingRequest
    )
}