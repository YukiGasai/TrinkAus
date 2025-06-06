package com.yukigasai.trinkaus.presentation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.yukigasai.trinkaus.shared.getVolumeString
import com.yukigasai.trinkaus.shared.getVolumeStringWithUnit

@Composable
fun HydrationInfo(
    hydrationLevel: Double,
    goalHydration: Double,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
    ) {
        Text(
            text = getVolumeString(hydrationLevel),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
            style =
                TextStyle(
                    color = MaterialTheme.colors.primary,
                    fontSize = 42.sp,
                ),
        )
        Text(
            text = "/ ${getVolumeStringWithUnit(goalHydration)}",
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
            style =
                TextStyle(
                    color = MaterialTheme.colors.onBackground,
                    fontSize = MaterialTheme.typography.caption1.fontSize,
                ),
        )
    }
}
