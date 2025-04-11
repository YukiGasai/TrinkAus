package com.yukigasai.trinkaus.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.yukigasai.trinkaus.util.HydrationHelper
import com.yukigasai.trinkaus.R
import com.yukigasai.trinkaus.shared.getVolumeStringWithUnit
import com.yukigasai.trinkaus.shared.isMetric
import kotlinx.coroutines.launch

data class HydrationOption(val icon: Int, val amountUS: Double, val amountMetric: Double)

val HYDRATION_OPTIONS = listOf<HydrationOption>(
    HydrationOption(icon = R.drawable.glass_small_icon, amountUS = 5.0, amountMetric = 0.125),
    HydrationOption(icon = R.drawable.glass_icon, amountUS = 9.0, amountMetric = 0.25),
    HydrationOption(icon = R.drawable.bottle_icon, amountUS = 20.0, amountMetric = 0.5)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddHydrationButtons(updateHydrationLevel: (newValue: Double) -> Unit) {

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    Row(
        modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        HYDRATION_OPTIONS.forEach { option ->
            val volume = if (isMetric()) option.amountMetric else option.amountUS
            val description = "${context.getString(R.string.add)} ${getVolumeStringWithUnit(volume)} ${context.getString(R.string.of_water)}"
            val tooltipState = rememberTooltipState()
            TooltipBox(
                positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                tooltip = {
                    Text(
                        modifier = Modifier
                            .background(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = MaterialTheme.shapes.small
                            )
                            .padding(8.dp),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        text = description
                    )
                },
                state = tooltipState
            ) {
                Button(onClick = {
                    scope.launch {
                        HydrationHelper.writeHydrationLevel(
                            context = context, amount = volume
                        )
                        updateHydrationLevel(HydrationHelper.readHydrationLevel(context))
                    }
                }) {
                    Icon(
                        painter = painterResource(id = option.icon),
                        contentDescription = description
                    )
                }
            }
        }
    }
}