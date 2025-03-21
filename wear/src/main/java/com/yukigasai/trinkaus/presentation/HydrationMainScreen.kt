package com.yukigasai.trinkaus.presentation

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableDoubleState
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
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.ui.tooling.preview.WearPreviewDevices
import com.yukigasai.trinkaus.R
import com.yukigasai.trinkaus.util.LocalStore
import com.yukigasai.trinkaus.util.SendMessageThread
import com.yukigasai.trinkaus.util.isMetric
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.floor

const val PROGRESS_BAR_GAP_SIZE = 28f

@Composable
fun HydrationMainScreen(hydrationLevel: MutableDoubleState, hydrationGoal: MutableDoubleState) {
    val context = LocalContext.current
    val focusRequester = remember { FocusRequester() }

    val hydrationLevel = remember { hydrationLevel }
    val goalHydration = remember { hydrationGoal }
    val tmpGoal = remember { mutableDoubleStateOf(0.0) }

    val saveJob = remember { mutableStateOf<Job?>(null) }

    val startAngle = 90 + PROGRESS_BAR_GAP_SIZE
    val endAngle = startAngle + 360 - PROGRESS_BAR_GAP_SIZE * 2

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        goalHydration.doubleValue = LocalStore.load(context, LocalStore.HYDRATION_GOAL_KEY)

        SendMessageThread(
            context = context,
            path = SendMessageThread.REQUEST_HYDRATION_PATH,
        ).start()
    }

    LaunchedEffect(goalHydration.doubleValue) {
        saveJob.value?.cancel()
        saveJob.value = launch {
            delay(1000)
            LocalStore.save(context, LocalStore.HYDRATION_GOAL_KEY, goalHydration.doubleValue)
            SendMessageThread(
                context = context,
                path = SendMessageThread.UPDATE_GOAL_PATH,
                msg = goalHydration.doubleValue
            ).start()
        }
    }

    val handleRotaryEvent = { event: RotaryScrollEvent ->
        if (tmpGoal.doubleValue == 0.0) {
            tmpGoal.doubleValue = goalHydration.doubleValue
        }

        if (isMetric()) {
            tmpGoal.doubleValue = tmpGoal.doubleValue + (event.verticalScrollPixels / 100)
            val oldGoal = goalHydration.doubleValue
            goalHydration.doubleValue = (floor(tmpGoal.doubleValue * 10) / 10).coerceIn(1.0, 10.0)
            if (oldGoal.toInt() != goalHydration.doubleValue.toInt()) {
                vibrateDevice(context)
            }
        } else {
            tmpGoal.doubleValue = tmpGoal.doubleValue + (event.verticalScrollPixels / 10)
            val oldGoal = goalHydration.doubleValue
            goalHydration.doubleValue = floor(tmpGoal.doubleValue).coerceIn(1.0, 200.0)
            if ((oldGoal / 10).toInt() != (goalHydration.doubleValue / 10).toInt()) {
                vibrateDevice(context)
            }
        }
        true
    }


    val HYDRATION_OPTIONS = listOf<HydrationOption>(
        HydrationOption(icon = R.drawable.glass_small_icon, amountUS = 5.0, amountMetric = 0.125),
        HydrationOption(icon = R.drawable.glas_icon, amountUS = 9.0, amountMetric = 0.25),
        HydrationOption(icon = R.drawable.bottle_icon, amountUS = 20.0, amountMetric = 0.5)
    )

    MaterialTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colors.background)
                .onRotaryScrollEvent(handleRotaryEvent)
                .focusRequester(focusRequester)
                .focusable(),
            contentAlignment = Alignment.Center
        ) {

            CircularProgressIndicator(
                progress = (hydrationLevel.doubleValue / goalHydration.doubleValue).toFloat(),
                strokeWidth = 8.dp,
                trackColor = MaterialTheme.colors.onBackground.copy(alpha = 0.2f),
                startAngle = startAngle,
                endAngle = endAngle,
                modifier = Modifier.fillMaxSize()
            )
            Column {
                HydrationInfo(hydrationLevel.doubleValue, goalHydration.doubleValue)
                Spacer(modifier = Modifier.size(16.dp))
                AddHydrationButton(HYDRATION_OPTIONS)
            }
        }
    }
}


@SuppressLint("UnrememberedMutableState")
@WearPreviewDevices
@Composable
fun PreviewWearApp() {
    HydrationMainScreen(mutableDoubleStateOf(1.0), mutableDoubleStateOf(3.0))
}