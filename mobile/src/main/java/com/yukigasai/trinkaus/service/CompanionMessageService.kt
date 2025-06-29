package com.yukigasai.trinkaus.service

import androidx.datastore.preferences.core.edit
import androidx.glance.appwidget.updateAll
import androidx.health.connect.client.HealthConnectClient
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import com.yukigasai.trinkaus.service.WaterServerService.Companion.triggerNotificationUpdate
import com.yukigasai.trinkaus.shared.Constants
import com.yukigasai.trinkaus.shared.Constants.DataStore.DataStoreKeys
import com.yukigasai.trinkaus.shared.DataStoreSingleton
import com.yukigasai.trinkaus.shared.HydrationOption
import com.yukigasai.trinkaus.shared.WearableMessenger
import com.yukigasai.trinkaus.shared.getAmount
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
        val dataStore = DataStoreSingleton.getInstance(this)

        println("Received message: ${messageEvent.path} : ${String(messageEvent.data)}")
        when (messageEvent.path) {
            Constants.Path.REQUEST_HYDRATION -> {
                CoroutineScope(Dispatchers.IO).launch {
                    val hydrationLevel =
                        HydrationHelper.readHydrationLevel(this@CompanionMessageService)
                    WearableMessenger.sendMessage(
                        this@CompanionMessageService,
                        Constants.Path.UPDATE_HYDRATION,
                        hydrationLevel.toString(),
                    )
                }
            }

            Constants.Path.UPDATE_GOAL -> {
                CoroutineScope(Dispatchers.IO).launch {
                    val goal = messageEvent.data.toString(Charsets.UTF_8).toDouble()
                    dataStore.edit { preferences ->
                        preferences[DataStoreKeys.HYDRATION_GOAL] = goal
                    }
                    TrinkAusWidget().updateAll(this@CompanionMessageService)
                }
            }

            Constants.Path.ADD_HYDRATION -> {
                val hydrationOptionId = messageEvent.data.toString(Charsets.UTF_8)

                CoroutineScope(Dispatchers.IO).launch {
                    val hydrationOption =
                        HydrationOption.entries.find { it.name == hydrationOptionId }
                            ?: return@launch

                    val newAmount = hydrationOption.getAmount(this@CompanionMessageService)

                    HydrationHelper.writeHydrationLevel(
                        this@CompanionMessageService,
                        newAmount,
                    )

                    val newHydration =
                        HydrationHelper.readHydrationLevel(this@CompanionMessageService)

                    WearableMessenger.sendMessage(
                        this@CompanionMessageService,
                        Constants.Path.UPDATE_HYDRATION,
                        newHydration.toString(),
                    )

                    triggerNotificationUpdate(this@CompanionMessageService)

                    TrinkAusWidget().updateAll(this@CompanionMessageService)
                }
            }

            else -> super.onMessageReceived(messageEvent)
        }
    }
}
