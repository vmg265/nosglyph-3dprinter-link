package com.example.bambuglyph.glyph

import android.content.ComponentName
import android.content.Context
import android.util.Log
import com.example.bambuglyph.data.PrinterRepository
import com.example.bambuglyph.model.GlyphDeviceType
import com.example.bambuglyph.model.PrinterStatus
import com.nothing.ketchum.Common
import com.nothing.ketchum.Glyph
import com.nothing.ketchum.GlyphMatrixManager

class GlyphController(private val context: Context) {
    private val tag = "GlyphController"

    private var glyphManager: GlyphMatrixManager? = null
    private var isConnected = false
    private var detectedDevice: String = Glyph.DEVICE_23112
    private var matrixLength: Int = 25

    init {
        detectDevice()
    }

    private fun detectDevice() {
        val configuredType = PrinterRepository.config.value.deviceType

        when (configuredType) {
            GlyphDeviceType.PHONE_3 -> {
                detectedDevice = Glyph.DEVICE_23112
                matrixLength = 25
            }
            GlyphDeviceType.PHONE_4A_PRO -> {
                detectedDevice = Glyph.DEVICE_25111p
                matrixLength = 13
            }
            GlyphDeviceType.AUTO_DETECT -> {
                try {
                    val len = Common.getDeviceMatrixLength()
                    if (len == 13 || Common.is25111p()) {
                        detectedDevice = Glyph.DEVICE_25111p
                        matrixLength = 13
                    } else {
                        detectedDevice = Glyph.DEVICE_23112
                        matrixLength = 25
                    }
                } catch (e: Exception) {
                    Log.w(tag, "Failed to query hardware matrix length, defaulting to 25x25: ${e.message}")
                    detectedDevice = Glyph.DEVICE_23112
                    matrixLength = 25
                }
            }
        }
        Log.i(tag, "Glyph Matrix configured for $detectedDevice ($matrixLength x $matrixLength)")
    }

    fun init() {
        detectDevice()
        try {
            glyphManager = GlyphMatrixManager.getInstance(context)
            glyphManager?.init(object : GlyphMatrixManager.Callback {
                override fun onServiceConnected(name: ComponentName?) {
                    Log.i(tag, "Glyph Matrix service connected: $name")
                    try {
                        val registered = glyphManager?.register(detectedDevice) ?: false
                        isConnected = registered
                        Log.i(tag, "Glyph registration result: $registered for $detectedDevice")
                    } catch (e: Exception) {
                        Log.e(tag, "Error registering target device: ${e.message}", e)
                    }
                }

                override fun onServiceDisconnected(name: ComponentName?) {
                    Log.w(tag, "Glyph Matrix service disconnected: $name")
                    isConnected = false
                }
            })
        } catch (e: Throwable) {
            Log.w(tag, "Glyph SDK not available on this device: ${e.message}")
            glyphManager = null
            isConnected = false
        }
    }

    fun renderFrame(status: PrinterStatus, animPhase: Double = 0.0) {
        val frame = GlyphMatrixRenderer.render(status, matrixLength, animPhase)

        // Always update Repository for UI preview
        PrinterRepository.updateGlyphFrame(frame, matrixLength)

        // Send to physical Nothing Glyph Matrix if hardware is present
        if (isConnected && glyphManager != null) {
            try {
                glyphManager?.setAppMatrixFrame(frame)
            } catch (e: Exception) {
                try {
                    glyphManager?.setMatrixFrame(frame)
                } catch (e2: Exception) {
                    Log.e(tag, "Error sending frame to Glyph Matrix: ${e2.message}")
                }
            }
        }
    }

    fun turnOff() {
        try {
            val empty = IntArray(matrixLength * matrixLength) { 0 }
            PrinterRepository.updateGlyphFrame(empty, matrixLength)
            glyphManager?.turnOff()
            glyphManager?.closeAppMatrix()
        } catch (e: Exception) {
            Log.w(tag, "Error turning off Glyph Matrix: ${e.message}")
        }
    }

    fun destroy() {
        turnOff()
        try {
            glyphManager?.unInit()
        } catch (e: Exception) {
            Log.w(tag, "Error uninitializing Glyph Manager: ${e.message}")
        }
        glyphManager = null
        isConnected = false
    }

    fun getMatrixLength(): Int = matrixLength
}
