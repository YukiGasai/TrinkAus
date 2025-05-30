package com.yukigasai.trinkaus.widget

import android.annotation.SuppressLint
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.glance.ColorFilter
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.action.clickable
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.unit.ColorProvider
import com.yukigasai.trinkaus.shared.isMetric
import com.yukigasai.trinkaus.util.HydrationOption
import com.yukigasai.trinkaus.util.TrinkAusStateHolder

@SuppressLint("RestrictedApi")
@Composable
fun ButtonList(
    stateHolder: TrinkAusStateHolder
) {
    val context = LocalContext.current
    Row(
        modifier = GlanceModifier.fillMaxWidth().padding(8.dp),
        horizontalAlignment = Alignment.Horizontal.CenterHorizontally,
        verticalAlignment = Alignment.Vertical.CenterVertically,
    ) {

        HydrationOption.all.forEachIndexed { index, option ->

            val amount = if (isMetric()) option.amountMetric else option.amountUS
            val backgroundColor = ColorProvider(
                color = GlanceTheme.colors.primaryContainer.getColor(context).copy(alpha = 1.0f)
            )

            Image(
                provider = ImageProvider(option.icon),
                contentDescription = amount.toString(),
                colorFilter = ColorFilter.tint(GlanceTheme.colors.onPrimaryContainer),
                modifier = GlanceModifier.padding(4.dp).size(48.dp).background(backgroundColor)
                    .cornerRadius(8.dp).clickable {
                        stateHolder.addHydration(amount)
                    })
            if (index < HydrationOption.all.size - 1) {
                Spacer(modifier = GlanceModifier.width(8.dp))
            }
        }
    }
}