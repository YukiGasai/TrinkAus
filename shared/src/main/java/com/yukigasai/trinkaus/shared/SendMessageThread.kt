package com.yukigasai.trinkaus.shared

import android.content.Context
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.Wearable

class SendMessageThread(
    private val context: Context,
    private val path: String,
    private val msg: Any = "",
) : Thread() {
    override fun run() {
        println("Sending message: $path : $msg")
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
}
