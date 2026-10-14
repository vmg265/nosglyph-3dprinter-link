package com.example.bambuglyph.data

import android.content.Context
import android.content.SharedPreferences
import com.example.bambuglyph.model.GlyphDeviceType
import com.example.bambuglyph.model.PrinterConfig

class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var isOnboardingCompleted: Boolean
        get() = prefs.getBoolean(KEY_ONBOARDING_COMPLETED, false)
        set(value) = prefs.edit().putBoolean(KEY_ONBOARDING_COMPLETED, value).apply()

    fun getPrinterConfig(): PrinterConfig {
        val ip = prefs.getString(KEY_IP, "192.168.1.100") ?: "192.168.1.100"
        val port = prefs.getInt(KEY_PORT, 8883)
        val serial = prefs.getString(KEY_SERIAL, "01P00A000000000") ?: "01P00A000000000"
        val code = prefs.getString(KEY_CODE, "12345678") ?: "12345678"
        val typeName = prefs.getString(KEY_DEVICE_TYPE, GlyphDeviceType.AUTO_DETECT.name) ?: GlyphDeviceType.AUTO_DETECT.name
        val deviceType = try {
            GlyphDeviceType.valueOf(typeName)
        } catch (_: Exception) {
            GlyphDeviceType.AUTO_DETECT
        }
        val mockMode = prefs.getBoolean(KEY_MOCK_MODE, false)

        return PrinterConfig(
            ipAddress = ip,
            port = port,
            serialNumber = serial,
            accessCode = code,
            deviceType = deviceType,
            mockMode = mockMode
        )
    }

    fun savePrinterConfig(config: PrinterConfig) {
        prefs.edit()
            .putString(KEY_IP, config.ipAddress)
            .putInt(KEY_PORT, config.port)
            .putString(KEY_SERIAL, config.serialNumber)
            .putString(KEY_CODE, config.accessCode)
            .putString(KEY_DEVICE_TYPE, config.deviceType.name)
            .putBoolean(KEY_MOCK_MODE, config.mockMode)
            .apply()
    }

    companion object {
        private const val PREFS_NAME = "bambu_glyph_prefs"
        private const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"
        private const val KEY_IP = "printer_ip"
        private const val KEY_PORT = "printer_port"
        private const val KEY_SERIAL = "printer_serial"
        private const val KEY_CODE = "printer_code"
        private const val KEY_DEVICE_TYPE = "device_type"
        private const val KEY_MOCK_MODE = "mock_mode"
    }
}
