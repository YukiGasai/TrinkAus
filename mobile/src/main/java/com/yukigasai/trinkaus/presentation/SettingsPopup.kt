package com.yukigasai.trinkaus.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.yukigasai.trinkaus.shared.Constants
import com.yukigasai.trinkaus.shared.LocalStore
import com.yukigasai.trinkaus.shared.SendMessageThread
import com.yukigasai.trinkaus.shared.getVolumeStringWithUnit
import com.yukigasai.trinkaus.shared.isMetric
import kotlin.math.roundToInt

@Composable
fun SettingsPopup(
    hydrationGoal: Double,
    updateHydrationGoal: (Double) -> Unit,
    updateShowSettingsModal: (Boolean) -> Unit,
) {
    val context = LocalContext.current
    Dialog(
        properties = DialogProperties(
            dismissOnBackPress = true, dismissOnClickOutside = true
        ), onDismissRequest = {
            updateShowSettingsModal(false)
        }) {

        val tmpGoal = remember { mutableDoubleStateOf(hydrationGoal) }

        Column(
            modifier = Modifier.Companion
                .background(
                    MaterialTheme.colorScheme.surface, MaterialTheme.shapes.medium
                )
                .padding(16.dp),
            horizontalAlignment = Alignment.Companion.CenterHorizontally
        ) {
            Text(
                text = "Set Daily Water Intake Goal",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )

            Text(
                text = getVolumeStringWithUnit(tmpGoal.doubleValue),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary
            )

            Slider(
                value = tmpGoal.doubleValue.toFloat(),
                onValueChange = {
                    if (isMetric()) {
                        tmpGoal.doubleValue = (it * 10).roundToInt() / 10.0
                    } else {
                        tmpGoal.doubleValue = it.toInt().toDouble()
                    }
                },
                valueRange = if (isMetric()) 1f..10f else 1f..200.0f,
                modifier = Modifier.Companion.fillMaxWidth()
            )
            Button(
                onClick = {
                    updateHydrationGoal(tmpGoal.doubleValue)
                    LocalStore.save(
                        context,
                        Constants.Preferences.HYDRATION_GOAL_KEY,
                        tmpGoal.doubleValue
                    )
                    SendMessageThread(
                        context,
                        Constants.Path.UPDATE_GOAL,
                        tmpGoal.doubleValue.toString()
                    ).start()
                    updateShowSettingsModal(false)
                }) {
                Text("Save")
            }
        }
    }
}