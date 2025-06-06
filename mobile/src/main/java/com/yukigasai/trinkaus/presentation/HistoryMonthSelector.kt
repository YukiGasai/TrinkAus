package com.yukigasai.trinkaus.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.yukigasai.trinkaus.R
import com.yukigasai.trinkaus.util.TrinkAusStateHolder
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryMonthSelector(
    stateHolder: TrinkAusStateHolder,
    modifier: Modifier = Modifier,
) {
    val selectedDate = remember { stateHolder.selectedDate }
    val showDatePickerDialog = remember { mutableStateOf(false) }
    val datePickerState =
        rememberDatePickerState(
            initialSelectedDateMillis =
                selectedDate.value
                    .toEpochDay() * 24 * 60 * 60 * 1000L,
            yearRange = 2010..selectedDate.value.year,
        )

    if (showDatePickerDialog.value) {
        DatePickerDialog(
            onDismissRequest = {
                showDatePickerDialog.value = false
            },
            confirmButton = {
                TextButton(onClick = {
                    showDatePickerDialog.value = false
                    datePickerState.selectedDateMillis?.let { millis ->
                        selectedDate.value =
                            Instant
                                .ofEpochMilli(millis)
                                .atZone(ZoneId.systemDefault())
                                .toLocalDate()
                    }
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDatePickerDialog.value = false
                }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        ) {
            DatePicker(
                showModeToggle = true,
                state = datePickerState,
            )
        }
    }

    Card(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            IconButton(onClick = {
                selectedDate.value =
                    selectedDate.value.minusMonths(1)
            }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = "Previous Month",
                    tint = MaterialTheme.colorScheme.primary,
                )
            }

            val monthName =
                selectedDate.value.month.getDisplayName(
                    java.time.format.TextStyle.FULL,
                    Locale.getDefault(),
                )
            Text(
                text = "$monthName ${selectedDate.value.year}",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier =
                    Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp)
                        .clickable {
                            showDatePickerDialog.value = true
                        },
            )

            IconButton(
                onClick = {
                    selectedDate.value =
                        selectedDate.value.plusMonths(1)
                },
                enabled = LocalDate.now() > selectedDate.value,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Next Month",
                    tint =
                        if (LocalDate.now() > selectedDate.value) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            Color.Transparent
                        },
                )
            }
        }
    }
}
