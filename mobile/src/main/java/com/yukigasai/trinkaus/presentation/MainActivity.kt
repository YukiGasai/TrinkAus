package com.yukigasai.trinkaus.presentation

import android.Manifest
import android.app.ComponentCaller
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.health.connect.client.HealthConnectClient
import com.yukigasai.trinkaus.shared.DataStoreSingleton
import com.yukigasai.trinkaus.util.HydrationHelper
import com.yukigasai.trinkaus.util.TrinkAusStateHolder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private lateinit var stateHolder: TrinkAusStateHolder
    private lateinit var healthConnectClient: HealthConnectClient

    /**
     * Request permission to post notifications on Android 13 and above.
     */
    private fun getPermissionToPostNotifications() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS,
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    1,
                )
            }
        }
    }

    /**
     * Handle Intents from Google Assistant.
     */
    override fun onNewIntent(
        intent: Intent,
        caller: ComponentCaller,
    ) {
        super.onNewIntent(intent, caller)
        if (intent.action != null && intent.action.equals("android.intent.action.VIEW")) {
            CoroutineScope(Dispatchers.IO).launch {
                if (intent.hasCategory("android.intent.category.BROWSABLE")) {
                    val hydrationLevel = HydrationHelper.readHydrationLevel(this@MainActivity)
                    val response = Intent()
                    response.putExtra("data", hydrationLevel)
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

    /**
     * Refresh the Health Connect data if the app reopens.
     */
    override fun onResume() {
        super.onResume()
        CoroutineScope(Dispatchers.IO).launch {
            stateHolder.refreshDataFromSource()
        }
    }

    /**
     * Set up the activity and the UI.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Get the Health Connect client
        healthConnectClient = HealthConnectClient.Companion.getOrCreate(this)

        // Make sure the app has the notification permission
        getPermissionToPostNotifications()

        // Init a state holder to manage the hydration data
        val dataStore = DataStoreSingleton.getInstance(this)
        stateHolder = TrinkAusStateHolder(this, dataStore)

        setContent {
            AppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.surface,
                ) {
                    MainScreen(stateHolder, healthConnectClient)
                }
            }
        }
    }
}
