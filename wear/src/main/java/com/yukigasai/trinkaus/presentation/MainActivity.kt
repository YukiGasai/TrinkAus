/* While this template provides a good starting point for using Wear Compose, you can always
 * take a look at https://github.com/android/wear-os-samples/tree/main/ComposeStarter to find the
 * most up to date changes to the libraries and their usages.
 */

package com.yukigasai.trinkaus.presentation

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.core.content.ContextCompat.getSystemService
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.yukigasai.trinkaus.shared.Constants
import com.yukigasai.trinkaus.shared.SendMessageThread
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

fun vibrateDevice(context: Context) {
    val vibrator = getSystemService(context, Vibrator::class.java)
    vibrator?.vibrate(VibrationEffect.createOneShot(100, 100))
}

class MainActivity : ComponentActivity() {
    val hydrationLevel = mutableDoubleStateOf(0.0)
    val hydrationGoal = mutableDoubleStateOf(0.0)

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                SendMessageThread(
                    context = this@MainActivity,
                    path = Constants.Path.REQUEST_HYDRATION,
                ).start()
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()

        val messageFilter = IntentFilter(Constants.IntentAction.NEW_HYDRATION)
        val messageReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent == null) return
                val hydration = intent.getDoubleExtra(Constants.IntentKey.HYDRATION_DATA, 0.0)
                if (hydration != 0.0) {
                    hydrationLevel.doubleValue = hydration
                }

                val goal = intent.getDoubleExtra(Constants.IntentKey.HYDRATION_GOAL, 0.0)
                if (goal != 0.0) {
                    hydrationGoal.doubleValue = goal
                }
            }
        }

        registerReceiver(messageReceiver, messageFilter, RECEIVER_EXPORTED)
        SendMessageThread(
            context = this, path = Constants.Path.REQUEST_HYDRATION
        ).start()

        super.onCreate(savedInstanceState)
        setContent {
            HydrationMainScreen(hydrationLevel, hydrationGoal)
        }
    }
}
