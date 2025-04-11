package com.yukigasai.trinkaus.presentation

import android.Manifest
import android.app.ComponentCaller
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.ui.Modifier
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.health.connect.client.HealthConnectClient
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.yukigasai.trinkaus.shared.Constants
import com.yukigasai.trinkaus.shared.LocalStore
import com.yukigasai.trinkaus.util.HydrationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private lateinit var healthConnectClient: HealthConnectClient

    private val hydrationLevel = mutableDoubleStateOf(0.0)
    private val hydrationGoal = mutableDoubleStateOf(0.0)

    private fun getPermissionToPostNotifications() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this, Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    1
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent, caller: ComponentCaller) {
        super.onNewIntent(intent, caller)
        if (intent.action != null && intent.action.equals("android.intent.action.VIEW")) {
            CoroutineScope(Dispatchers.IO).launch {
                if (intent.hasCategory("android.intent.category.BROWSABLE")) {
                    val hydrationLevel = HydrationHelper.readHydrationLevel(this@MainActivity)
                    val response = Intent()
                    response.putExtra(Constants.IntentKey.HYDRATION_DATA, hydrationLevel)
                    println("Hydration level: $hydrationLevel after intent received ${intent.action}")

                    // Set the result to be read by Google Assistant
                    setResult(RESULT_OK, response)

//                var ttsReady = false
//                // Optional: You can also speak it directly
//                val tts = TextToSpeech(this@MainActivity) { status ->
//                    if (status == TextToSpeech.SUCCESS) {
//                        ttsReady = true
//                    } else {
//                        println("TextToSpeech initialization failed")
//                    }
//                }

//                // Wait for TTS to be ready
//                while (!ttsReady) {
//                    Thread.sleep(100)
//                }
//
//                tts.speak(
//                    "Your hydration level is $hydrationLevel percent",
//                    TextToSpeech.QUEUE_FLUSH, null, null
//                )

                    finish()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        healthConnectClient = HealthConnectClient.Companion.getOrCreate(this)
        getPermissionToPostNotifications()

        val messageFilter = IntentFilter(Constants.IntentAction.NEW_HYDRATION)
        val messageReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                CoroutineScope(Dispatchers.IO).launch {
                    val hydration = intent.getDoubleExtra(Constants.IntentKey.HYDRATION_DATA, 0.0)
                    if (hydration != 0.0) {
                        hydrationLevel.doubleValue = hydration
                    }

                    val newHydrationGoal =
                        intent.getDoubleExtra(Constants.IntentKey.HYDRATION_GOAL, 0.0)
                    if (newHydrationGoal != 0.0) {
                        hydrationGoal.doubleValue = newHydrationGoal
                    }
                }
            }
        }

        LocalBroadcastManager.getInstance(this).registerReceiver(messageReceiver, messageFilter)

        hydrationGoal.doubleValue =
            LocalStore.load(this, Constants.Preferences.HYDRATION_GOAL_KEY).toDouble()

        setContent {
            AppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    MainScreen(hydrationLevel, hydrationGoal, healthConnectClient)
                }
            }
        }
    }
}