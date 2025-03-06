package com.yukigasai.trinkaus.service

import android.content.Intent
import android.util.Log
import androidx.wear.tiles.TileService
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import com.yukigasai.trinkaus.util.LocalStore

const val NEW_HYDRATION = "NEW_HYDRATION"
const val HYDRATION_DATA = "hydration_data"
const val HYDRATION_GOAL = "hydration_goal"

class WatchMessageService : WearableListenerService() {
    override fun onMessageReceived(messageEvent: MessageEvent) {
        when (messageEvent.path) {

            "/update_goal" -> {
                Log.println(Log.INFO, "WatchMessageService", "Received update_goal message")

                val goal = messageEvent.data.toString(Charsets.UTF_8).toDouble()
                val intent = Intent(NEW_HYDRATION).apply {
                    putExtra(HYDRATION_GOAL, goal)
                }

                LocalStore.save(this, LocalStore.HYDRATION_GOAL_KEY, goal)
                TileService.getUpdater(this@WatchMessageService).requestUpdate(HydrationTileService::class.java)
                sendBroadcast(intent)
            }


            "/update_hydration" -> {
                Log.println(Log.INFO, "WatchMessageService", "Received update_hydration message")

                val hydration = messageEvent.data.toString(Charsets.UTF_8).toDouble()

                val intent = Intent(NEW_HYDRATION).apply {
                    putExtra(HYDRATION_DATA, hydration)
                }

                LocalStore.save(this@WatchMessageService, LocalStore.HYDRATION_KEY, hydration)
                TileService.getUpdater(this@WatchMessageService).requestUpdate(HydrationTileService::class.java)
                sendBroadcast(intent)
            }

            else -> super.onMessageReceived(messageEvent)
        }
    }
}