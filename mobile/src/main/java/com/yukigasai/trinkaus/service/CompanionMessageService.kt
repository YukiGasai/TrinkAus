package com.yukigasai.trinkaus.service

import android.content.Intent
import androidx.health.connect.client.HealthConnectClient
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import com.yukigasai.trinkaus.shared.Constants
import com.yukigasai.trinkaus.util.HydrationHelper
import com.yukigasai.trinkaus.shared.LocalStore
import com.yukigasai.trinkaus.shared.SendMessageThread
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CompanionMessageService : WearableListenerService() {

    private lateinit var healthConnectClient: HealthConnectClient

    override fun onCreate() {
        super.onCreate()
        healthConnectClient = HealthConnectClient.getOrCreate(this)
    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        println("Received message: ${messageEvent.path}")
        when (messageEvent.path) {

            Constants.Path.REQUEST_HYDRATION -> {
                CoroutineScope(Dispatchers.IO).launch {
                    val hydrationLevel =
                        HydrationHelper.readHydrationLevel(this@CompanionMessageService)
                    SendMessageThread(
                        this@CompanionMessageService,
                        Constants.Path.UPDATE_HYDRATION,
                        hydrationLevel.toString()
                    ).start()
                }
            }

            Constants.Path.UPDATE_GOAL -> {
                val goal = messageEvent.data.toString(Charsets.UTF_8).toDouble()
                LocalStore.save(this, Constants.Preferences.HYDRATION_GOAL_KEY, goal)

                val intent = Intent(Constants.IntentAction.NEW_HYDRATION).apply {
                    putExtra(Constants.IntentKey.HYDRATION_GOAL, goal)
                }
                LocalBroadcastManager.getInstance(this@CompanionMessageService)
                    .sendBroadcast(intent)
            }


            Constants.Path.ADD_HYDRATION -> {
                val hydration = messageEvent.data.toString(Charsets.UTF_8).toDouble()

                CoroutineScope(Dispatchers.IO).launch {
                    HydrationHelper.writeHydrationLevel(
                        this@CompanionMessageService, hydration
                    )

                    val newHydration =
                        HydrationHelper.readHydrationLevel(this@CompanionMessageService)
                    val intent = Intent(Constants.IntentAction.NEW_HYDRATION).apply {
                        putExtra(Constants.IntentKey.HYDRATION_DATA, newHydration)
                    }

                    LocalBroadcastManager.getInstance(this@CompanionMessageService)
                        .sendBroadcast(intent)

                    SendMessageThread(
                        this@CompanionMessageService,
                        Constants.Path.UPDATE_HYDRATION,
                        newHydration.toString()
                    ).start()

                }
            }

            else -> super.onMessageReceived(messageEvent)
        }
    }
}