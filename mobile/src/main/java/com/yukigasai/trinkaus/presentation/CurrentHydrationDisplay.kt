package com.yukigasai.trinkaus.presentation

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.yukigasai.trinkaus.shared.getVolumeString
import com.yukigasai.trinkaus.shared.getVolumeStringWithUnit
import kotlin.div

@Composable
fun CurrentHydrationDisplay(
    hydrationLevel: Double,
    hydrationGoal: Double,
) {
    val initialProgress = remember { mutableStateOf(0f) }
    val targetProgress = (hydrationLevel / hydrationGoal).coerceIn(0.0, 1.0).toFloat()

    val configuration = LocalConfiguration.current
    val circleSize = configuration.screenWidthDp.dp * 0.7f

    val animatedProgress =
        animateFloatAsState(
            targetValue = initialProgress.value,
            animationSpec =
                tween(
                    durationMillis = 1000,
                    easing = androidx.compose.animation.core.FastOutSlowInEasing,
                ),
        )

    LaunchedEffect(targetProgress) {
        initialProgress.value = targetProgress
    }
    Card(
        modifier = Modifier.size(circleSize),
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
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.primaryContainer,
                strokeWidth = 16.dp,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                strokeCap = StrokeCap.Round,
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = getVolumeString(hydrationLevel),
                    style =
                        MaterialTheme.typography.displayMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        ),
                )
                Text(
                    text = "/${getVolumeStringWithUnit(hydrationGoal)}",
                    style =
                        MaterialTheme.typography.headlineSmall.copy(
                            color = MaterialTheme.colorScheme.onSurface,
                        ),
                )
            }
        }
    }
}
