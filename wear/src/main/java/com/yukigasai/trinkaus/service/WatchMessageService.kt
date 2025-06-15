package com.yukigasai.trinkaus.service

import androidx.datastore.preferences.core.edit
import androidx.wear.tiles.TileService
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import com.yukigasai.trinkaus.shared.Constants
import com.yukigasai.trinkaus.shared.DataStoreSingleton
import com.yukigasai.trinkaus.shared.UnitHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class WatchMessageService : WearableListenerService() {
    override fun onMessageReceived(messageEvent: MessageEvent) {
        println("Received message: ${messageEvent.path}")

        val dataStore = DataStoreSingleton.getInstance(this)

        when (messageEvent.path) {
            Constants.Path.UPDATE_GOAL -> {
                CoroutineScope(Dispatchers.IO).launch {
                    val goal = messageEvent.data.toString(Charsets.UTF_8).toDouble()

                    dataStore.edit { preferences ->
                        preferences[Constants.DataStore.DataStoreKeys.HYDRATION_GOAL] = goal
                    }
                    TileService
                        .getUpdater(this@WatchMessageService)
                        .requestUpdate(HydrationTileService::class.java)
                }
            }

            Constants.Path.UPDATE_HYDRATION -> {
                CoroutineScope(Dispatchers.IO).launch {
                    val hydration = messageEvent.data.toString(Charsets.UTF_8).toDouble()

                    dataStore.edit { preferences ->
                        preferences[Constants.DataStore.DataStoreKeys.HYDRATION_LEVEL] = hydration
                    }
                    TileService
                        .getUpdater(this@WatchMessageService)
                        .requestUpdate(HydrationTileService::class.java)
                }
            }

            Constants.Path.UPDATE_UNIT -> {
                CoroutineScope(Dispatchers.IO).launch {
                    val isMetric = messageEvent.data.toString(Charsets.UTF_8).toBoolean()

                    dataStore.edit { preferences ->
                        preferences[Constants.DataStore.DataStoreKeys.IS_METRIC] = isMetric
                    }
                    UnitHelper.setMetric(this@WatchMessageService, isMetric)
                    TileService
                        .getUpdater(this@WatchMessageService)
                        .requestUpdate(HydrationTileService::class.java)
                }
            }

            else -> super.onMessageReceived(messageEvent)
        }
    }
}
