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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.GlassWater
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Martini
import com.composables.icons.lucide.Milk
import com.yukigasai.trinkaus.shared.HydrationOption
import com.yukigasai.trinkaus.shared.getDisplayName

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddHydrationButtons(updateHydrationLevel: (hydrationOption: HydrationOption) -> Unit) {
    val context = LocalContext.current

    Row(
        modifier = Modifier.fillMaxWidth(),
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
                    imageVector = option.getLucidIcon(),
                    contentDescription = option.getDisplayName(context),
                )
            }
        }
    }
}

fun HydrationOption.getLucidIcon(): ImageVector =
    when (this) {
        HydrationOption.SMALL -> Lucide.Martini
        HydrationOption.MEDIUM -> Lucide.GlassWater
        HydrationOption.LARGE -> Lucide.Milk
    }
