package com.yukigasai.trinkaus.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
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
        startForegroundService()
        scope.launch {
            startKtorServer()
        }
        return START_STICKY
    }

    private fun startKtorServer() {
        ServerManager.startServer(this@WaterServerService)
    }

    private fun startForegroundService() {
        val channelId = "WaterIntakeServiceChannel"
        val channel = NotificationChannel(channelId, "PC Sync Service", NotificationManager.IMPORTANCE_DEFAULT)
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)

        val notification: Notification =
            NotificationCompat
                .Builder(this, channelId)
                .setContentTitle("PC Sync Active")
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
                    // Open this
                    PendingIntent.getActivity(
                        this,
                        0,
                        Intent(this, WaterServerService::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        },
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                    ),
                ).build()

        startForeground(1, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        ServerManager.stopServer()
        job.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
