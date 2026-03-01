package com.fiozxr.yoursql.server.middleware

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.util.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.ConcurrentLinkedQueue
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RequestLogger @Inject constructor() {

    private val _logs = MutableStateFlow<List<RequestLogEntry>>(emptyList())
    val logs: StateFlow<List<RequestLogEntry>> = _logs.asStateFlow()

    private val logQueue = ConcurrentLinkedQueue<RequestLogEntry>()
    private val maxLogEntries = 1000

    fun log(entry: RequestLogEntry) {
        logQueue.offer(entry)

        // Trim if exceeds max
        while (logQueue.size > maxLogEntries) {
            logQueue.poll()
        }

        _logs.value = logQueue.toList().sortedByDescending { it.timestamp }
        Timber.d("${entry.method} ${entry.path} - ${entry.statusCode} (${entry.latencyMs}ms)")
    }

    fun clearLogs() {
        logQueue.clear()
        _logs.value = emptyList()
    }

    fun getRecentLogs(limit: Int = 100): List<RequestLogEntry> {
        return _logs.value.take(limit)
    }
}

data class RequestLogEntry(
    val timestamp: Long = System.currentTimeMillis(),
    val method: String,
    val path: String,
    val statusCode: Int,
    val latencyMs: Long,
    val clientIp: String,
    val apiKey: String? = null
) {
    val formattedTimestamp: String
        get() = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
            .format(Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()))

    val statusCategory: StatusCategory
        get() = when (statusCode) {
            in 200..299 -> StatusCategory.SUCCESS
            in 300..399 -> StatusCategory.REDIRECT
            in 400..499 -> StatusCategory.CLIENT_ERROR
            in 500..599 -> StatusCategory.SERVER_ERROR
            else -> StatusCategory.UNKNOWN
        }
}

enum class StatusCategory {
    SUCCESS,      // 2xx - Green
    REDIRECT,     // 3xx - Yellow
    CLIENT_ERROR, // 4xx - Orange
    SERVER_ERROR  // 5xx - Red
}

class RequestLoggingPluginConfig {
    lateinit var logger: RequestLogger
}

val RequestLoggingPlugin = createApplicationPlugin(
    name = "RequestLogging",
    createConfiguration = ::RequestLoggingPluginConfig
) {
    val logger = pluginConfig.logger

    onCall { call ->
        call.attributes.put(requestStartTime, System.currentTimeMillis())
    }

    onCallRespond { call ->
        val startTime = call.attributes[requestStartTime]
        val latency = System.currentTimeMillis() - startTime

        val entry = RequestLogEntry(
            method = call.request.httpMethod.value,
            path = call.request.path(),
            statusCode = call.response.status()?.value ?: 0,
            latencyMs = latency,
            clientIp = call.request.origin.remoteHost,
            apiKey = call.request.headers["apikey"] ?: call.request.headers["Authorization"]
                ?.removePrefix("Bearer ")
        )

        logger.log(entry)
    }
}

private val requestStartTime = AttributeKey<Long>("RequestStartTime")
