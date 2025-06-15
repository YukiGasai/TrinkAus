package com.yukigasai.trinkaus.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.rotary.RotaryScrollEvent
import androidx.compose.ui.input.rotary.onRotaryScrollEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.MaterialTheme
import com.yukigasai.trinkaus.shared.HydrationOption
import com.yukigasai.trinkaus.shared.UnitHelper
import com.yukigasai.trinkaus.util.TrinkAusStateHolder
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.floor

const val PROGRESS_BAR_GAP_SIZE = 28f

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HydrationMainScreen(
    stateHolder: TrinkAusStateHolder,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    val hydrationLevel = stateHolder.hydrationLevel.collectAsState(0.0)
    val hydrationGoal = stateHolder.hydrationGoal.collectAsState(2.0)

    val focusRequester = remember { FocusRequester() }
    val tmpGoal = remember { mutableDoubleStateOf(0.0) }
    val saveJob = remember { mutableStateOf<Job?>(null) }

    val startAngle = 90 + PROGRESS_BAR_GAP_SIZE
    val endAngle = startAngle + 360 - PROGRESS_BAR_GAP_SIZE * 2

    val isLoading = remember { mutableStateOf(false) }
    val listState = rememberScalingLazyListState()

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        stateHolder.refreshDataFromSource()
    }

    LaunchedEffect(isLoading.value) {
        if (isLoading.value) {
            delay(1000)
            isLoading.value = false
        }
    }

    LaunchedEffect(hydrationGoal.value) {
        saveJob.value?.cancel()
        saveJob.value =
            launch {
                delay(1000)
                stateHolder.updateGoal(hydrationGoal.value)
            }
    }

    val handleRotaryEvent = { event: RotaryScrollEvent ->
        if (tmpGoal.doubleValue == 0.0) {
            tmpGoal.doubleValue = hydrationGoal.value
        }

        if (UnitHelper.isMetric()) {
            tmpGoal.doubleValue = tmpGoal.doubleValue + (event.verticalScrollPixels / 100)
            val oldGoal = hydrationGoal.value
            val newValue = (floor(tmpGoal.doubleValue * 10) / 10).coerceIn(1.0, 10.0)
            stateHolder.updateGoal(newValue)
            if (oldGoal.toInt() != hydrationGoal.value.toInt()) {
                vibrateDevice(context)
            }
        } else {
            tmpGoal.doubleValue = tmpGoal.doubleValue + (event.verticalScrollPixels / 10)
            val oldGoal = hydrationGoal.value
            val newValue = floor(tmpGoal.doubleValue).coerceIn(1.0, 200.0)
            stateHolder.updateGoal(newValue)
            if ((oldGoal / 10).toInt() != (hydrationGoal.value / 10).toInt()) {
                vibrateDevice(context)
            }
        }
        true
    }

    MaterialTheme {
        PullToRefreshBox(
            isRefreshing = isLoading.value,
            onRefresh = {
                isLoading.value = true
                stateHolder.refreshDataFromSource()
            },
            modifier = modifier.fillMaxSize(),
        ) {
            ScalingLazyColumn(
                state = listState,
                modifier =
                    Modifier
                        .fillMaxSize()
                        .onRotaryScrollEvent {
                            true
                        }.focusRequester(focusRequester)
                        .focusable(),
            ) {
                item {
                    Box(
                        modifier =
                            Modifier
                                .fillParentMaxSize()
                                .background(MaterialTheme.colors.background),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(
                            progress = (hydrationLevel.value / hydrationGoal.value).toFloat(),
                            strokeWidth = 8.dp,
                            trackColor = MaterialTheme.colors.onBackground.copy(alpha = 0.2f),
                            startAngle = startAngle,
                            endAngle = endAngle,
                            modifier = Modifier.fillMaxSize(),
                        )
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            HydrationInfo(hydrationLevel.value, hydrationGoal.value)
                            Spacer(modifier = Modifier.size(16.dp))
                            AddHydrationButton(HydrationOption.entries)
                        }
                    }
                }
            }
        }
    }
}

// @SuppressLint("UnrememberedMutableState")
// @WearPreviewDevices
// @Composable
// fun PreviewWearApp() {
//    HydrationMainScreen()
// }
