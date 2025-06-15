package com.yukigasai.trinkaus.widget

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
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
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
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
import androidx.glance.unit.ColorProvider
import com.yukigasai.trinkaus.R
import com.yukigasai.trinkaus.presentation.MainActivity
import com.yukigasai.trinkaus.presentation.theme.WidgetTheme
import com.yukigasai.trinkaus.shared.DataStoreSingleton
import com.yukigasai.trinkaus.shared.UnitHelper
import com.yukigasai.trinkaus.util.TrinkAusStateHolder
import com.yukigasai.trinkaus.widget.actions.RefreshActionCallback
import kotlin.math.max
import kotlin.math.min

fun getRandomWave(): ImageProvider {
    val waves =
        listOf(
            R.drawable.wave1,
            R.drawable.wave2,
            R.drawable.wave3,
            R.drawable.wave4,
            R.drawable.wave5,
            R.drawable.wave6,
            R.drawable.wave7,
            R.drawable.wave8,
            R.drawable.wave9,
        )
    return ImageProvider(waves.random())
}

@Composable
fun GlanceModifier.widgetBackground(color: ColorProvider): GlanceModifier =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        this
            .background(color)
            .cornerRadius(16.dp)
    } else {
        // Fallback path for older devices using a tinted drawable
        this.background(
            ImageProvider(R.drawable.rounded_background),
            colorFilter = ColorFilter.tint(color),
        )
    }

class TrinkAusWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(
        context: Context,
        id: GlanceId,
    ) {
        val dataStore = DataStoreSingleton.getInstance(context)
        provideContent {
            val stateHolder = remember { TrinkAusStateHolder(context, dataStore) }
            WidgetTheme {
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
    fun GlanceContent(
        stateHolder: TrinkAusStateHolder,
        modifier: GlanceModifier = GlanceModifier,
    ) {
        val size = LocalSize.current
        val context = LocalContext.current
        val hydrationLevel = stateHolder.hydrationLevel.collectAsState(initial = -1.0)
        val hydrationGoal = stateHolder.hydrationGoal.collectAsState(initial = 2.0)

        val goal = max(0.1, hydrationGoal.value)
        val currentLevel = hydrationLevel.value
        val progress = min(1f, max(0f, currentLevel.toFloat() / goal.toFloat()))
        val waterTargetHeight = size.height * progress

        LaunchedEffect(Unit) {
            stateHolder.refreshDataFromSource()
        }

        Box(
            contentAlignment = Alignment.BottomEnd,
            modifier =
                modifier
                    .fillMaxSize()
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
                    modifier = GlanceModifier.fillMaxSize().widgetBackground(GlanceTheme.colors.widgetBackground),
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
                            .widgetBackground(GlanceTheme.colors.widgetBackground)
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
                        val boxModifier =
                            if (currentLevel >= goal) {
                                GlanceModifier.fillMaxSize()
                            } else {
                                GlanceModifier
                                    .fillMaxWidth()
                                    .height(waterTargetHeight)
                            }

                        Box(
                            modifier = boxModifier,
                        ) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S || waterTargetHeight > 16.dp) {
                                Box(
                                    modifier =
                                        GlanceModifier
                                            .fillMaxSize()
                                            .widgetBackground(GlanceTheme.colors.secondaryContainer),
                                ) {}
                            }
                            if ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.S || waterTargetHeight > 8.dp) && currentLevel < goal) {
                                Image(
                                    provider = getRandomWave(),
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
                                "${UnitHelper.getVolumeString(currentLevel)} / ${UnitHelper.getVolumeStringWithUnit(goal)}"
                            } else {
                                UnitHelper.getVolumeStringWithUnit(currentLevel)
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
                            text = "/ ${UnitHelper.getVolumeStringWithUnit(goal)}",
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
                        GlanceModifier
                            .padding(8.dp)
                            .clickable(actionRunCallback<RefreshActionCallback>()),
//                            .clickable {
//                            scope.launch(Dispatchers.IO) {
//                                TrinkAusWidget().updateAll(context)
//                            }
//                        },
                )
            }
        }
    }
}
