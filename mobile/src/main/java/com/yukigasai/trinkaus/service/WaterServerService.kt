package com.yukigasai.trinkaus.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.yukigasai.trinkaus.R
import com.yukigasai.trinkaus.util.ServerManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class WaterServerService : Service() {
    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        if (intent?.action == ACTION_START) {
            promoteToForeground()
            scope.launch {
                startKtorServer()
            }
        } else {
            stopSelf()
        }

        return START_NOT_STICKY
    }

    private fun startKtorServer() {
        ServerManager.startServer(this@WaterServerService)
    }

    private fun promoteToForeground() {
        val notification = createNotification(this@WaterServerService)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                1,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
            )
        } else {
            startForeground(1, notification)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        ServerManager.stopServer()
        job.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_START = "com.yukigasai.trinkaus.service.ACTION_START"

        private fun createNotifcaionChannel(context: Context) {
            val channelId = "WaterIntakeServiceChannel"
            val channel =
                NotificationChannel(
                    channelId,
                    "PC Sync Service",
                    NotificationManager.IMPORTANCE_DEFAULT,
                )
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        fun createNotification(
            context: Context,
            text: String = "PC Sync Active",
        ): Notification {
            createNotifcaionChannel(context)
            return NotificationCompat
                .Builder(context, "WaterIntakeServiceChannel")
                .setContentTitle(text)
                .setContentText("Listening for water intake data on port 8080.")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .setOngoing(true)
                .setForegroundServiceBehavior(
                    NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE,
                ).setSilent(true)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .setStyle(NotificationCompat.BigTextStyle().bigText("Listening for water intake data on port 8080."))
                .setContentIntent(
                    PendingIntent.getActivity(
                        context,
                        0,
                        Intent(context, WaterServerService::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        },
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                    ),
                ).build()
        }
    }
}
