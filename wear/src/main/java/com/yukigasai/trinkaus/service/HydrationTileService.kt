package com.yukigasai.trinkaus.service

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.wear.protolayout.ActionBuilders
import androidx.wear.protolayout.ColorBuilders.argb
import androidx.wear.protolayout.DimensionBuilders
import androidx.wear.protolayout.DimensionBuilders.expand
import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.protolayout.ModifiersBuilders
import androidx.wear.protolayout.ResourceBuilders
import androidx.wear.protolayout.TimelineBuilders
import androidx.wear.protolayout.material.Button
import androidx.wear.protolayout.material.CircularProgressIndicator
import androidx.wear.protolayout.material.Colors
import androidx.wear.protolayout.material.Text
import androidx.wear.protolayout.material.Typography
import androidx.wear.tiles.EventBuilders
import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.TileBuilders
import com.google.android.horologist.annotations.ExperimentalHorologistApi
import com.google.android.horologist.tiles.SuspendingTileService
import com.google.android.horologist.tiles.images.drawableResToImageResource
import com.yukigasai.trinkaus.R
import com.yukigasai.trinkaus.presentation.MainActivity
import com.yukigasai.trinkaus.presentation.PROGRESS_BAR_GAP_SIZE
import com.yukigasai.trinkaus.presentation.dataStore
import com.yukigasai.trinkaus.shared.Constants
import com.yukigasai.trinkaus.shared.SendMessageThread
import com.yukigasai.trinkaus.shared.getVolumeString
import com.yukigasai.trinkaus.shared.getVolumeStringWithUnit
import com.yukigasai.trinkaus.shared.isMetric
import kotlinx.coroutines.flow.first

private const val RESOURCES_VERSION = "0"
private const val GLASS_ICON = "glass_icon"
private const val BOTTLE_ICON = "bottle_icon"
private const val ADD_WATER_025 = "add_water_025"
private const val ADD_WATER_05 = "add_water_05"

/**
 * Tile for displaying hydration level and adding water.
 */
@OptIn(ExperimentalHorologistApi::class)
class HydrationTileService : SuspendingTileService() {

    override suspend fun resourcesRequest(
        requestParams: RequestBuilders.ResourcesRequest
    ) = resources()

    override suspend fun tileRequest(
        requestParams: RequestBuilders.TileRequest
    ) = tile(requestParams, this)

    override fun onTileEnterEvent(requestParams: EventBuilders.TileEnterEvent) {
        super.onTileEnterEvent(requestParams)
        SendMessageThread(
            context = this, path = Constants.Path.REQUEST_HYDRATION, msg = ""
        ).start()
    }
}

private fun resources(): ResourceBuilders.Resources {
    return ResourceBuilders.Resources.Builder().setVersion(RESOURCES_VERSION).addIdToImageMapping(
            GLASS_ICON, drawableResToImageResource(R.drawable.glas_icon)
        ).addIdToImageMapping(
            BOTTLE_ICON, drawableResToImageResource(R.drawable.bottle_icon)
        ).build()
}

private suspend fun tile(
    requestParams: RequestBuilders.TileRequest,
    context: Context,
): TileBuilders.Tile {

    var currentHydration = context.dataStore.data.first()[Constants.DataStore.DataStoreKeys.HYDRATION_LEVEL] ?: 0.0
    var goalHydration = context.dataStore.data.first()[Constants.DataStore.DataStoreKeys.HYDRATION_GOAL] ?: 3.0

    if (requestParams.currentState.lastClickableId == ADD_WATER_025) {
        val addedWater = if (isMetric()) 0.25 else 9.0
        currentHydration += addedWater
        context.dataStore.edit { settings ->
            settings[Constants.DataStore.DataStoreKeys.HYDRATION_LEVEL] = currentHydration
        }
        SendMessageThread(
            context = context, path = Constants.Path.ADD_HYDRATION, msg = addedWater
        ).start()
    } else if (requestParams.currentState.lastClickableId == ADD_WATER_05) {
        val addedWater = if (isMetric()) 0.5 else 20.0
        currentHydration += addedWater
        context.dataStore.edit { settings ->
            settings[Constants.DataStore.DataStoreKeys.HYDRATION_LEVEL] = currentHydration
        }
        SendMessageThread(
            context = context, path = Constants.Path.ADD_HYDRATION, msg = addedWater
        ).start()
    }


    val singleTileTimeline = TimelineBuilders.Timeline.Builder().addTimelineEntry(
        TimelineBuilders.TimelineEntry.Builder().setLayout(
            LayoutElementBuilders.Layout.Builder()
                .setRoot(tileLayout(requestParams, context, currentHydration, goalHydration))
                .build()
        ).build()
    ).build()

    return TileBuilders.Tile.Builder().setResourcesVersion(RESOURCES_VERSION)
        .setTileTimeline(singleTileTimeline).build()
}

private fun tileLayout(
    requestParams: RequestBuilders.TileRequest,
    context: Context,
    currentHydration: Double = 0.0,
    goalHydration: Double = 3.0,
): LayoutElementBuilders.LayoutElement {

    val progress = (currentHydration / goalHydration).toFloat()

    val startAngle = 180 + PROGRESS_BAR_GAP_SIZE
    val endAngle = startAngle + 360 - PROGRESS_BAR_GAP_SIZE * 2

    return LayoutElementBuilders.Box.Builder().setModifiers(
            ModifiersBuilders.Modifiers.Builder().setClickable(
                    ModifiersBuilders.Clickable.Builder().setOnClick(
                            ActionBuilders.LaunchAction.Builder().setAndroidActivity(
                                    ActionBuilders.AndroidActivity.Builder()
                                        .setClassName(MainActivity::class.java.name)
                                        .setPackageName(context.packageName).build()
                                ).build()
                        ).setVisualFeedbackEnabled(true).build()
                ).build()
        ).setVerticalAlignment(LayoutElementBuilders.VERTICAL_ALIGN_CENTER)
        .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER).setWidth(expand())
        .setHeight(expand()).addContent(
            CircularProgressIndicator.Builder().setStartAngle(startAngle).setEndAngle(endAngle)
                .setProgress(progress).setStrokeWidth(8f).build()
        ).addContent(
            LayoutElementBuilders.Column.Builder()
                .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
                .setWidth(expand()).addContent(
                    Text.Builder(context, context.getString(R.string.water))
                        .setColor(argb(Colors.DEFAULT.onSurface))
                        .setTypography(Typography.TYPOGRAPHY_BODY1).build()
                ).addContent(
                    LayoutElementBuilders.Spacer.Builder().setHeight(DimensionBuilders.dp(8f))
                        .build()
                ).addContent(
                    Text.Builder(context, getVolumeString(currentHydration))
                        .setColor(argb(Colors.DEFAULT.primary)).build()
                ).addContent(
                    LayoutElementBuilders.Spacer.Builder().setHeight(DimensionBuilders.dp(8f))
                        .build()
                ).addContent(
                    if (currentHydration >= goalHydration) {
                        Text.Builder(context, context.getString(R.string.done))
                            .setColor(argb(Colors.DEFAULT.onSurface))
                            .setTypography(Typography.TYPOGRAPHY_CAPTION1).build()
                    } else {
                        Text.Builder(
                            context,
                            "${getVolumeStringWithUnit(goalHydration - currentHydration)} ${
                                context.getString(
                                    R.string.missing
                                )
                            }"
                        ).setColor(argb(Colors.DEFAULT.onSurface))
                            .setTypography(Typography.TYPOGRAPHY_CAPTION1).build()
                    }
                ).addContent(
                    LayoutElementBuilders.Spacer.Builder().setHeight(DimensionBuilders.dp(8f))
                        .build()
                ).addContent(
                    LayoutElementBuilders.Row.Builder().addContent(
                            Button.Builder(
                                context,
                                ModifiersBuilders.Clickable.Builder().setVisualFeedbackEnabled(true)
                                    .setOnClick(ActionBuilders.LoadAction.Builder().build())
                                    .setId(ADD_WATER_025).build()
                            ).setSize(DimensionBuilders.dp(40f)).setIconContent(GLASS_ICON).build()
                        ).addContent(
                            LayoutElementBuilders.Spacer.Builder()
                                .setWidth(DimensionBuilders.dp(8f)).build()
                        ).addContent(
                            Button.Builder(
                                context,
                                ModifiersBuilders.Clickable.Builder().setVisualFeedbackEnabled(true)
                                    .setOnClick(ActionBuilders.LoadAction.Builder().build())
                                    .setId(ADD_WATER_05).build()
                            ).setSize(DimensionBuilders.dp(40f)).setIconContent(BOTTLE_ICON).build()
                        ).build()
                ).build()
        ).build()
}

//@Preview(device = WearDevices.SMALL_ROUND)
//@Preview(device = WearDevices.LARGE_ROUND)
//fun tilePreview(context: Context) = TilePreviewData {
//    tile(it, context)
//}