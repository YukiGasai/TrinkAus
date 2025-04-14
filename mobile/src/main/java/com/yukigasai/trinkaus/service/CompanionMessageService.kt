package com.yukigasai.trinkaus.service

import androidx.datastore.preferences.core.edit
import androidx.glance.appwidget.updateAll
import androidx.health.connect.client.HealthConnectClient
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import com.yukigasai.trinkaus.presentation.dataStore
import com.yukigasai.trinkaus.shared.Constants
import com.yukigasai.trinkaus.shared.Constants.DataStore.DataStoreKeys
import com.yukigasai.trinkaus.shared.SendMessageThread
import com.yukigasai.trinkaus.util.HydrationHelper
import com.yukigasai.trinkaus.widget.TrinkAusWidget
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
        println("Received message: ${messageEvent.path} : ${String(messageEvent.data)}")
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
                CoroutineScope(Dispatchers.IO).launch {
                    val goal = messageEvent.data.toString(Charsets.UTF_8).toDouble()
                    this@CompanionMessageService.dataStore.edit { preferences ->
                        preferences[DataStoreKeys.HYDRATION_GOAL] = goal
                    }
                    TrinkAusWidget().updateAll(this@CompanionMessageService)
                }
            }


            Constants.Path.ADD_HYDRATION -> {
                val hydration = messageEvent.data.toString(Charsets.UTF_8).toDouble()

                CoroutineScope(Dispatchers.IO).launch {
                    HydrationHelper.writeHydrationLevel(
                        this@CompanionMessageService, hydration
                    )

                    val newHydration =
                        HydrationHelper.readHydrationLevel(this@CompanionMessageService)

                    SendMessageThread(
                        this@CompanionMessageService,
                        Constants.Path.UPDATE_HYDRATION,
                        newHydration.toString()
                    ).start()

                    TrinkAusWidget().updateAll(this@CompanionMessageService)
                }
            }

            else -> super.onMessageReceived(messageEvent)
        }
    }
}