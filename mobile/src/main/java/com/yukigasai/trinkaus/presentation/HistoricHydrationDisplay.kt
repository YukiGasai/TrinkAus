package com.yukigasai.trinkaus.presentation

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.yukigasai.trinkaus.R
import com.yukigasai.trinkaus.shared.getVolumeStringWithUnit
import com.yukigasai.trinkaus.util.HydrationHelper
import com.yukigasai.trinkaus.util.TrinkAusStateHolder
import ir.ehsannarmani.compose_charts.ColumnChart
import ir.ehsannarmani.compose_charts.models.BarProperties
import ir.ehsannarmani.compose_charts.models.Bars
import ir.ehsannarmani.compose_charts.models.GridProperties
import ir.ehsannarmani.compose_charts.models.HorizontalIndicatorProperties
import ir.ehsannarmani.compose_charts.models.LabelHelperProperties
import ir.ehsannarmani.compose_charts.models.LabelProperties
import ir.ehsannarmani.compose_charts.models.PopupProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.time.LocalDate
import kotlin.collections.get

fun convertToGraphFormat(
    historyData: Map<LocalDate, Double>,
    color: Color,
): List<Bars> =
    historyData.map {
        Bars(
            label = it.key.dayOfMonth.toString(),
            values =
                listOf(
                    Bars.Data(
                        value = it.value,
                        color = SolidColor(color),
                    ),
                ),
        )
    }

@Composable
fun HistoricHydrationDisplay(
    stateHolder: TrinkAusStateHolder,
    modifier: Modifier = Modifier,
) {
    val isLoadingHistory = remember { mutableStateOf(false) }
    val graphData = remember { mutableStateListOf<Bars>() }
    val selectedDate = remember { stateHolder.selectedDate }
    val maxValue = remember { mutableDoubleStateOf(0.0) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var historyJob: Job? = null
    val primaryColor = MaterialTheme.colorScheme.primary

    LaunchedEffect(selectedDate.value) {
        historyJob?.cancel()
        historyJob =
            scope.launch(Dispatchers.IO) {
                isLoadingHistory.value = true
                try {
                    val newHydrationData =
                        HydrationHelper.getHydrationHistoryForMonth(context, selectedDate.value)
                    maxValue.doubleValue = (newHydrationData.values.maxOrNull() ?: 0.0) + 1
                    graphData.clear()
                    // Fetch hydration history for the selected month
                    graphData.addAll(
                        convertToGraphFormat(
                            newHydrationData,
                            primaryColor,
                        ),
                    )
                } finally {
                    isLoadingHistory.value = false
                }
            }
    }

    if (isLoadingHistory.value) {
        Column(
            modifier = modifier.fillMaxWidth().height(300.dp),
            horizontalAlignment = CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.loading),
                modifier = Modifier.padding(vertical = 32.dp),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    } else {
        if (graphData.isNotEmpty()) {
            ColumnChart(
                modifier = modifier.fillMaxWidth().height(300.dp).padding(bottom = 10.dp),
                maxValue = maxValue.doubleValue,
                labelProperties =
                    LabelProperties(
                        enabled = true,
                        rotation =
                            LabelProperties.Rotation(
                                degree = -90f,
                                padding = 10.dp,
                            ),
                        textStyle =
                            TextStyle(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = MaterialTheme.typography.labelSmall.fontSize,
                                fontWeight = FontWeight.Normal,
                            ),
                    ),
                popupProperties =
                    PopupProperties(
                        textStyle =
                            TextStyle(
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                fontSize = MaterialTheme.typography.bodyMedium.fontSize,
                                fontWeight = FontWeight.Bold,
                            ),
                        containerColor =
                            Color(
                                MaterialTheme.colorScheme.secondaryContainer.value,
                            ),
                        contentBuilder = { dataIndex, valueIndex, value ->
                            "${graphData[dataIndex].label}: ${getVolumeStringWithUnit(value)}"
                        },
                    ),
                indicatorProperties =
                    HorizontalIndicatorProperties(
                        textStyle =
                            TextStyle(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = MaterialTheme.typography.labelSmall.fontSize,
                                fontWeight = FontWeight.Normal,
                            ),
                    ),
                labelHelperProperties = LabelHelperProperties(enabled = false),
                gridProperties = GridProperties(enabled = false),
                barProperties =
                    BarProperties(
                        cornerRadius = Bars.Data.Radius.Circular(2.dp),
                        thickness = 8.dp,
                    ),
                data = graphData,
                animationSpec =
                    spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow,
                    ),
            )
        } else {
            Box(
                modifier = modifier.fillMaxWidth().height(300.dp).padding(bottom = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.no_history_data),
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
