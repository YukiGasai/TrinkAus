package com.yukigasai.trinkaus.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.ComponentName
import android.content.Context
import android.os.Build
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.compose
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 *  The GlanceAppWidgetReceiver for the TrinkAusWidget.
 *  This class a special BroadcastReceiver for handling the widget's lifecycle events.
 */
class TrinkAusWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TrinkAusWidget()

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        CoroutineScope(Dispatchers.IO).launch {
            glanceAppWidget.updateAll(context)

            // Setup the preview for the widget
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                appWidgetManager.setWidgetPreview(
                    ComponentName(
                        context,
                        this@TrinkAusWidgetReceiver::class.java,
                    ),
                    AppWidgetProviderInfo.WIDGET_CATEGORY_HOME_SCREEN,
                    TrinkAusWidget().compose(context),
                )
            }
        }
    }
}
