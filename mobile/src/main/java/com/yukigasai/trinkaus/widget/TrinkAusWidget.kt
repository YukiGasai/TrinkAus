package com.yukigasai.trinkaus.widget

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.unit.dp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.CircularProgressIndicator
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.yukigasai.trinkaus.R
import com.yukigasai.trinkaus.presentation.MainActivity
import com.yukigasai.trinkaus.shared.DataStoreSingleton
import com.yukigasai.trinkaus.shared.getVolumeString
import com.yukigasai.trinkaus.shared.getVolumeStringWithUnit
import com.yukigasai.trinkaus.util.TrinkAusStateHolder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min

class TrinkAusWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(
        context: Context,
        id: GlanceId,
    ) {
        val dataStore = DataStoreSingleton.getInstance(context)
        provideContent {
            val stateHolder = remember { TrinkAusStateHolder(context, dataStore) }
            GlanceTheme {
                GlanceContent(stateHolder)
            }
        }
    }

    @Composable
    fun string(id: Int): String {
        val context = LocalContext.current
        return context.getString(id)
    }

    @Composable
    fun GlanceContent(stateHolder: TrinkAusStateHolder) {
        val size = LocalSize.current
        val context = LocalContext.current
        val scope = rememberCoroutineScope()
        val hydrationLevel = stateHolder.hydrationLevel.collectAsState(initial = -1.0)
        val hydrationGoal = stateHolder.hydrationGoal.collectAsState(initial = 2.0)

        val goal = max(0.1, hydrationGoal.value)
        val currentLevel = hydrationLevel.value
        val progress = min(1f, max(0f, currentLevel.toFloat() / goal.toFloat()))
        val waterTargetHeight = size.height * progress

        LaunchedEffect(Unit) {
            stateHolder.refreshDataFromSource()
        }

        LaunchedEffect(size) {
            println("${size.height}  ${size.width}")
        }

        Box(
            contentAlignment = Alignment.BottomEnd,
            modifier =
                GlanceModifier
                    .fillMaxSize()
                    .background(GlanceTheme.colors.widgetBackground)
                    .clickable {
                        PendingIntent
                            .getActivity(
                                context,
                                0,
                                Intent(context, MainActivity::class.java),
                                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                            ).send()
                    },
        ) {
            if (hydrationLevel.value < 0) {
                Column(
                    modifier = GlanceModifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CircularProgressIndicator(
                        color = GlanceTheme.colors.primary,
                    )
                    Text(
                        maxLines = 1,
                        text = string(R.string.loading),
                        style =
                            TextStyle(
                                color = GlanceTheme.colors.primary,
                                fontSize = MaterialTheme.typography.bodyLarge.fontSize,
                                fontWeight = FontWeight.Medium,
                            ),
                    )
                }
            } else {
                Box(
                    contentAlignment = Alignment.BottomEnd,
                    modifier =
                        GlanceModifier
                            .fillMaxSize()
                            .background(GlanceTheme.colors.widgetBackground)
                            .clickable {
                                PendingIntent
                                    .getActivity(
                                        context,
                                        0,
                                        Intent(context, MainActivity::class.java),
                                        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                                    ).send()
                            },
                ) {
                    if (hydrationLevel.value < 0) {
                        Column(
                            modifier = GlanceModifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            CircularProgressIndicator(
                                color = GlanceTheme.colors.primary,
                            )
                            Text(
                                maxLines = 1,
                                text = string(R.string.loading),
                                style =
                                    TextStyle(
                                        color = GlanceTheme.colors.primary,
                                        fontSize = MaterialTheme.typography.bodyLarge.fontSize,
                                        fontWeight = FontWeight.Medium,
                                    ),
                            )
                        }
                    } else {
                        Box(
                            modifier =
                                GlanceModifier
                                    .fillMaxWidth()
                                    .height(waterTargetHeight),
                        ) {
                            Box(
                                modifier =
                                    GlanceModifier
                                        .fillMaxSize()
                                        .background(GlanceTheme.colors.secondaryContainer),
                            ) {}

                            if (waterTargetHeight > 0.dp) {
                                Image(
                                    provider = ImageProvider(R.drawable.wave),
                                    contentDescription = "Water wave decoration",
                                    colorFilter = ColorFilter.tint(GlanceTheme.colors.widgetBackground),
                                    contentScale = ContentScale.FillBounds,
                                    modifier =
                                        GlanceModifier
                                            .fillMaxWidth()
                                            .height(16.dp),
                                )
                            }
                        }
                    }
                }

                Column(
                    modifier = GlanceModifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (size.height > 110.dp && size.width > 90.dp) {
                        Text(
                            maxLines = 1,
                            text = "Hydration",
                            style =
                                TextStyle(
                                    color = GlanceTheme.colors.onPrimaryContainer,
                                    fontSize = MaterialTheme.typography.headlineSmall.fontSize,
                                    fontWeight = FontWeight.Bold,
                                ),
                        )
                    }
                    Spacer(modifier = GlanceModifier.height(8.dp))
                    Text(
                        text =
                            if (size.width > 90.dp) {
                                "${getVolumeString(currentLevel)} / ${getVolumeStringWithUnit(goal)}"
                            } else {
                                getVolumeStringWithUnit(currentLevel)
                            },
                        style =
                            TextStyle(
                                fontSize = MaterialTheme.typography.bodyLarge.fontSize,
                                color = GlanceTheme.colors.onPrimaryContainer,
                                fontWeight = FontWeight.Bold,
                            ),
                    )
                    if (size.height > 110.dp && size.width < 90.dp) {
                        Text(
                            text = "/ ${getVolumeStringWithUnit(goal)}",
                            style =
                                TextStyle(
                                    fontSize = MaterialTheme.typography.bodyLarge.fontSize,
                                    color = GlanceTheme.colors.onPrimaryContainer,
                                    fontWeight = FontWeight.Medium,
                                ),
                        )
                    }

                    if (size.height > 110.dp) {
                        Spacer(modifier = GlanceModifier.height(8.dp))
                        if (currentLevel >= goal) {
                            Text(
                                maxLines = 1,
                                text = string(R.string.done),
                                style =
                                    TextStyle(
                                        color = GlanceTheme.colors.onPrimaryContainer,
                                        fontSize = MaterialTheme.typography.bodyMedium.fontSize,
                                        fontWeight = FontWeight.Normal,
                                    ),
                            )
                        } else {
                            Text(
                                maxLines = 1,
                                text = "${(progress * 100).toInt()}%",
                                style =
                                    TextStyle(
                                        color = GlanceTheme.colors.onPrimaryContainer,
                                        fontSize = MaterialTheme.typography.bodyMedium.fontSize,
                                        fontWeight = FontWeight.Normal,
                                    ),
                            )
                        }
                    }
                    Spacer(modifier = GlanceModifier.height(8.dp))
                    ButtonList(stateHolder)
                }
                Image(
                    provider = ImageProvider(R.drawable.baseline_refresh_24),
                    contentDescription = "Refresh",
                    colorFilter = ColorFilter.tint(GlanceTheme.colors.primary),
                    modifier =
                        GlanceModifier.padding(4.dp).clickable {
                            scope.launch(Dispatchers.IO) {
                                TrinkAusWidget().updateAll(context)
                            }
                        },
                )
            }
        }
    }
}
