package com.yukigasai.trinkaus.util

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.glance.appwidget.updateAll
import com.yukigasai.trinkaus.service.WaterServerService.Companion.triggerNotificationUpdate
import com.yukigasai.trinkaus.shared.Constants.DataStore.DataStoreKeys
import com.yukigasai.trinkaus.shared.DataStoreSingleton
import com.yukigasai.trinkaus.shared.HydrationOption
import com.yukigasai.trinkaus.shared.UnitHelper
import com.yukigasai.trinkaus.shared.WearableMessenger
import com.yukigasai.trinkaus.shared.getDefaultAmount
import com.yukigasai.trinkaus.widget.TrinkAusWidget
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
import io.ktor.server.websocket.DefaultWebSocketServerSession
import io.ktor.server.websocket.WebSockets
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.net.Inet4Address
import java.net.NetworkInterface
import java.time.LocalDate
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicReference

/**
 * Important TODO
 *  - Add error handling for network issues
 *  - Add SSL support for secure connections
 *      - Without this, everyone in the local network can find the authToken
 */
object ServerManager {
    private val ktorServer = AtomicReference<EmbeddedServer<NettyApplicationEngine, NettyApplicationEngine.Configuration>?>(null)
    private val webSocketConnections = Collections.newSetFromMap(ConcurrentHashMap<DefaultWebSocketServerSession, Boolean>())

    private val serverMutex = Mutex()

    const val PORT = 8372

    fun isRunning(): Boolean = ktorServer.get() != null

    suspend fun startServer(context: Context): Result<Unit> =
        serverMutex.withLock {
            withContext(Dispatchers.IO) {
                if (isRunning()) {
                    println("Server is already running.")
                    return@withContext Result.success(Unit)
                }

                try {
                    val server =
                        embeddedServer(Netty, port = PORT, host = "0.0.0.0") {
                            install(CORS) {
                                anyHost()
                                allowHeader(HttpHeaders.ContentType)
                                allowHeader(HttpHeaders.Authorization)
                                allowHeader(HttpHeaders.AccessControlAllowOrigin)
                                allowMethod(HttpMethod.Get)
                                allowMethod(HttpMethod.Post)
                                allowSameOrigin = true
                            }
                            install(WebSockets)
                            routing {
                                get("/unit") {
                                    val result = validateRequest(context, call)
                                    if (!result) return@get

                                    call.respondText(
                                        getUnit(context),
                                        contentType = ContentType.Application.Json,
                                    )
                                }
                                get("/hydration") {
                                    val result = validateRequest(context, call)
                                    if (!result) return@get

                                    val date = getDateFromRequest(call)
                                    if (date == null) return@get

                                    call.respondText(
                                        getHydration(context, date),
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

                                get("/streaks") {
                                    val result = validateRequest(context, call)
                                    if (!result) return@get

                                    call.respondText(
                                        getStreaks(context),
                                        contentType = ContentType.Application.Json,
                                    )
                                }

                                get("/addHydrationAmounts") {
                                    val result = validateRequest(context, call)
                                    if (!result) return@get

                                    call.respondText(
                                        getAddHydrationAmounts(context),
                                        contentType = ContentType.Application.Json,
                                    )
                                }

                                get("/history") {
                                    val result = validateRequest(context, call)
                                    if (!result) return@get

                                    val date = getDateFromRequest(call)
                                    if (date == null) return@get

                                    val response = getHistoryForDate(context, date)
                                    call.respondText(
                                        response,
                                        contentType = ContentType.Application.Json,
                                    )
                                }
                                post("/hydration") {
                                    val result = validateRequest(context, call)
                                    if (!result) return@post

                                    val date = getDateFromRequest(call)
                                    if (date == null) return@post

                                    val hydration =
                                        call.request.queryParameters["hydration"]?.toIntOrNull()
                                    if (hydration != null) {
                                        // Save hydration level to DataStore
                                        val response = addHydration(context, hydration, date)
                                        call.respondText(
                                            response,
                                            contentType = ContentType.Application.Json,
                                        )
                                    } else {
                                        call.respondText(
                                            "\"message\": \"Invalid hydration value\"}",
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
                                            "{\"message\": \"Invalid goal value\"}",
                                            contentType = ContentType.Application.Json,
                                        )
                                    }
                                }
                                webSocket("/hydration-updates") {
                                    val token = call.request.queryParameters["token"] ?: ""
                                    println(token)
                                    if (!isTokenValid(context, token)) {
                                        close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Invalid or missing auth token"))
                                        return@webSocket
                                    }

                                    // Add the session to our connection set
                                    webSocketConnections += this
                                    println("Client connected via WebSocket. Total clients: ${webSocketConnections.size}")
                                    try {
                                        // Keep the connection alive and listen for the client to close it.
                                        // We ignore any incoming frames from the client.
                                        for (frame in incoming) {
                                            // Do nothing, just keep connection open
                                        }
                                    } catch (e: ClosedReceiveChannelException) {
                                        // This exception is expected when the client disconnects.
                                        println("Client disconnected. ${e.message}")
                                    } catch (e: Exception) {
                                        println("Error in WebSocket session: ${e.message}")
                                    } finally {
                                        // Remove the session from the set on disconnect.
                                        webSocketConnections -= this
                                        println("Client session removed. Total clients: ${webSocketConnections.size}")
                                    }
                                }
                            }
                        }.start(wait = false)

                    ktorServer.set(server)
                    println("Server started successfully on port $PORT")
                    triggerNotificationUpdate(context)
                    Result.success(Unit)
                } catch (e: Exception) {
                    println("Error starting server: ${e.message}")
                    e.printStackTrace()
                    ktorServer.set(null)
                    Result.failure(e)
                }
            }
        }

    suspend fun stopServer() =
        serverMutex.withLock {
            withContext(Dispatchers.IO) {
                val server = ktorServer.getAndSet(null)
                if (server != null) {
                    println("Stopping server...")
                    try {
                        // Close all WebSocket connections gracefully before stopping the server
                        webSocketConnections.forEach { it.close(CloseReason(CloseReason.Codes.NORMAL, "Server shutting down")) }
                        webSocketConnections.clear()
                        server.stop(1000, 2000)
                        println("Server stopped.")
                    } catch (e: Exception) {
                        println("Error stopping server: ${e.message}")
                    }
                }
            }
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

    private suspend fun isTokenValid(
        context: Context,
        token: String,
    ): Boolean {
        val dataStore = DataStoreSingleton.getInstance(context)
        val storedAuthToken = dataStore.data.first()[DataStoreKeys.AUTH_TOKEN]

        if (storedAuthToken.isNullOrEmpty() || storedAuthToken.isEmpty()) {
            return false
        }

        return storedAuthToken == token
    }

    private suspend fun validateRequest(
        context: Context,
        call: RoutingCall,
    ): Boolean {
        val authToken = call.queryParameters["token"] ?: ""
        if (!isTokenValid(context, authToken)) {
            call.respondText(
                "{\"message\": \"Invalid or missing auth token\"}",
                contentType = ContentType.Application.Json,
                status = HttpStatusCode.Unauthorized,
            )
            return false
        } else {
            return true
        }
    }

    private suspend fun getDateFromRequest(call: RoutingCall): LocalDate? {
        val date =
            call.request.queryParameters["date"]?.let {
                try {
                    LocalDate.parse(it)
                } catch (e: Exception) {
                    call.respondText(
                        "{\"message\": \"Invalid date format use YYYY-MM-DD\"}",
                        status = HttpStatusCode.BadRequest,
                        contentType = ContentType.Application.Json,
                    )
                    return null
                }
            } ?: LocalDate.now()
        return date
    }

    private suspend fun getUnit(context: Context): String {
        val dataStore = DataStoreSingleton.getInstance(context)
        val isMetric = dataStore.data.first()[DataStoreKeys.IS_METRIC] != false
        return "{\"isMetric\": $isMetric }"
    }

    private suspend fun getHydration(
        context: Context,
        date: LocalDate = LocalDate.now(),
    ): String {
        var hydration = HydrationHelper.readHydrationLevel(context, date, readOnly = true)

        if (UnitHelper.isMetric()) {
            hydration = hydration * 1000
        }

        return "{\"hydration\": ${hydration.toInt()} }"
    }

    private suspend fun getGoal(context: Context): String {
        val dataStore = DataStoreSingleton.getInstance(context)
        var goal = dataStore.data.first()[DataStoreKeys.HYDRATION_GOAL] ?: 2.0
        if (UnitHelper.isMetric()) {
            goal = goal * 1000
        }

        return "{\"goal\": ${goal.toInt()} }"
    }

    private suspend fun getAddHydrationAmounts(context: Context): String {
        val dataStore = DataStoreSingleton.getInstance(context)
        val data = dataStore.data.first()
        val small = data[DataStoreKeys.SMALL_AMOUNT] ?: HydrationOption.SMALL.getDefaultAmount()
        val medium = data[DataStoreKeys.MEDIUM_AMOUNT] ?: HydrationOption.MEDIUM.getDefaultAmount()
        val large = data[DataStoreKeys.LARGE_AMOUNT] ?: HydrationOption.LARGE.getDefaultAmount()
        return "{\"small\": $small, \"medium\": $medium, \"large\": $large }"
    }

    private suspend fun getStreaks(context: Context): String {
        val dataStore = DataStoreSingleton.getInstance(context)
        val goal = dataStore.data.first()[DataStoreKeys.HYDRATION_GOAL] ?: 2.0
        val longestStreak = HydrationHelper.getLongestWaterIntakeStreak(context, goal)
        val currentStreak = HydrationHelper.getCurrentWaterIntakeStreakLength(context, goal)

        return """
            {
                "longestStreak": {
                    "length": ${longestStreak.length}
                    ${if (longestStreak.startDate != null) ",\"startDate\": \"${longestStreak.startDate}\"" else ""}
                },
                "currentStreak": {
                    "length": ${currentStreak.length}
                     ${if (currentStreak.startDate != null) ",\"startDate\": \"${currentStreak.startDate}\"" else ""}
                }
            }
            """.trimIndent()
    }

    private suspend fun addHydration(
        context: Context,
        hydration: Int,
        date: LocalDate = LocalDate.now(),
    ): String {
        HydrationHelper.writeHydrationLevel(context, hydration, date)
        var hydration = HydrationHelper.readHydrationLevel(context, date, readOnly = true)

        if (date == LocalDate.now()) {
            val dataStore = DataStoreSingleton.getInstance(context)
            dataStore.edit { preferences ->
                preferences[DataStoreKeys.HYDRATION_LEVEL] = hydration
            }
        }

        if (UnitHelper.isMetric()) {
            hydration = hydration * 1000
        }

        triggerNotificationUpdate(context)
        WearableMessenger.sendMessage(
            context,
            com.yukigasai.trinkaus.shared.Constants.Path.UPDATE_HYDRATION,
            hydration,
        )
        TrinkAusWidget().updateAll(context)
        broadcastHydrationUpdate(context)

        return "{\"hydration\": ${hydration.toInt()} }"
    }

    private suspend fun updateGoal(
        context: Context,
        goal: Double,
    ): String {
        val dataStore = DataStoreSingleton.getInstance(context)
        dataStore.edit { preferences ->
            preferences[DataStoreKeys.HYDRATION_GOAL] = goal
        }
        return "{\"goal\": $goal }"
    }

    private suspend fun getHistoryForDate(
        context: Context,
        dateInMonth: LocalDate,
    ): String {
        val data = HydrationHelper.getHydrationHistoryForMonth(context, dateInMonth)
        val jsonBuilder = StringBuilder()

        val isMetric = UnitHelper.isMetric()

        jsonBuilder.append("{\"history\": {")
        data.entries.forEachIndexed { index, entry ->

            var hydration = entry.value
            if (isMetric) {
                hydration = entry.value * 1000
            }

            jsonBuilder.append("\"${entry.key}\": ${hydration.toInt()}")
            if (index < data.size - 1) {
                jsonBuilder.append(", ")
            }
        }
        jsonBuilder.append("}}")
        return jsonBuilder.toString()
    }

    private suspend fun broadcastUpdate(jsonPayload: String) {
        val deadSessions = mutableSetOf<DefaultWebSocketServerSession>()
        for (session in webSocketConnections) {
            try {
                session.send(Frame.Text(jsonPayload))
            } catch (e: Exception) {
                // The client session is likely dead. Mark it for removal.
                println("Failed to send to client, marking for removal: ${e.message}")
                deadSessions.add(session)
            }
        }
        // Remove all dead sessions from the main connection set
        if (deadSessions.isNotEmpty()) {
            webSocketConnections.removeAll(deadSessions)
            println("Removed ${deadSessions.size} dead clients. Total clients: ${webSocketConnections.size}")
        }
    }

    suspend fun broadcastHydrationUpdate(context: Context) {
        if (!isRunning()) {
            return
        }
        val dataStore = DataStoreSingleton.getInstance(context)
        var hydration = HydrationHelper.readHydrationLevel(context, readOnly = true)
        var goal = dataStore.data.first()[DataStoreKeys.HYDRATION_GOAL] ?: 2.0
        if (UnitHelper.isMetric()) {
            goal = goal * 1000
        }

        if (UnitHelper.isMetric()) {
            hydration = hydration * 1000
        }

        val jsonData =
            """
            {
                "hydration": ${hydration.toInt()},
                "goal": ${goal.toInt()}
            }
            """.trimIndent()

        broadcastUpdate(jsonData)
    }
}
