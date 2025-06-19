package com.yukigasai.trinkaus.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.yukigasai.trinkaus.service.WaterServerService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NotificationStarter : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            scheduleMidnightUpdate(context)
            CoroutineScope(Dispatchers.IO).launch {
                ReminderScheduler.startOrRescheduleReminders(context)

                // If the Local Server is enabled, start it

                if (ServerManager.isEnabled(context)) {
                    val serverIntent = Intent(context, WaterServerService::class.java)
                    context.startForegroundService(serverIntent)
                }
            }
        }
    }
}
