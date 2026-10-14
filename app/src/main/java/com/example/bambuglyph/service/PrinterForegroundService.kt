package com.example.bambuglyph.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
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

    private val tag = "PrinterForegroundService"
    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)

    private var wakeLock: PowerManager.WakeLock? = null
    private var glyphController: GlyphController? = null
    private var mqttClient: BambuMqttClient? = null

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
                "Glyph3DMonitor:ForegroundWakeLock"
            ).apply {
                setReferenceCounted(false)
                acquire(12 * 60 * 60 * 1000L) // 12 hours max
            }
            Log.i(tag, "WakeLock acquired")
        } catch (e: Exception) {
            Log.e(tag, "Failed to acquire wake lock", e)
        }
    }

    private fun releaseWakeLock() {
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
                Log.i(tag, "WakeLock released")
            }
        } catch (e: Exception) {
            Log.e(tag, "Failed to release wake lock", e)
        }
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

        Log.i(tag, "Starting MQTT client for ${config.ipAddress}")
        mqttClient?.disconnect()
        mqttClient = BambuMqttClient(config, serviceScope).apply {
            connect()
        }

        // Start render loop
        startRenderLoop()

        // Collect status updates to update the system notification
        statusCollectorJob?.cancel()
        statusCollectorJob = serviceScope.launch {
            PrinterRepository.status.collect { status ->
                val notification = buildNotification(status)
                val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
                notificationManager.notify(NOTIFICATION_ID, notification)
            }
        }

        PrinterRepository.updateServiceRunning(true)
    }

    private fun startRenderLoop() {
        renderJob?.cancel()
        renderJob = serviceScope.launch {
            Log.i(tag, "Starting Glyph Matrix animation render loop")
            var phase = 0.0

            while (isActive) {
                val currentStatus = PrinterRepository.status.value
                glyphController?.renderFrame(currentStatus, phase)

                // ~20-25 FPS loop (40ms) for smooth breathing/pulsing
                phase += 0.04
                if (phase > 1000.0) phase = 0.0
                delay(40L)
            }
        }
    }

    private fun startForegroundWithNotification() {
        val notification = buildNotification(PrinterRepository.status.value)
        startForeground(NOTIFICATION_ID, notification)
    }

    private fun buildNotification(status: com.example.bambuglyph.model.PrinterStatus): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val text = when (status.printState) {
            PrintState.PREHEAT -> "Preheating (PH) • ${status.nozzleTemp.toInt()}°C"
            PrintState.PRINTING -> "${status.percent}% • ${status.remainingMinutes}m left • ${status.subtaskName}"
            PrintState.PAUSED -> "Paused (||) at ${status.percent}%"
            PrintState.COMPLETED -> "Print Finished (100%)"
            PrintState.FAILED -> "Print Error / Interrupted"
            PrintState.IDLE -> if (status.isConnected) "Printer Online • Standby" else "Connecting to 3D Printer..."
        }

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

    override fun onDestroy() {
        Log.i(tag, "Service onDestroy")
        renderJob?.cancel()
        statusCollectorJob?.cancel()
        serviceScope.cancel()

        mqttClient?.disconnect()
        glyphController?.destroy()
        releaseWakeLock()

        PrinterRepository.updateServiceRunning(false)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val NOTIFICATION_ID = 23112
        const val ACTION_START = "com.example.bambuglyph.action.START"
        const val ACTION_STOP = "com.example.bambuglyph.action.STOP"

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
    }
}
