package com.yukigasai.trinkaus.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.yukigasai.trinkaus.R
import com.yukigasai.trinkaus.util.StreakResult
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun StreakItem(
    title: String,
    streak: StreakResult,
    modifier: Modifier = Modifier,
) {
    val cardColor =
        if (streak.isLoading) {
            MaterialTheme.colorScheme.surfaceContainerLow
        } else if (streak.length == 0) {
            MaterialTheme.colorScheme.errorContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh
        }

    Card(
        modifier = modifier,
        colors =
            CardDefaults.cardColors(
                containerColor = cardColor,
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (streak.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.padding(4.dp),
                )
            } else {
                Text(
                    text = streak.length.toString(),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                )
            }
            Text(
                text = streak.startDate?.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT)) ?: "-",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun StreakDisplay(
    largestStreak: StreakResult,
    currentStreak: StreakResult,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        Spacer(modifier = Modifier.weight(0.2f))
        StreakItem(
            modifier = Modifier.weight(1f),
            title = stringResource(R.string.longest_streak),
            streak = largestStreak,
        )
        Spacer(modifier = Modifier.weight(0.2f))
        StreakItem(
            modifier = Modifier.weight(1f),
            title = stringResource(R.string.current_streak),
            streak = currentStreak,
        )
        Spacer(modifier = Modifier.weight(0.2f))
    }
}
