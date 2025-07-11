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
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.unit.ColorProvider
import com.yukigasai.trinkaus.R
import com.yukigasai.trinkaus.shared.HydrationOption
import com.yukigasai.trinkaus.shared.getDisplayName
import com.yukigasai.trinkaus.util.TrinkAusStateHolder
import com.yukigasai.trinkaus.widget.actions.AddWaterActionCallback
import com.yukigasai.trinkaus.widget.actions.HydrationOptionKey

@SuppressLint("RestrictedApi")
@Composable
fun ButtonList(
    stateHolder: TrinkAusStateHolder,
    modifier: GlanceModifier = GlanceModifier,
) {
    val context = LocalContext.current
    Row(
        modifier = modifier.fillMaxWidth().padding(8.dp),
        horizontalAlignment = Alignment.Horizontal.CenterHorizontally,
        verticalAlignment = Alignment.Vertical.CenterVertically,
    ) {
        HydrationOption.entries.forEachIndexed { index, option ->

            val backgroundColor =
                ColorProvider(
                    color =
                        GlanceTheme.colors.primaryContainer
                            .getColor(context)
                            .copy(alpha = 0f),
                )

            Box(
                contentAlignment = Alignment.Center,
                modifier =
                    GlanceModifier
                        .background(backgroundColor)
                        .size(48.dp)
                        .clickable(
                            actionRunCallback<AddWaterActionCallback>(
                                parameters =
                                    actionParametersOf(
                                        HydrationOptionKey to option,
                                    ),
                            ),
                        ),
//                        .clickable {
//                            stateHolder.addHydration(option)
//                        },
            ) {
                Image(
                    provider = ImageProvider(option.icon),
                    contentDescription = option.getDisplayName(context),
                    colorFilter = ColorFilter.tint(GlanceTheme.colors.onPrimaryContainer),
                    modifier = GlanceModifier.fillMaxSize().padding(8.dp),
                )
                Image(
                    provider = ImageProvider(R.drawable.square),
                    contentDescription = "Outline",
                    colorFilter = ColorFilter.tint(GlanceTheme.colors.onPrimaryContainer),
                    modifier = GlanceModifier.fillMaxSize(),
                )
            }

            if (index < HydrationOption.entries.size - 1) {
                Spacer(modifier = GlanceModifier.width(8.dp))
            }
        }
    }
}
