package com.yukigasai.trinkaus.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.yukigasai.trinkaus.R
import com.yukigasai.trinkaus.presentation.MainActivity
import com.yukigasai.trinkaus.shared.Constants
import com.yukigasai.trinkaus.shared.Constants.DataStore.DataStoreKeys
import com.yukigasai.trinkaus.shared.DataStoreSingleton
import com.yukigasai.trinkaus.shared.HydrationOption
import com.yukigasai.trinkaus.shared.UnitHelper
import com.yukigasai.trinkaus.shared.UnitHelper.getVolumeString
import com.yukigasai.trinkaus.shared.getDisplayName
import com.yukigasai.trinkaus.util.ServerManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class WaterServerService : Service() {
    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)
    private lateinit var connectivityManager: ConnectivityManager

    private val networkCallback =
        object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                super.onAvailable(network)
                println("Network available. Starting server...")
                scope.launch {
                    val dataStore = DataStoreSingleton.getInstance(this@WaterServerService)
                    val isServerEnabled = dataStore.data.firstOrNull()?.get(DataStoreKeys.USE_LOCAL_SERVER) == true
                    if (!isServerEnabled) {
                        println("Server feature is disabled. Not starting server.")
                        return@launch
                    }
                    ServerManager.startServer(this@WaterServerService)
                }
            }

            override fun onLost(network: Network) {
                super.onLost(network)
                println("Network lost. Stopping server...")
                scope.launch {
                    if (ServerManager.getLocalIpAddress().isFailure) {
                        ServerManager.stopServer()
                        updateNotification()
                    }
                }
            }
        }

    override fun onCreate() {
        super.onCreate()
        connectivityManager = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        registerNetworkCallback()
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        when (intent?.action) {
            ACTION_START -> {
                promoteToForeground()
                println("Foreground service started.")
                updateNotification()
            }
            ACTION_STOP -> {
                println("Stopping service via intent.")
                stopSelf()
            }
            // HANDLE THE NEW ACTION
            ACTION_UPDATE_NOTIFICATION -> {
                println("Received request to update notification.")
                updateNotification()
            }
        }

        return START_STICKY
    }

    private fun registerNetworkCallback() {
        val networkRequest =
            NetworkRequest
                .Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
                .build()
        connectivityManager.registerNetworkCallback(networkRequest, networkCallback)
    }

    private fun promoteToForeground() {
        val notification =
            createNotification(
                context = this,
                title = "PC Sync Service",
                text = "Initializing...",
            )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun updateNotification() {
        scope.launch {
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

            val dataStore = DataStoreSingleton.getInstance(this@WaterServerService)
            val data = dataStore.data.firstOrNull()
            val currentHydration = data?.get(DataStoreKeys.HYDRATION_LEVEL) ?: 0.0
            val goal = data?.get(DataStoreKeys.HYDRATION_GOAL) ?: 2.0

            val title = "Hydration: ${getVolumeString(currentHydration)} / ${UnitHelper.getVolumeStringWithUnit(goal)}"

            val content: String
            val isServerRunning = ServerManager.isRunning()
            if (isServerRunning) {
                val ipResult = ServerManager.getLocalIpAddress()
                content =
                    if (ipResult.isSuccess) {
                        "Server online at: ${ipResult.getOrNull()}"
                    } else {
                        "Server running, but IP not found."
                    }
            } else {
                content = "Server is offline. Waiting for network..."
            }

            // --- Build and Post Notification ---
            val notification = createNotification(this@WaterServerService, title, content)
            notificationManager.notify(NOTIFICATION_ID, notification)
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    override fun onDestroy() {
        super.onDestroy()
        println("Service is being destroyed.")

        connectivityManager.unregisterNetworkCallback(networkCallback)
        GlobalScope.launch {
            ServerManager.stopServer()
        }
        job.cancel()
        println("Service destroyed and job cancelled.")
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_START = "com.yukigasai.trinkaus.service.ACTION_START"
        const val ACTION_STOP = "com.yukigasai.trinkaus.service.ACTION_STOP"
        const val ACTION_UPDATE_NOTIFICATION = "com.yukigasai.trinkaus.service.ACTION_UPDATE_NOTIFICATION"
        private const val NOTIFICATION_ID = 53123
        private const val CHANNEL_ID = "WaterIntakeServiceChannel"

        fun triggerNotificationUpdate(context: Context) {
            val intent =
                Intent(context, WaterServerService::class.java).apply {
                    action = ACTION_UPDATE_NOTIFICATION
                }
            context.startService(intent)
        }

        private fun createNotificationChannel(context: Context) {
            val channel =
                NotificationChannel(
                    CHANNEL_ID,
                    "PC Sync Service",
                    NotificationManager.IMPORTANCE_LOW,
                )
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        private fun createNotification(
            context: Context,
            title: String,
            text: String,
        ): Notification {
            createNotificationChannel(context)

            val openAppIntent =
                Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
            val openAppPendingIntent: PendingIntent =
                PendingIntent.getActivity(
                    context,
                    33121,
                    openAppIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )

            val builder =
                NotificationCompat
                    .Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_launcher_foreground)
                    .setContentTitle(title)
                    .setContentText(text)
                    .setStyle(NotificationCompat.BigTextStyle().bigText(text))
                    .setPriority(NotificationCompat.PRIORITY_LOW)
                    .setOngoing(true)
                    .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
                    .setOnlyAlertOnce(true)
                    .setContentIntent(openAppPendingIntent)

            listOf(
                Constants.IntentAction.ADD_SMALL,
                Constants.IntentAction.ADD_MEDIUM,
                Constants.IntentAction.ADD_LARGE,
            ).forEachIndexed { index, action ->
                val option = HydrationOption.entries[index]

                val addIntent =
                    Intent(context, NotificationActionReceiver::class.java).apply {
                        this.action = action
                    }
                val addPendingIntent: PendingIntent =
                    PendingIntent.getBroadcast(
                        context,
                        100 + index,
                        addIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                    )

                builder.addAction(
                    option.icon,
                    option.getDisplayName(context),
                    addPendingIntent,
                )
            }

            return builder.build()
        }
    }
}
