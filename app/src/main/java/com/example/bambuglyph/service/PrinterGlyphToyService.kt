package com.example.bambuglyph.service

import android.app.Service
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Message
import android.os.Messenger
import android.util.Log
import com.example.bambuglyph.data.PreferencesManager
import com.example.bambuglyph.data.PrinterRepository
import com.example.bambuglyph.glyph.GlyphController
import com.example.bambuglyph.model.PrintState
import com.nothing.ketchum.GlyphToy

/**
 * Service recognized by Nothing OS as a native "Glyph Toy".
 * Shows up in Nothing Phone Glyph Toy Carousel, handles AOD (Always On Display) when screen is off,
 * and responds to physical Glyph Button interactions.
 */
class PrinterGlyphToyService : Service() {
    private val tag = "PrinterGlyphToyService"
    private var glyphController: GlyphController? = null

    private val serviceHandler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                GlyphToy.MSG_GLYPH_TOY -> {
                    val bundle: Bundle? = msg.data
                    val event = bundle?.getString(GlyphToy.MSG_GLYPH_TOY_DATA)
                    Log.i(tag, "Received Glyph Toy event: $event")

                    when (event) {
                        GlyphToy.EVENT_CHANGE -> {
                            // Long-press on Glyph Button: cycle demo state or trigger refresh
                            handleGlyphButtonLongPress()
                        }
                        GlyphToy.EVENT_AOD -> {
                            // Received periodically (e.g. every minute) when screen is off in AOD mode
                            renderCurrentStatus()
                        }
                        GlyphToy.STATUS_START -> {
                            initToy()
                        }
                        GlyphToy.STATUS_END -> {
                            cleanupToy()
                        }
                    }
                }
                else -> super.handleMessage(msg)
            }
        }
    }

    private val serviceMessenger = Messenger(serviceHandler)

    override fun onBind(intent: Intent?): IBinder {
        Log.i(tag, "onBind from Nothing OS Glyph Toy manager")
        initToy()
        return serviceMessenger.binder
    }

    override fun onUnbind(intent: Intent?): Boolean {
        Log.i(tag, "onUnbind")
        cleanupToy()
        return false
    }

    private fun initToy() {
        if (glyphController == null) {
            glyphController = GlyphController(applicationContext).apply {
                init()
            }
        }
        renderCurrentStatus()
    }

    private fun renderCurrentStatus() {
        val status = PrinterRepository.status.value
        glyphController?.renderFrame(status)
    }

    private fun handleGlyphButtonLongPress() {
        // Refresh and re-render frame on physical button long press
        renderCurrentStatus()
    }

    private fun cleanupToy() {
        glyphController?.destroy()
        glyphController = null
    }

    override fun onDestroy() {
        cleanupToy()
        super.onDestroy()
    }
}
