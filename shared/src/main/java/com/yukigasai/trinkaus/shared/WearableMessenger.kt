package com.yukigasai.trinkaus.shared

import android.content.Context
import android.util.Log
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

sealed class SendMessageResult {
    object Success : SendMessageResult()

    object NoNodesFound : SendMessageResult()

    object ApiNotAvailable : SendMessageResult()

    data class Error(
        val exception: Exception,
    ) : SendMessageResult()
}

object WearableMessenger {
    private const val TAG = "WearableMessenger"

    fun isWearableApiAvailable(context: Context): Boolean =
        try {
            Wearable.getNodeClient(context)
            true
        } catch (e: Exception) {
            Log.w(TAG, "Wearable API check failed. Assuming not available.", e)
            false
        }

    suspend fun sendMessage(
        context: Context,
        path: String,
        msg: Any = "",
    ): SendMessageResult {
        return withContext(Dispatchers.IO) {
            Log.d(TAG, "Attempting to send message: $path : $msg")
            try {
                val nodes = Wearable.getNodeClient(context).connectedNodes.await()

                if (nodes.isEmpty()) {
                    Log.w(TAG, "No connected Wear OS nodes found. Aborting message send.")
                    return@withContext SendMessageResult.NoNodesFound
                }

                val messageClient = Wearable.getMessageClient(context)
                nodes.forEach { node ->
                    val nodeId = node.id
                    Log.d(TAG, "Sending message to node: $nodeId")
                    messageClient
                        .sendMessage(
                            nodeId,
                            path,
                            msg.toString().toByteArray(),
                        ).await()
                }
                Log.d(TAG, "Message successfully sent to ${nodes.size} node(s).")
                SendMessageResult.Success
            } catch (e: ApiException) {
                // Error that the Phone does not have Wearable API available
                if (e.statusCode == 17) {
                    Log.e(TAG, "Wearable.API is not available on this device.", e)
                    return@withContext SendMessageResult.ApiNotAvailable
                }
                Log.e(TAG, "Failed to send message to wearable due to an API exception.", e)
                SendMessageResult.Error(e)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send message to wearable.", e)
                SendMessageResult.Error(e)
            }
        }
    }
}
