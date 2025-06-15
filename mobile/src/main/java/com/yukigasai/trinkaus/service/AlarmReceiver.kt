package com.yukigasai.trinkaus.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.yukigasai.trinkaus.util.NotificationWorker

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent?,
    ) {
        Log.d("Trinkaus", "AlarmReceiver triggered! Enqueuing NotificationWorker.")
        val workRequest = OneTimeWorkRequestBuilder<NotificationWorker>().build()
        WorkManager.getInstance(context).enqueue(workRequest)
    }
}
