package com.yukigasai.trinkaus.util

import android.content.Context
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.Wearable



class SendMessageThread(
    private val context: Context,
    private val path: String,
    private val msg: Any = ""
) : Thread() {
    override fun run() {
        try {
            val messageClient = Wearable.getMessageClient(context)
            val wearableList = Wearable.getNodeClient(context).connectedNodes

            val nodes = Tasks.await(wearableList)

            for (node in nodes) {
                val task: Task<Int> = messageClient.sendMessage(node.id, path, msg.toString().toByteArray())
                Tasks.await(task)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    companion object {
        const val ADD_HYDRATION_PATH = "/add_hydration"
        const val REQUEST_HYDRATION_PATH = "/request_hydration"
        const val UPDATE_GOAL_PATH = "/update_goal"
    }
}