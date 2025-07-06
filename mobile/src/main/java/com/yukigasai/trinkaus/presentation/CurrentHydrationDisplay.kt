package com.yukigasai.trinkaus.presentation

import android.annotation.SuppressLint
import android.content.Context
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.yukigasai.trinkaus.R
import com.yukigasai.trinkaus.shared.UnitHelper
import com.yukigasai.trinkaus.util.TrinkAusStateHolder
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToInt

@SuppressLint("ConfigurationScreenWidthHeight")
@Composable
fun CurrentHydrationDisplay(
    stateHolder: TrinkAusStateHolder,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val hydrationLevel = stateHolder.hydrationLevel.collectAsState(0.0)
    val hydrationGoal = stateHolder.hydrationGoal.collectAsState(0.1)
    val selectedDate = stateHolder.selectedDate

    val initialProgress = remember { mutableFloatStateOf(0f) }
    val targetProgress = (hydrationLevel.value / hydrationGoal.value).coerceIn(0.0, 1.0).toFloat()

    val configuration = LocalConfiguration.current
    val circleSize = configuration.screenWidthDp.dp * 0.7f
    val today = remember { LocalDate.now() }

    // State to track the horizontal drag offset
    var totalDragOffset by remember { mutableFloatStateOf(0f) }
    // Define a threshold for the swipe gesture (e.g., 1/4 of the screen width)
    val swipeThreshold = with(LocalDensity.current) { (configuration.screenWidthDp.dp / 4).toPx() }

    val context = LocalContext.current

    val animatedProgress =
        animateFloatAsState(
            targetValue = initialProgress.floatValue,
            animationSpec =
                tween(
                    durationMillis = 1000,
                    easing = androidx.compose.animation.core.FastOutSlowInEasing,
                ),
            label = "progressAnimation",
        )

    fun formatDate(
        context: Context,
        date: LocalDate,
        today: LocalDate,
    ): String =
        when (date) {
            today -> context.getString(R.string.today)
            today.minusDays(1) -> context.getString(R.string.yesterday)
            else ->
                if (date.isAfter(today.minusDays(7)) && date.isBefore(today)) {
                    date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())
                } else {
                    val formatter =
                        DateTimeFormatter
                            .ofLocalizedDate(FormatStyle.MEDIUM)
                            .withLocale(Locale.getDefault())
                    date.format(formatter)
                }
        }

    LaunchedEffect(targetProgress) {
        initialProgress.floatValue = targetProgress
    }

    Card(
        modifier =
            modifier
                .size(circleSize)
                // Apply the drag offset to visually move the card while dragging
                .offset { IntOffset(totalDragOffset.roundToInt(), 0) }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragStart = {
                            // Reset offset when a new drag starts
                            totalDragOffset = 0f
                        },
                        onHorizontalDrag = { change, dragAmount ->
                            // Prevent parent composables from consuming the gesture
                            change.consume()
                            // Accumulate the horizontal drag amount
                            totalDragOffset += dragAmount
                        },
                        onDragEnd = {
                            // When the drag ends, check if it crossed the threshold
                            if (totalDragOffset > swipeThreshold) {
                                stateHolder.updateSelectedDate(selectedDate.value.minusDays(1))
                            } else if (totalDragOffset < -swipeThreshold && selectedDate.value != today) {
                                stateHolder.updateSelectedDate(selectedDate.value.plusDays(1))
                            }
                            // Reset the offset after the drag ends
                            totalDragOffset = 0f
                        },
                    )
                },
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
        shape = CircleShape,
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize(),
        ) {
            CircularProgressIndicator(
                progress = { animatedProgress.value },
                modifier = Modifier.fillMaxSize().clickable { onClick() },
                color = MaterialTheme.colorScheme.primaryContainer,
                strokeWidth = 16.dp,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                strokeCap = StrokeCap.Round,
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text =
                        formatDate(
                            context = context,
                            date = selectedDate.value,
                            today = today,
                        ),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = UnitHelper.getVolumeString(hydrationLevel.value),
                    style =
                        MaterialTheme.typography.displayMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        ),
                )
                Text(
                    text = "/${UnitHelper.getVolumeStringWithUnit(hydrationGoal.value)}",
                    style =
                        MaterialTheme.typography.headlineSmall.copy(
                            color = MaterialTheme.colorScheme.onSurface,
                        ),
                )
            }
        }
    }
}
