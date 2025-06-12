package com.yukigasai.trinkaus.widget.actions

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.appwidget.action.ActionCallback
import com.yukigasai.trinkaus.widget.TrinkAusWidget

/**
 * Action to trigger a refresh.
 * Action callback is used to make sure the widget is updated using a pending intent.
 */
class RefreshActionCallback : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: androidx.glance.action.ActionParameters,
    ) {
        TrinkAusWidget().update(context, glanceId)
    }
}
