package com.example.bambuglyph.data

import com.example.bambuglyph.model.PrinterConfig
import com.example.bambuglyph.model.PrinterStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object PrinterRepository {
    private val _status = MutableStateFlow(PrinterStatus())
    val status: StateFlow<PrinterStatus> = _status.asStateFlow()

    private val _config = MutableStateFlow(PrinterConfig())
    val config: StateFlow<PrinterConfig> = _config.asStateFlow()

    private val _isServiceRunning = MutableStateFlow(false)
    val isServiceRunning: StateFlow<Boolean> = _isServiceRunning.asStateFlow()

    // Holds the latest rendered Glyph Matrix frame for live preview in Compose UI
    private val _currentGlyphFrame = MutableStateFlow(IntArray(25 * 25) { 0 })
    val currentGlyphFrame: StateFlow<IntArray> = _currentGlyphFrame.asStateFlow()

    private val _currentMatrixSize = MutableStateFlow(25)
    val currentMatrixSize: StateFlow<Int> = _currentMatrixSize.asStateFlow()

    fun updateConfig(newConfig: PrinterConfig) {
        _config.value = newConfig
    }

    fun updateStatus(newStatus: PrinterStatus) {
        _status.value = newStatus
    }

    fun updateServiceRunning(running: Boolean) {
        _isServiceRunning.value = running
    }

    fun updateGlyphFrame(frame: IntArray, size: Int) {
        _currentMatrixSize.value = size
        _currentGlyphFrame.value = frame
    }
}
