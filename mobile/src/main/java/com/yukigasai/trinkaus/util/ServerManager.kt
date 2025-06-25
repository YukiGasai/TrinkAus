package com.yukigasai.trinkaus.util

import android.content.Context
import androidx.datastore.preferences.core.edit
import com.yukigasai.trinkaus.shared.Constants.DataStore.DataStoreKeys
import com.yukigasai.trinkaus.shared.DataStoreSingleton
import com.yukigasai.trinkaus.shared.HydrationOption
import com.yukigasai.trinkaus.shared.UnitHelper
import com.yukigasai.trinkaus.shared.getDefaultAmount
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
                    allowHeader(HttpHeaders.AccessControlAllowOrigin)
                    allowMethod(HttpMethod.Get)
                    allowMethod(HttpMethod.Post)
                    allowSameOrigin = true
                }
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

                        val hydration = call.request.queryParameters["hydration"]?.toIntOrNull()
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
        val authToken = call.queryParameters["token"]
        if (storedAuthToken != authToken || authToken.isNullOrEmpty() || storedAuthToken.isNullOrEmpty()) {
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

        if (UnitHelper.isMetric()) {
            hydration = hydration * 1000
        }

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
}
