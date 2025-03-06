package com.yukigasai.trinkaus

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

class NotificationStarter : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == "android.intent.action.BOOT_COMPLETED") {
            context?.let {
                val workRequest =
                    PeriodicWorkRequestBuilder<NotificationWorker>(2, TimeUnit.HOURS).build()
                WorkManager.getInstance(it).enqueue(workRequest)
            }
        }
    }
}

