package com.yukigasai.trinkaus.presentation.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.edit
import com.yukigasai.trinkaus.R
import com.yukigasai.trinkaus.shared.Constants
import com.yukigasai.trinkaus.shared.DataStoreSingleton
import com.yukigasai.trinkaus.util.ReminderScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

val reminderIntervalOptions = listOf(15, 30, 45, 60, 90, 120, 180, 240, 360, 480, 720)

@Composable
fun ReminderIntervalSelector(
    reminderEnabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val dataStore = DataStoreSingleton.getInstance(context)

    val savedInterval by dataStore.data
        .map { it[Constants.DataStore.DataStoreKeys.REMINDER_INTERVAL] ?: 60 }
        .collectAsState(initial = 60)

    var sliderPosition by remember { mutableStateOf(savedInterval.toFloat()) }

    var showCustomInputDialog by remember { mutableStateOf(false) }

    LaunchedEffect(savedInterval) {
        sliderPosition = savedInterval.toFloat()
    }

    Column(
        modifier = modifier,
    ) {
        Text(
            text = formatMinutesToHoursAndMinutes(sliderPosition.roundToInt()),
            style = MaterialTheme.typography.displaySmall,
            color =
                if (reminderEnabled) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                },
            modifier =
                Modifier
                    .clickable(enabled = reminderEnabled) {
                        showCustomInputDialog = true
                    }.padding(8.dp),
        )

        // The Slider
        Slider(
            enabled = reminderEnabled,
            value = sliderPosition,
            onValueChange = { newValue ->
                sliderPosition = newValue
            },
            onValueChangeFinished = {
                val snappedValue =
                    reminderIntervalOptions
                        .minByOrNull { abs(it - sliderPosition) } ?: sliderPosition.toInt()

                sliderPosition = snappedValue.toFloat()

                scope.launch(Dispatchers.IO) {
                    dataStore.edit { preferences ->
                        preferences[Constants.DataStore.DataStoreKeys.REMINDER_INTERVAL] =
                            snappedValue
                    }
                    ReminderScheduler.startOrRescheduleReminders(context)
                }
            },
            valueRange = 0f..720f,
            steps = 48 - 1,
        )
    }

    if (showCustomInputDialog) {
        CustomIntervalDialog(
            initialValue = savedInterval,
            onDismiss = { showCustomInputDialog = false },
            onConfirm = { newMinutes ->
                showCustomInputDialog = false
                scope.launch(Dispatchers.IO) {
                    dataStore.edit { preferences ->
                        preferences[Constants.DataStore.DataStoreKeys.REMINDER_INTERVAL] = newMinutes
                    }
                    ReminderScheduler.startOrRescheduleReminders(context)
                }
            },
        )
    }
}

@Composable
fun CustomIntervalDialog(
    initialValue: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit,
) {
    var textValue by remember { mutableStateOf(initialValue.toString()) }
    val minAllowed = 5
    val maxAllowed = 1440 * 7 // 7 days in minutes

    val parsedValue = textValue.toIntOrNull()
    val isError = parsedValue == null || parsedValue !in minAllowed..maxAllowed

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.reminder_interval)) },
        text = {
            OutlinedTextField(
                value = textValue,
                onValueChange = { textValue = it },
                label = { Text(text = stringResource(R.string.minutes)) },
                singleLine = true,
                isError = isError,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    parsedValue?.let { onConfirm(it) }
                },
                enabled = !isError,
            ) {
                Text(stringResource(android.R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(android.R.string.cancel), textAlign = TextAlign.Center)
            }
        },
    )
}

@Composable
fun formatMinutesToHoursAndMinutes(minutes: Int): String {
    if (minutes < 60) {
        return "$minutes ${stringResource(R.string.minutes)}"
    }
    val hours = minutes / 60
    val remainingMinutes = minutes % 60
    var hoursStr = hours.toString()
    if (hours < 10) {
        hoursStr = "0$hoursStr"
    }
    var minutesStr = remainingMinutes.toString()
    if (remainingMinutes < 10) {
        minutesStr = "0$minutesStr"
    }
    return "$hoursStr:$minutesStr ${stringResource(R.string.hours)}"
}
