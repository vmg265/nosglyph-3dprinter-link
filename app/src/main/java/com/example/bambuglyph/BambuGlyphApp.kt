package com.example.bambuglyph

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.example.bambuglyph.data.PreferencesManager
import com.example.bambuglyph.data.PrinterRepository

class BambuGlyphApp : Application() {
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        // Load saved configuration into repository
        val prefs = PreferencesManager(this)
        PrinterRepository.updateConfig(prefs.getPrinterConfig())
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.channel_desc)
                setShowBadge(false)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_ID = "bambu_glyph_monitor_channel"
    }
}
