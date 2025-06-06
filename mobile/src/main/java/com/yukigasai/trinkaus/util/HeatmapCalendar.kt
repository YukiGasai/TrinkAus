package com.yukigasai.trinkaus.util

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.yukigasai.trinkaus.R
import com.yukigasai.trinkaus.shared.getVolumeStringWithUnit
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlin.math.ceil

@Composable
fun HeatmapCalendar(
    yearMonth: YearMonth,
    data: Map<LocalDate, Double>,
    minValue: Double,
    maxValue: Double,
    emptyCellColor: Color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
    startColor: Color = MaterialTheme.colorScheme.primaryContainer,
    endColor: Color = MaterialTheme.colorScheme.primary,
    isLoading: Boolean = false,
) {
    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp.dp
    val cellDimension = remember(screenWidthDp) { screenWidthDp / 7 }

    val firstDayOfMonth = yearMonth.atDay(1)
    val daysInMonth = yearMonth.lengthOfMonth()
    val firstDayOfWeekOffset = (firstDayOfMonth.dayOfWeek.value - DayOfWeek.MONDAY.value + 7) % 7

    // Calculate grid height based on passed cellDimension and number of rows
    val totalCellsInGrid = firstDayOfWeekOffset + daysInMonth
    val numRows = remember(totalCellsInGrid) { ceil(totalCellsInGrid / 7.0).toInt() }
    val gridHeight = remember(cellDimension, numRows) { cellDimension * numRows }
    val selectedDate = remember { mutableStateOf<LocalDate?>(null) }

    if (isLoading) {
        Column(
            modifier = Modifier.fillMaxWidth().height(300.dp).padding(bottom = 10.dp),
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
        return
    }

    selectedDate.value?.let { date ->
        Dialog(
            properties =
                DialogProperties(
                    dismissOnBackPress = true,
                    dismissOnClickOutside = true,
                    usePlatformDefaultWidth = true,
                ),
            onDismissRequest = { selectedDate.value = null },
            content = {
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .background(
                                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                                shape = MaterialTheme.shapes.medium,
                            ).padding(16.dp),
                    horizontalAlignment = CenterHorizontally,
                ) {
                    Text(
                        text = date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL)),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = data[date]?.let { getVolumeStringWithUnit(it) } ?: "No data",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
            },
        )
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(7),
        userScrollEnabled = false,
        modifier =
            Modifier
                .fillMaxWidth()
                .height(gridHeight),
        // Used to allow padding in cells but keep the grid height consistent
        verticalArrangement = Arrangement.spacedBy((-2).dp),
    ) {
        items(
            count = firstDayOfWeekOffset,
            key = { index -> "spacer_$index" },
            contentType = { "emptyDayCell" },
        ) {
            Box(
                Modifier
                    .size(cellDimension)
                    .aspectRatio(1f)
                    .padding(2.dp)
                    .clip(MaterialTheme.shapes.extraSmall),
            )
        }

        items(
            count = daysInMonth,
            key = { dayIndex -> yearMonth.atDay(dayIndex + 1).toString() },
            contentType = { "dayCell" },
        ) { dayIndex ->
            val day = dayIndex + 1
            val currentDate = yearMonth.atDay(day)
            val value = data[currentDate]

            // Animation: Verzögerung basierend auf dem Index
            val isVisible = remember { mutableStateOf(false) }
            val animatedAlpha =
                animateFloatAsState(
                    targetValue = if (isVisible.value) 1f else 0f,
                    animationSpec =
                        tween(
                            durationMillis = 100,
                            easing = androidx.compose.animation.core.FastOutSlowInEasing, // Bezier-Easing
                        ),
                )
            val animatedSize =
                animateDpAsState(
                    targetValue = if (isVisible.value) cellDimension else 0.dp,
                    animationSpec =
                        tween(
                            durationMillis = 400,
                            easing = androidx.compose.animation.core.FastOutSlowInEasing, // Bezier-Easing
                        ),
                )

            LaunchedEffect(Unit) {
                kotlinx.coroutines.delay(dayIndex * 50L)
                isVisible.value = true
            }
            Box(
                modifier = Modifier.size(cellDimension),
                contentAlignment = Alignment.Center,
            ) {
                DayCell(
                    date = currentDate,
                    dayNumberText = day.toString(),
                    value = value,
                    minValue = minValue,
                    maxValue = maxValue,
                    emptyCellColor = emptyCellColor,
                    startColor = startColor,
                    endColor = endColor,
                    onDayClick = { date, value ->
                        selectedDate.value = date
                    },
                    modifier =
                        Modifier
                            .size(animatedSize.value)
                            .graphicsLayer(alpha = animatedAlpha.value),
                )
            }
        }
    }
}

@Composable
private fun DayCell(
    modifier: Modifier = Modifier,
    date: LocalDate,
    dayNumberText: String,
    value: Double?,
    minValue: Double,
    maxValue: Double,
    emptyCellColor: Color,
    startColor: Color,
    endColor: Color,
    onDayClick: (date: LocalDate, value: Double?) -> Unit,
) {
    val cellColor =
        when {
            value == null || value == 0.0 -> emptyCellColor
            else -> {
                val fraction = ((value - minValue) / (maxValue - minValue)).coerceIn(0.0, 1.0).toFloat()
                lerp(startColor, endColor, fraction)
            }
        }
    val cellTextColor =
        when {
            value == null -> MaterialTheme.colorScheme.onSurfaceVariant
            cellColor.luminance() < 0.4f -> Color.White
            else -> Color.Black.copy(alpha = 0.7f)
        }

    Box(
        modifier =
            modifier
                .aspectRatio(1f)
                .padding(2.dp)
                .clip(MaterialTheme.shapes.extraSmall)
                .background(cellColor)
                .clickable { onDayClick(date, value) },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = dayNumberText,
            fontSize = 9.sp,
            color = cellTextColor,
            fontWeight = FontWeight.Normal,
        )
    }
}

fun Color.luminance(): Double {
    val r = red
    val g = green
    val b = blue
    return (0.2126 * r + 0.7152 * g + 0.0722 * b)
}
