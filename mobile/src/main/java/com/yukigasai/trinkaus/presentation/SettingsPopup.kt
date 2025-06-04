package com.yukigasai.trinkaus.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.edit
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.yukigasai.trinkaus.R
import com.yukigasai.trinkaus.shared.Constants
import com.yukigasai.trinkaus.shared.Constants.DataStore.DataStoreKeys
import com.yukigasai.trinkaus.shared.SendMessageThread
import com.yukigasai.trinkaus.shared.getVolumeStringWithUnit
import com.yukigasai.trinkaus.shared.isMetric
import com.yukigasai.trinkaus.util.NotificationWorker
import com.yukigasai.trinkaus.util.TrinkAusStateHolder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsPopup(
    stateHolder: TrinkAusStateHolder,
    updateShowSettingsModal: (Boolean) -> Unit,
) {
    val hydrationGoal = stateHolder.hydrationGoal.collectAsState(initial = 2.0)
    val reminderEnabled = stateHolder.isReminderEnabled.collectAsState(false)
    val reminderDespiteGoal = stateHolder.reminderDespiteGoal.collectAsState(false)
    val reminderStartTime = stateHolder.startTime.collectAsState(0f)
    val reminderEndTime = stateHolder.endTime.collectAsState(24f)

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = { updateShowSettingsModal(false) },
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Text(
                text = stringResource(R.string.settings),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Water Intake Section
            Surface(
                shape = MaterialTheme.shapes.medium,
                tonalElevation = 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.daily_water_intake_goal),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = getVolumeStringWithUnit(hydrationGoal.value),
                        style = MaterialTheme.typography.displaySmall,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Slider(
                        value = hydrationGoal.value.toFloat(),
                        onValueChange = {
                            scope.launch(Dispatchers.IO) {
                                context.dataStore.edit { preferences ->
                                    preferences[DataStoreKeys.HYDRATION_GOAL] =
                                        if (isMetric()) (it * 10).roundToInt() / 10.0 else it.toInt()
                                            .toDouble()
                                }
                            }
                        },
                        onValueChangeFinished = {
                            scope.launch(Dispatchers.IO) {
                                SendMessageThread(
                                    context, Constants.Path.UPDATE_GOAL, hydrationGoal.value
                                ).start()
                            }
                        },
                        valueRange = if (isMetric()) 1f..10f else 1f..200.0f,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Reminder Section
            Surface(
                shape = MaterialTheme.shapes.medium,
                tonalElevation = 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Reminder Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(R.string.enable_reminders),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Switch(
                            checked = reminderEnabled.value,
                            onCheckedChange = { isChecked ->
                                scope.launch(Dispatchers.IO) {
                                    context.dataStore.edit { preferences ->
                                        preferences[DataStoreKeys.IS_REMINDER_ENABLED] = isChecked
                                    }
                                }
                            }
                        )
                    }

                    if (reminderEnabled.value) {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 8.dp),
                            color = MaterialTheme.colorScheme.outlineVariant
                        )

                        Text(
                            text = stringResource(R.string.reminder_time_range),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Text(
                            text = "${reminderStartTime.value.toInt()}:00 - ${reminderEndTime.value.toInt()}:00",
                            style = MaterialTheme.typography.displaySmall,
                            color = MaterialTheme.colorScheme.primary
                        )

                        RangeSlider(
                            value = reminderStartTime.value..reminderEndTime.value,
                            onValueChange = { values ->
                                scope.launch(Dispatchers.IO) {
                                    context.dataStore.edit { preferences ->
                                        preferences[DataStoreKeys.REMINDER_START_TIME] = values.start
                                        preferences[DataStoreKeys.REMINDER_END_TIME] = values.endInclusive
                                    }
                                }
                            },
                            valueRange = 0f..24f,
                            steps = 23,
                            modifier = Modifier.fillMaxWidth()
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 8.dp),
                            color = MaterialTheme.colorScheme.outlineVariant
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = stringResource(R.string.reminder_despite_goal),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Switch(
                                checked = reminderDespiteGoal.value,
                                onCheckedChange = { isChecked ->
                                    scope.launch(Dispatchers.IO) {
                                        context.dataStore.edit { preferences ->
                                            preferences[DataStoreKeys.REMINDER_DESPITE_GOAL] = isChecked
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }

            // Save Button
            TextButton(
                onClick = { updateShowSettingsModal(false) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 16.dp),
                colors = ButtonDefaults.textButtonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                shape = MaterialTheme.shapes.medium,
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                Text(
                    text = stringResource(R.string.save),
                    style = MaterialTheme.typography.labelLarge
                )
            }

            TextButton(
                onClick = {
                    val workRequest =
                        OneTimeWorkRequestBuilder<NotificationWorker>().build()
                    WorkManager.getInstance(context).enqueue(workRequest)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 16.dp),
                colors = ButtonDefaults.textButtonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                shape = MaterialTheme.shapes.medium,
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                Text(
                    text = stringResource(R.string.test_notification),
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}