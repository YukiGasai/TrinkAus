package com.yukigasai.trinkaus

import android.content.Intent
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.records.HydrationRecord
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.units.Volume
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneOffset


const val NEW_HYDRATION = "NEW_HYDRATION"
const val HYDRATION_DATA = "hydration_data"
const val HYDRATION_GOAL = "hydration_goal"

class CompanionMessageService : WearableListenerService() {

    private lateinit var healthConnectClient: HealthConnectClient

    override fun onCreate() {
        super.onCreate()
        healthConnectClient = HealthConnectClient.getOrCreate(this)
    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        when (messageEvent.path) {

            "/request_hydration" -> {
                print("Received request_hydration message")

                CoroutineScope(Dispatchers.IO).launch {
                    val hydrationLevel =
                        HydrationHelper.readHydrationLevel(this@CompanionMessageService)
                    SendMessageThread(
                        this@CompanionMessageService, "/update_hydration", hydrationLevel.toString()
                    ).start()
                }
            }

            "/update_goal" -> {
                println("Received update_goal message")

                val goal = messageEvent.data.toString(Charsets.UTF_8).toDouble()
                LocalStore.save(this, LocalStore.HYDRATION_GOAL_KEY, goal)

                val intent = Intent(NEW_HYDRATION).apply {
                    putExtra(HYDRATION_GOAL, goal)
                }
                LocalBroadcastManager.getInstance(this@CompanionMessageService)
                    .sendBroadcast(intent)
            }

            "/add_hydration" -> {
                println("Received add_hydration message")

                val hydration = messageEvent.data.toString(Charsets.UTF_8).toDouble()

                val hydrationRecord = HydrationRecord(
                    volume = Volume.liters(hydration),
                    startTime = Instant.now().minusSeconds(60),
                    endTime = Instant.now(),
                    startZoneOffset = ZoneOffset.systemDefault().rules.getOffset(Instant.now()),
                    endZoneOffset = ZoneOffset.systemDefault().rules.getOffset(Instant.now()),
                    metadata = Metadata.unknownRecordingMethodWithId("manual")
                )

                CoroutineScope(Dispatchers.IO).launch {
                    healthConnectClient.insertRecords(listOf(hydrationRecord))

                    val newHydration =
                        HydrationHelper.readHydrationLevel(this@CompanionMessageService)
                    val intent = Intent(NEW_HYDRATION).apply {
                        putExtra(HYDRATION_DATA, newHydration)
                    }

                    LocalBroadcastManager.getInstance(this@CompanionMessageService)
                        .sendBroadcast(intent)

                    SendMessageThread(
                        this@CompanionMessageService, "/update_hydration", newHydration.toString()
                    ).start()

                }
            }
            else -> super.onMessageReceived(messageEvent)
        }
    }
}