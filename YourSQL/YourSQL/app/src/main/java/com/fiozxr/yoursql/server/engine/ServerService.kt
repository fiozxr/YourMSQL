package com.fiozxr.yoursql.server.engine

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiManager
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.fiozxr.yoursql.R
import com.fiozxr.yoursql.YourSQLApplication
import com.fiozxr.yoursql.presentation.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import kotlinx.coroutines.*
import timber.log.Timber
import java.net.InetAddress
import java.net.NetworkInterface
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@AndroidEntryPoint
class ServerService : Service() {

    @Inject
    lateinit var serverEngine: YourSQLServerEngine

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val binder = LocalBinder()
    private var isRunning = false
    private var startTime: Long = 0
    private var requestCount = 0L

    private var serverJob: Job? = null
    private var currentPort: Int = DEFAULT_PORT

    private val connectivityManager by lazy {
        getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    }

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            super.onAvailable(network)
            Timber.d("Network available")
            if (isRunning) {
                updateNotification()
            }
        }

        override fun onLost(network: Network) {
            super.onLost(network)
            Timber.d("Network lost")
            if (isRunning) {
                updateNotification()
            }
        }
    }

    inner class LocalBinder : Binder() {
        fun getService(): ServerService = this@ServerService
    }

    override fun onCreate() {
        super.onCreate()
        registerNetworkCallback()
    }

    override fun onBind(intent: Intent): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startServer()
            ACTION_STOP -> stopServer()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        stopServer()
        unregisterNetworkCallback()
        serviceScope.cancel()
    }

    fun startServer(port: Int = DEFAULT_PORT) {
        if (isRunning) return

        currentPort = port
        startTime = System.currentTimeMillis()
        requestCount = 0

        val notification = createNotification()
        startForeground(NOTIFICATION_ID, notification)

        serverJob = serviceScope.launch(Dispatchers.IO) {
            try {
                isRunning = true
                serverEngine.start(port)
            } catch (e: Exception) {
                Timber.e(e, "Server failed to start")
                isRunning = false
                updateNotification(error = e.message)
            }
        }

        Timber.d("Server started on port $port")
    }

    fun stopServer() {
        if (!isRunning) return

        serverJob?.cancel()
        serviceScope.launch(Dispatchers.IO) {
            serverEngine.stop()
        }

        isRunning = false
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()

        Timber.d("Server stopped")
    }

    fun isServerRunning(): Boolean = isRunning

    fun getServerPort(): Int = currentPort

    fun getLocalIpAddress(): String? {
        return try {
            val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val wifiInfo = wifiManager.connectionInfo
            val ipInt = wifiInfo.ipAddress
            String.format(
                "%d.%d.%d.%d",
                ipInt and 0xff,
                ipInt shr 8 and 0xff,
                ipInt shr 16 and 0xff,
                ipInt shr 24 and 0xff
            )
        } catch (e: Exception) {
            getNetworkInterfaceIp()
        }
    }

    fun getUptime(): Long {
        return if (isRunning) System.currentTimeMillis() - startTime else 0
    }

    fun incrementRequestCount() {
        requestCount++
    }

    fun getRequestCount(): Long = requestCount

    private fun getNetworkInterfaceIp(): String? {
        return try {
            NetworkInterface.getNetworkInterfaces().toList()
                .flatMap { it.inetAddresses.toList() }
                .firstOrNull { !it.isLoopbackAddress && it is java.net.Inet4Address }
                ?.hostAddress
        } catch (e: Exception) {
            null
        }
    }

    private fun createNotification(error: String? = null): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        val ipAddress = getLocalIpAddress() ?: "Unknown"
        val contentText = if (error != null) {
            "Error: $error"
        } else {
            "Running on $ipAddress:$currentPort"
        }

        val stopIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, ServerService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, YourSQLApplication.CHANNEL_SERVER)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .addAction(android.R.drawable.ic_media_pause, "Stop", stopIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun updateNotification(error: String? = null) {
        val notification = createNotification(error)
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun registerNetworkCallback() {
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        connectivityManager.registerNetworkCallback(request, networkCallback)
    }

    private fun unregisterNetworkCallback() {
        try {
            connectivityManager.unregisterNetworkCallback(networkCallback)
        } catch (e: Exception) {
            // Ignore
        }
    }

    companion object {
        const val DEFAULT_PORT = 5432
        const val NOTIFICATION_ID = 1
        const val ACTION_START = "com.fiozxr.yoursql.START_SERVER"
        const val ACTION_STOP = "com.fiozxr.yoursql.STOP_SERVER"

        fun startService(context: Context, port: Int = DEFAULT_PORT) {
            val intent = Intent(context, ServerService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, ServerService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }
}
