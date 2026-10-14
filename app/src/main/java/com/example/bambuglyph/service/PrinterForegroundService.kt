package com.example.bambuglyph.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.bambuglyph.BambuGlyphApp
import com.example.bambuglyph.MainActivity
import com.example.bambuglyph.R
import com.example.bambuglyph.data.PreferencesManager
import com.example.bambuglyph.data.PrinterRepository
import com.example.bambuglyph.glyph.GlyphController
import com.example.bambuglyph.model.PrintState
import com.example.bambuglyph.mqtt.BambuMockClient
import com.example.bambuglyph.mqtt.BambuMqttClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class PrinterForegroundService : Service() {
    private val tag = "PrinterFgService"
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private var wakeLock: PowerManager.WakeLock? = null
    private var glyphController: GlyphController? = null
    private var mqttClient: BambuMqttClient? = null
    private var mockClient: BambuMockClient? = null
    private var renderJob: Job? = null
    private var statusCollectorJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        Log.i(tag, "Service onCreate")
        acquireWakeLock()
        initGlyph()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_START

        when (action) {
            ACTION_STOP -> {
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_SET_DEMO_STATE -> {
                val stateName = intent?.getStringExtra(EXTRA_DEMO_STATE)
                if (stateName != null) {
                    try {
                        val state = PrintState.valueOf(stateName)
                        mockClient?.setDemoState(state)
                    } catch (e: Exception) {
                        Log.w(tag, "Unknown state: $stateName")
                    }
                }
            }
            ACTION_START -> {
                startForegroundWithNotification()
                startMonitoring()
            }
        }

        return START_STICKY
    }

    private fun acquireWakeLock() {
        try {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "BambuGlyph:MonitorWakeLock"
            ).apply {
                setReferenceCounted(false)
                acquire(12 * 60 * 60 * 1000L) // 12 hours timeout max
            }
            Log.i(tag, "Partial WakeLock acquired for screen-off execution")
        } catch (e: Exception) {
            Log.w(tag, "Failed to acquire WakeLock: ${e.message}")
        }
    }

    private fun startForegroundWithNotification() {
        val notification = buildNotification("Starting 3D printer monitor...")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
        PrinterRepository.updateServiceRunning(true)
    }

    private fun initGlyph() {
        glyphController = GlyphController(applicationContext).apply {
            init()
        }
    }

    private fun startMonitoring() {
        val prefs = PreferencesManager(applicationContext)
        val config = prefs.getPrinterConfig()
        PrinterRepository.updateConfig(config)

        if (config.mockMode) {
            Log.i(tag, "Starting Mock Demo client")
            mqttClient?.disconnect()
            mqttClient = null

            mockClient = BambuMockClient(serviceScope).apply {
                start()
            }
        } else {
            Log.i(tag, "Starting live Bambu MQTT client for ${config.ipAddress}")
            mockClient?.stop()
            mockClient = null

            mqttClient = BambuMqttClient(config, serviceScope).apply {
                connect()
            }
        }

        // Start render loop
        startRenderLoop()

        // Collect status updates to update the system notification
        statusCollectorJob?.cancel()
        statusCollectorJob = serviceScope.launch {
            PrinterRepository.status.collect { status ->
                updateNotification(status.statusMessage)
            }
        }
    }

    private fun startRenderLoop() {
        renderJob?.cancel()
        renderJob = serviceScope.launch(Dispatchers.Default) {
            var phase = 0.0
            val startTime = System.currentTimeMillis()

            while (isActive) {
                val status = PrinterRepository.status.value
                val elapsedSeconds = (System.currentTimeMillis() - startTime) / 1000.0
                phase = elapsedSeconds * 2.0 * Math.PI

                // Render to Glyph Matrix
                glyphController?.renderFrame(status, phase)

                // High framerate (25 FPS / 40ms) when breathing checkmark or preheat pulse, otherwise 1000ms
                val intervalMs = when (status.printState) {
                    PrintState.COMPLETED -> 40L
                    PrintState.PREHEAT -> 80L
                    else -> 1000L
                }

                delay(intervalMs)
            }
        }
    }

    private fun buildNotification(text: String): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, BambuGlyphApp.CHANNEL_ID)
            .setContentTitle("3D Printer Glyph Monitor")
            .setContentText(text)
            .setSmallIcon(R.drawable.img_toy_preview)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    private fun updateNotification(text: String) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, buildNotification(text))
    }

    override fun onDestroy() {
        Log.i(tag, "Service onDestroy")
        renderJob?.cancel()
        statusCollectorJob?.cancel()
        serviceScope.cancel()

        mqttClient?.disconnect()
        mockClient?.stop()
        glyphController?.destroy()

        wakeLock?.let {
            if (it.isHeld) it.release()
        }

        PrinterRepository.updateServiceRunning(false)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val NOTIFICATION_ID = 23112
        const val ACTION_START = "com.example.bambuglyph.action.START"
        const val ACTION_STOP = "com.example.bambuglyph.action.STOP"
        const val ACTION_SET_DEMO_STATE = "com.example.bambuglyph.action.SET_DEMO_STATE"
        const val EXTRA_DEMO_STATE = "extra_demo_state"

        fun start(context: Context) {
            val intent = Intent(context, PrinterForegroundService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, PrinterForegroundService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }

        fun setDemoState(context: Context, state: PrintState) {
            val intent = Intent(context, PrinterForegroundService::class.java).apply {
                action = ACTION_SET_DEMO_STATE
                putExtra(EXTRA_DEMO_STATE, state.name)
            }
            context.startService(intent)
        }
    }
}
