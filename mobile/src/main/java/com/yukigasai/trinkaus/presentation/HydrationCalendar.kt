package com.yukigasai.trinkaus.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.yukigasai.trinkaus.util.HeatmapCalendar
import com.yukigasai.trinkaus.util.HydrationHelper
import com.yukigasai.trinkaus.util.TrinkAusStateHolder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun HydrationCalendar(stateHolder: TrinkAusStateHolder) {
    val isLoadingHistory = remember { mutableStateOf(false) }
    val selectedDate = remember { stateHolder.selectedHistoryDate }
    val maxValue = remember { mutableDoubleStateOf(0.0) }
    val historyData = remember { mutableStateOf<Map<LocalDate, Double>>(emptyMap()) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var historyJob: Job? = null

    LaunchedEffect(selectedDate.value) {
        historyJob?.cancel()
        historyJob =
            scope.launch(Dispatchers.IO) {
                isLoadingHistory.value = true
                try {
                    val newHydrationData =
                        HydrationHelper.getHydrationHistoryForMonth(context, selectedDate.value)
                    maxValue.doubleValue = (newHydrationData.values.maxOrNull() ?: 0.0) + 1
                    historyData.value = newHydrationData
                } finally {
                    isLoadingHistory.value = false
                }
            }
    }

    HeatmapCalendar(
        yearMonth =
            YearMonth.of(
                selectedDate.value.year,
                selectedDate.value.monthValue,
            ),
        data = historyData.value,
        minValue = 0.0,
        maxValue = maxValue.doubleValue,
        isLoading = isLoadingHistory.value,
        onDayClick = { date, _ ->
            // If the selected date is in the future, reset to today
            if (date.isAfter(LocalDate.now())) {
                stateHolder.updateSelectedDate(LocalDate.now())
            } else {
                stateHolder.updateSelectedDate(date)
            }
        },
    )
}
