package com.yukigasai.trinkaus.util

import android.content.Context
import androidx.datastore.preferences.core.edit
import com.yukigasai.trinkaus.shared.Constants.DataStore.DataStoreKeys
import com.yukigasai.trinkaus.shared.DataStoreSingleton
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.install
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.netty.NettyApplicationEngine
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.response.respondText
import io.ktor.server.routing.RoutingCall
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import kotlinx.coroutines.flow.first
import java.net.Inet4Address
import java.net.NetworkInterface
import java.time.LocalDate

/**
 * Important TODO
 *  - Add error handling for network issues
 *  - Add SSL support for secure connections
 *      - Without this, everyone in the local network can find the authToken
 */
object ServerManager {
    private var ktorServer: EmbeddedServer<NettyApplicationEngine, NettyApplicationEngine.Configuration>? = null

    const val PORT = 8372

    fun startServer(context: Context) {
        if (ktorServer != null) return
        ktorServer =
            embeddedServer(Netty, port = PORT) {
                install(CORS) {
                    anyHost()
                    allowHeader(HttpHeaders.ContentType)
                    allowHeader(HttpHeaders.Authorization)
                    allowMethod(HttpMethod.Get)
                    allowMethod(HttpMethod.Post)
                    allowMethod(HttpMethod.Options)
                }
                routing {
                    get("/hydration") {
                        val result = validateRequest(context, call)
                        if (!result) return@get

                        call.respondText(
                            getHydration(context),
                            contentType = ContentType.Application.Json,
                        )
                    }
                    get("/goal") {
                        val result = validateRequest(context, call)
                        if (!result) return@get

                        call.respondText(
                            getGoal(context),
                            contentType = ContentType.Application.Json,
                        )
                    }
                    get("/history") {
                        val result = validateRequest(context, call)
                        if (!result) return@get

                        val dateInMonthRaw = call.request.queryParameters["date"]

                        val dateInMonth =
                            if (dateInMonthRaw != null) {
                                try {
                                    LocalDate.parse(dateInMonthRaw)
                                } catch (e: Exception) {
                                    call.respondText(
                                        "{\"status\": \"error\", \"message\": \"Invalid date format use YYYY/MM/DD\"}",
                                        contentType = ContentType.Application.Json,
                                    )
                                    return@get
                                }
                            } else {
                                LocalDate.now()
                            }

                        val response = getHistoryForDate(context, dateInMonth)
                        call.respondText(
                            response,
                            contentType = ContentType.Application.Json,
                        )
                    }
                    post("/hydration") {
                        val result = validateRequest(context, call)
                        if (!result) return@post

                        val hydration = call.request.queryParameters["hydration"]?.toIntOrNull()
                        if (hydration != null) {
                            // Save hydration level to DataStore
                            val response = addHydration(context, hydration)
                            call.respondText(
                                response,
                                contentType = ContentType.Application.Json,
                            )
                        } else {
                            call.respondText(
                                "{\"status\": \"error\", \"message\": \"Invalid hydration value\"}",
                                contentType = ContentType.Application.Json,
                            )
                        }
                    }
                    post("/goal") {
                        val result = validateRequest(context, call)
                        if (!result) return@post

                        val goal = call.request.queryParameters["goal"]?.toDoubleOrNull()
                        if (goal != null) {
                            val response = updateGoal(context, goal)
                            call.respondText(
                                response,
                                contentType = ContentType.Application.Json,
                            )
                        } else {
                            call.respondText(
                                "{\"status\": \"error\", \"message\": \"Invalid goal value\"}",
                                contentType = ContentType.Application.Json,
                            )
                        }
                    }
                }
            }.start(wait = false)

        println("Server started!")
    }

    fun stopServer() {
        ktorServer?.stop(1000, 2000)
        ktorServer = null
    }

    suspend fun isEnabled(context: Context): Boolean {
        val dataStore = DataStoreSingleton.getInstance(context)
        return dataStore.data.first()[DataStoreKeys.USE_LOCAL_SERVER] == true
    }

    fun getLocalIpAddress(): Result<String> {
        try {
            val interfaceList = NetworkInterface.getNetworkInterfaces()
            while (interfaceList.hasMoreElements()) {
                val intf = interfaceList.nextElement()
                val enumIpAddr = intf.inetAddresses
                while (enumIpAddr.hasMoreElements()) {
                    val inetAddress = enumIpAddr.nextElement()
                    if (!inetAddress.isLoopbackAddress &&
                        inetAddress is Inet4Address &&
                        intf.isUp &&
                        !intf.isVirtual &&
                        intf.supportsMulticast()
                    ) {
                        return Result.success(inetAddress.hostAddress)
                    }
                }
            }
        } catch (ex: Exception) {
            return Result.failure(ex)
        }
        return Result.failure(
            Exception("Failed to get local IP address"),
        )
    }

    fun getServerUrl(): Result<String> {
        val ip = getLocalIpAddress()
        return if (ip.isFailure) {
            Result.failure(
                Exception(ip.exceptionOrNull()?.message ?: "Failed to get local IP address"),
            )
        } else {
            Result.success("http://${ip.getOrThrow()}:$PORT")
        }
    }

    suspend fun createOrRefreshAuthToken(context: Context) {
        val dataStore = DataStoreSingleton.getInstance(context)
        val authToken = dataStore.data.first()[DataStoreKeys.AUTH_TOKEN]
        if (authToken.isNullOrEmpty()) {
            val newAuthToken =
                java.util.UUID
                    .randomUUID()
                    .toString()
                    .replace("-", "")
            dataStore.edit { preferences ->
                preferences[DataStoreKeys.AUTH_TOKEN] = newAuthToken
            }
        }
    }

    private suspend fun validateRequest(
        context: Context,
        call: RoutingCall,
    ): Boolean {
        val dataStore = DataStoreSingleton.getInstance(context)
        val storedAuthToken = dataStore.data.first()[DataStoreKeys.AUTH_TOKEN]
        val authToken = call.request.headers["Authorization"]
        if (storedAuthToken != authToken || authToken.isNullOrEmpty() || storedAuthToken.isNullOrEmpty()) {
            call.respondText(
                "{\"status\": \"error\", \"message\": \"Invalid or missing auth token\"}",
                contentType = ContentType.Application.Json,
                status = HttpStatusCode.Unauthorized,
            )
            return false
        } else {
            return true
        }
    }

    private suspend fun getBaseSuccessResponse(context: Context): StringBuilder {
        val dataStore = DataStoreSingleton.getInstance(context)
        val isMetric = dataStore.data.first()[DataStoreKeys.IS_METRIC] == true

        val jsonBuilder = StringBuilder()
        jsonBuilder.append("{\"status\": \"success\", \"isMetric\": \"$isMetric\", ")
        return jsonBuilder
    }

    private suspend fun getHydration(context: Context): String {
        val hydration = HydrationHelper.readHydrationLevel(context)
        val jsonBuilder = getBaseSuccessResponse(context)
        jsonBuilder.append("\"hydration\": $hydration")
        jsonBuilder.append("}")
        return jsonBuilder.toString()
    }

    private suspend fun getGoal(context: Context): String {
        val dataStore = DataStoreSingleton.getInstance(context)
        val goal = dataStore.data.first()[DataStoreKeys.HYDRATION_GOAL] ?: 2.0
        val jsonBuilder = getBaseSuccessResponse(context)
        jsonBuilder.append("\"goal\": $goal")
        jsonBuilder.append("}")
        return jsonBuilder.toString()
    }

    private suspend fun addHydration(
        context: Context,
        hydration: Int,
    ): String {
        HydrationHelper.writeHydrationLevel(context, hydration)
        val hydration = HydrationHelper.readHydrationLevel(context)
        val jsonBuilder = getBaseSuccessResponse(context)
        jsonBuilder.append("\"hydration\": $hydration")
        jsonBuilder.append("}")
        return jsonBuilder.toString()
    }

    private suspend fun updateGoal(
        context: Context,
        goal: Double,
    ): String {
        val dataStore = DataStoreSingleton.getInstance(context)
        dataStore.edit { preferences ->
            preferences[DataStoreKeys.HYDRATION_GOAL] = goal
        }
        val jsonBuilder = getBaseSuccessResponse(context)
        jsonBuilder.append("\"goal\": $goal")
        jsonBuilder.append("}")
        return jsonBuilder.toString()
    }

    private suspend fun getHistoryForDate(
        context: Context,
        dateInMonth: LocalDate,
    ): String {
        val data = HydrationHelper.getHydrationHistoryForMonth(context, dateInMonth)
        val jsonBuilder = getBaseSuccessResponse(context)
        jsonBuilder.append("\"history\": {")
        data.entries.forEachIndexed { index, entry ->
            jsonBuilder.append("\"${entry.key}\": ${entry.value}")
            if (index < data.size - 1) {
                jsonBuilder.append(", ")
            }
        }
        jsonBuilder.append("}}")
        return jsonBuilder.toString()
    }
}
