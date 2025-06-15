package com.yukigasai.trinkaus.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.yukigasai.trinkaus.presentation.theme.TrinkAusTheme
import com.yukigasai.trinkaus.shared.DataStoreSingleton
import com.yukigasai.trinkaus.util.TrinkAusStateHolder

class MainActivity : ComponentActivity() {
    private lateinit var stateHolder: TrinkAusStateHolder

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Init a state holder to manage the hydration data
        val dataStore = DataStoreSingleton.getInstance(this)
        stateHolder = TrinkAusStateHolder(this, dataStore)

        setContent {
            TrinkAusTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.surface,
                ) {
                    MainScreen(stateHolder)
                }
            }
        }
    }
}
