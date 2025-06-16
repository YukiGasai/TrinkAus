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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.edit
import com.yukigasai.trinkaus.R
import com.yukigasai.trinkaus.shared.Constants
import com.yukigasai.trinkaus.shared.DataStoreSingleton
import com.yukigasai.trinkaus.shared.UnitHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@Composable
fun HydrationGoalSelector(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val dataStore = DataStoreSingleton.getInstance(context)
    val isMetric = UnitHelper.isMetric()

    val hydartionGoal by dataStore.data
        .map { it[Constants.DataStore.DataStoreKeys.HYDRATION_GOAL] ?: 2 }
        .collectAsState(initial = 2)

    var sliderPosition by remember { mutableFloatStateOf(hydartionGoal.toFloat()) }

    var showCustomInputDialog by remember { mutableStateOf(false) }

    LaunchedEffect(hydartionGoal) {
        sliderPosition = hydartionGoal.toFloat()
    }

    Column(
        modifier = modifier,
    ) {
        Text(
            text = UnitHelper.getVolumeStringWithUnit(sliderPosition.toDouble()),
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.primary,
            modifier =
                Modifier
                    .clickable {
                        showCustomInputDialog = true
                    }.padding(8.dp),
        )

        Slider(
            value = sliderPosition,
            onValueChange = {
                sliderPosition =
                    if (isMetric) {
                        (it * 10).toInt() / 10f
                    } else {
                        it.toInt().toFloat()
                    }
            },
            onValueChangeFinished = {
                scope.launch(Dispatchers.IO) {
                    dataStore.edit { preferences ->
                        preferences[Constants.DataStore.DataStoreKeys.HYDRATION_GOAL] = sliderPosition.toDouble()
                    }
                }
            },
            valueRange = if (UnitHelper.isMetric()) 0.5f..5f else 1f..150.0f,
        )
    }

    if (showCustomInputDialog) {
        CustomGoalDialog(
            initialValue =
                if (isMetric) {
                    (sliderPosition * 10).toInt() / 10.0
                } else {
                    sliderPosition.toInt().toDouble()
                },
            onDismiss = { showCustomInputDialog = false },
            onConfirm = {
                showCustomInputDialog = false
                scope.launch(Dispatchers.IO) {
                    dataStore.edit { preferences ->
                        preferences[Constants.DataStore.DataStoreKeys.HYDRATION_GOAL] = it
                    }
                }
            },
        )
    }
}

@Composable
fun CustomGoalDialog(
    initialValue: Double,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit,
) {
    var textValue by remember { mutableStateOf(initialValue.toString()) }
    val minAllowed = 0.5

    val parsedValue = textValue.toDoubleOrNull()
    val isError = parsedValue == null || parsedValue < minAllowed

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.daily_water_intake_goal)) },
        text = {
            OutlinedTextField(
                value = textValue,
                onValueChange = { textValue = it },
                label = { Text(UnitHelper.getUnit()) },
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
                Text(stringResource(android.R.string.cancel))
            }
        },
    )
}
