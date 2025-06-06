package com.yukigasai.trinkaus.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.yukigasai.trinkaus.shared.HydrationOption
import com.yukigasai.trinkaus.shared.getDisplayName

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddHydrationButtons(
    modifier: Modifier = Modifier,
    updateHydrationLevel: (hydrationOption: HydrationOption) -> Unit,
) {
    val context = LocalContext.current

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        HydrationOption.entries.forEach { option ->
            Button(
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.65f),
                    ),
                shape = MaterialTheme.shapes.small,
                onClick = {
                    updateHydrationLevel(option)
                },
            ) {
                Icon(
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.onPrimary,
                    painter = painterResource(option.icon),
                    contentDescription = option.getDisplayName(context),
                )
            }
        }
    }
}
