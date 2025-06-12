package com.yukigasai.trinkaus.widget.actions

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.updateAll
import com.yukigasai.trinkaus.shared.Constants
import com.yukigasai.trinkaus.shared.HydrationOption
import com.yukigasai.trinkaus.shared.WearableMessenger
import com.yukigasai.trinkaus.shared.getAmount
import com.yukigasai.trinkaus.util.HydrationHelper
import com.yukigasai.trinkaus.widget.TrinkAusWidget

val HydrationOptionKey = ActionParameters.Key<HydrationOption>("hydrationOptionKey")

class AddWaterActionCallback : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        val option = parameters[HydrationOptionKey] ?: HydrationOption.SMALL

        val amountToAdd = option.getAmount(context)
        HydrationHelper.writeHydrationLevel(context, amountToAdd)
        val hydration = HydrationHelper.readHydrationLevel(context)

        WearableMessenger.sendMessage(
            context,
            Constants.Path.UPDATE_HYDRATION,
            hydration,
        )

        TrinkAusWidget().updateAll(context)
    }
}
